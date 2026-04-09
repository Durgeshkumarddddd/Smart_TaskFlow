import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';
import { TaskComment } from '../models/task.model';

const API = `${environment.apiUrl}`;

@Injectable({
  providedIn: 'root'
})
export class CommentService {

  constructor(private http: HttpClient) {}

  getComments(taskId: number): Observable<TaskComment[]> {
    return this.http.get<TaskComment[]>(`${API}/tasks/${taskId}/comments`);
  }

  addComment(taskId: number, body: string): Observable<TaskComment> {
    return this.http.post<TaskComment>(`${API}/tasks/${taskId}/comments`, { body });
  }

  deleteComment(commentId: number): Observable<void> {
    return this.http.delete<void>(`${API}/comments/${commentId}`);
  }
}
