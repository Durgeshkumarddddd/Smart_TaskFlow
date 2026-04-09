import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { AuthService } from '../../services/auth.service';
import { NotificationService } from '../../services/notification.service';

@Component({
  selector: 'app-reset-password',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  templateUrl: './reset-password.component.html',
  styleUrl: './reset-password.component.css'
})
export class ResetPasswordComponent implements OnInit {
  oobCode: string | null = null;
  newPassword = '';
  confirmPassword = '';
  isLoading = false;
  isSuccess = false;
  errorMessage = '';

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private authService: AuthService,
    private notificationService: NotificationService
  ) {}

  ngOnInit() {
    this.oobCode = this.route.snapshot.queryParamMap.get('oobCode');
    if (!this.oobCode) {
      this.errorMessage = 'Invalid or expired reset link. Please request a new one.';
    }
  }

  onSubmit() {
    if (!this.oobCode) return;
    if (this.newPassword !== this.confirmPassword) {
      this.errorMessage = 'Passwords do not match.';
      return;
    }
    if (this.newPassword.length < 6) {
      this.errorMessage = 'Password must be at least 6 characters.';
      return;
    }

    this.isLoading = true;
    this.errorMessage = '';

    this.authService.confirmPasswordReset(this.oobCode, this.newPassword).subscribe({
      next: () => {
        this.isLoading = false;
        this.isSuccess = true;
        const user = this.authService.getCurrentUser();
        if (user && user.id) {
          sessionStorage.removeItem(`tf_chat_history_${user.id}`);
          localStorage.removeItem(`tf_chat_history_${user.id}`);
        }
        this.authService.logout();
        this.router.navigate(['/login']);
      },
      error: (err) => {
        this.isLoading = false;
        this.errorMessage = err.message || 'Failed to reset password. The link may have expired.';
      }
    });
  }
}
