import { Component, OnInit, Input } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { CommentService } from '../services/comment.service';
import { AuthService } from '../services/auth.service';
import { ToastService } from '../services/toast.service';
import { TaskComment } from '../models/task.model';
import { RelativeTimePipe } from '../pipes/relative-time.pipe';

@Component({
  selector: 'app-comments',
  standalone: true,
  imports: [CommonModule, FormsModule, RelativeTimePipe],
  templateUrl: './comments.component.html'
})
export class CommentsComponent implements OnInit {
  @Input() taskId!: number;

  comments: TaskComment[] = [];
  newComment = '';
  isLoading = false;
  isPosting = false;
  deletingId: number | null = null;
  currentUserId: number | null = null;

  constructor(
    private commentService: CommentService,
    private authService: AuthService,
    private toastService: ToastService
  ) {}

  ngOnInit(): void {
    const user = this.authService.getCurrentUser();
    this.currentUserId = user?.id || null;
    this.loadComments();
  }

  loadComments(): void {
    if (!this.taskId) return;
    this.isLoading = true;
    this.commentService.getComments(this.taskId).subscribe({
      next: (comments) => {
        this.comments = comments;
        this.isLoading = false;
      },
      error: () => {
        this.isLoading = false;
        this.toastService.error('Failed to load comments.');
      }
    });
  }

  postComment(): void {
    if (!this.newComment.trim()) return;
    this.isPosting = true;
    this.commentService.addComment(this.taskId, this.newComment.trim()).subscribe({
      next: (comment) => {
        this.comments.push(comment);
        this.newComment = '';
        this.isPosting = false;
        this.toastService.success('Comment posted.');
      },
      error: () => {
        this.isPosting = false;
        this.toastService.error('Failed to post comment.');
      }
    });
  }

  deleteComment(commentId: number): void {
    this.deletingId = commentId;
    this.commentService.deleteComment(commentId).subscribe({
      next: () => {
        this.comments = this.comments.filter(c => c.id !== commentId);
        this.deletingId = null;
        this.toastService.success('Comment deleted.');
      },
      error: (err) => {
        this.deletingId = null;
        if (err.status === 403) {
          this.toastService.error('You can only delete your own comments.');
        } else {
          this.toastService.error('Failed to delete comment.');
        }
      }
    });
  }

  getInitial(name?: string): string {
    return (name || 'U').charAt(0).toUpperCase();
  }

  getAvatarColor(name?: string): string {
    const colors = ['#3B82F6', '#8B5CF6', '#EF4444', '#F59E0B', '#22C55E', '#06B6D4', '#EC4899', '#6366F1'];
    const str = name || 'U';
    let hash = 0;
    for (let i = 0; i < str.length; i++) {
      hash = str.charCodeAt(i) + ((hash << 5) - hash);
    }
    return colors[Math.abs(hash) % colors.length];
  }
}
