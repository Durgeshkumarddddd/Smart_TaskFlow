import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { AuthService } from '../../services/auth.service';
import { ToastService } from '../../services/toast.service';

@Component({
  selector: 'app-forgot-password',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  templateUrl: './forgot-password.component.html',
  styleUrls: ['./forgot-password.component.css']
})
export class ForgotPasswordComponent {
  email: string = '';
  isLoading: boolean = false;
  isSent: boolean = false;

  constructor(
    private authService: AuthService,
    private toastService: ToastService,
    private router: Router
  ) {}

  onSubmit() {
    if (!this.email) {
      this.toastService.error('Please enter your email address');
      return;
    }

    this.isLoading = true;
    this.authService.sendPasswordResetEmail(this.email).subscribe({
      next: () => {
        this.isLoading = false;
        this.isSent = true;
        this.toastService.success('Password reset email sent!');
      },
      error: (error) => {
        this.isLoading = false;
        this.toastService.error(error.message || 'Failed to send reset email');
      }
    });
  }
}
