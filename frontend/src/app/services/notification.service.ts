import { Injectable, OnDestroy } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { BehaviorSubject, Observable, Subject, Subscription } from 'rxjs';
import { Client, IMessage } from '@stomp/stompjs';
import { AuthService } from './auth.service';

export interface Notification {
  id: string;
  title: string;
  message: string;
  type: 'info' | 'success' | 'warning' | 'error';
  timestamp: Date;
  isRead: boolean;
}

@Injectable({
  providedIn: 'root'
})
export class NotificationService implements OnDestroy {
  private notificationsSubject = new BehaviorSubject<Notification[]>([]);
  public notifications$ = this.notificationsSubject.asObservable();

  private unreadCountSubject = new BehaviorSubject<number>(0);
  public unreadCount$ = this.unreadCountSubject.asObservable();
  
  public taskUpdate$ = new Subject<any>();

  private stompClient: Client | null = null;
  private authSubscription: Subscription;
  private readonly apiUrl = 'http://localhost:8080/api/activity';
  private teamService: any; // Lazy assignment to avoid circular dep if any

  constructor(private authService: AuthService, private http: HttpClient) {
    this.authSubscription = this.authService.currentUser$.subscribe(user => {
      if (user && user.id) {
        this.fetchInitialNotifications();
        this.connectWebSocket();
      } else {
        this.disconnectWebSocket();
        this.notificationsSubject.next([]);
      }
    });
  }

  ngOnDestroy(): void {
    this.authSubscription.unsubscribe();
    this.disconnectWebSocket();
  }

  private getDismissedKey(): string {
    const user = this.authService.getCurrentUser();
    return `tf-dismissed-notifications-${user?.id ?? 'guest'}`;
  }

  private getDismissedIds(): Set<string> {
    try {
      const stored = localStorage.getItem(this.getDismissedKey());
      return new Set(stored ? JSON.parse(stored) : []);
    } catch {
      return new Set();
    }
  }

  private saveDismissedIds(ids: Set<string>): void {
    localStorage.setItem(this.getDismissedKey(), JSON.stringify([...ids]));
  }

  private fetchInitialNotifications(): void {
    const dismissed = this.getDismissedIds();
    this.http.get<any[]>(`${this.apiUrl}?excludeMe=true`).subscribe({
      next: (activities) => {
        const notes = activities
          .filter(a => !dismissed.has(a.id.toString()))
          .map(a => ({
            id: a.id.toString(),
            title: a.actionCode.replace(/_/g, ' '),
            message: a.message,
            type: 'info' as const,
            timestamp: new Date(a.createdAt),
            isRead: false
          }));
        this.notificationsSubject.next(notes);
        this.updateUnreadCount();
      },
      error: (err) => console.error('Failed to fetch initial notifications', err)
    });
  }

  private connectWebSocket(): void {
    if (this.stompClient && this.stompClient.active) return;

    const token = this.authService.getToken();
    this.stompClient = new Client({
      brokerURL: 'ws://localhost:8080/ws',
      connectHeaders: token ? { Authorization: `Bearer ${token}` } : {},
      debug: (msg) => console.log('STOMP: ' + msg),
      reconnectDelay: 5000,
      onConnect: () => {
        console.log('Connected to WebSocket');
        
        const taskTypes = ['TASK_ASSIGNED', 'TASK_COMPLETED', 'TASK_DELETED', 'TASK_CREATED', 'TASK_UPDATED'];

        const handleWebSocketMessage = (message: IMessage) => {
          const wsMsg = JSON.parse(message.body);
          const payload = wsMsg.payload || {};
          const type = wsMsg.type || '';
          const initiatedBy = wsMsg.initiatedBy;
          const currentUser = this.authService.getCurrentUser();

          // Filter out redundant updates initiated by current user
          if (initiatedBy && currentUser && initiatedBy === currentUser.email) {
            console.log(`Skipping real-time update for ${type} as it was initiated by self.`);
          } else if (taskTypes.includes(type)) {
            console.log(`Real-time signal received: ${type}`);
            this.taskUpdate$.next(wsMsg);
          }

          let title = 'Notification';
          let notifType: 'info' | 'success' | 'warning' | 'error' = 'info';

          if (type === 'TASK_ASSIGNED') {
            title = '📋 Task Assigned';
          } else if (type === 'TASK_COMPLETED') {
            title = '✅ Task Completed';
            notifType = 'success';
          } else if (type === 'TASK_DELETED') {
            title = '🗑️ Task Deleted';
            notifType = 'warning';
          } else if (type === 'TASK_CREATED') {
            title = '🆕 New Task';
            notifType = 'info';
          } else if (type === 'TASK_UPDATED') {
            title = '🔄 Task Updated';
            notifType = 'info';
          }

          this.addNotification({
            title,
            message: payload.message || 'You have a new notification',
            type: notifType
          });
        };

        // 1. Subscribe to individual user notifications
        this.stompClient?.subscribe('/user/queue/notifications', handleWebSocketMessage);

        // 2. Subscribe to team topics if user has teams
        // Note: Using direct inject pattern or simple HTTP to avoid complex circular dependencies if required
        this.http.get<any[]>('http://localhost:8080/api/teams').subscribe({
          next: (teams) => {
            teams.forEach(team => {
              console.log(`Subscribing to team topic: /topic/teams/${team.id}`);
              this.stompClient?.subscribe(`/topic/teams/${team.id}`, handleWebSocketMessage);
            });
          },
          error: (err) => console.error('Failed to subscribe to teams', err)
        });
      },
      onStompError: (frame) => {
        console.error('STOMP error', frame);
      }
    });

    this.stompClient.activate();
  }

  private disconnectWebSocket(): void {
    if (this.stompClient) {
      this.stompClient.deactivate();
      this.stompClient = null;
    }
  }

  addNotification(notification: Omit<Notification, 'id' | 'timestamp' | 'isRead'>): void {
    const newNotification: Notification = {
      ...notification,
      id: Math.random().toString(36).substring(2, 9),
      timestamp: new Date(),
      isRead: false
    };

    const current = this.notificationsSubject.value;
    this.notificationsSubject.next([newNotification, ...current]);
    this.updateUnreadCount();
  }

  markAsRead(id: string): void {
    const current = this.notificationsSubject.value.map(n => 
      n.id === id ? { ...n, isRead: true } : n
    );
    this.notificationsSubject.next(current);
    this.updateUnreadCount();
  }

  markAllAsRead(): void {
    const current = this.notificationsSubject.value.map(n => ({ ...n, isRead: true }));
    this.notificationsSubject.next(current);
    this.updateUnreadCount();
  }

  clearAll(): void {
    const currentIds = this.notificationsSubject.value.map(n => n.id);
    if (currentIds.length > 0) {
      const dismissed = this.getDismissedIds();
      currentIds.forEach(id => dismissed.add(id));
      this.saveDismissedIds(dismissed);
    }
    this.notificationsSubject.next([]);
    this.updateUnreadCount();
  }

  private updateUnreadCount(): void {
    const count = this.notificationsSubject.value.filter(n => !n.isRead).length;
    this.unreadCountSubject.next(count);
  }
}
