import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router, RouterModule, ActivatedRoute } from '@angular/router';
import { AuthService } from '../../services/auth.service';
import { ToastService } from '../../services/toast.service';
import { LoginRequest } from '../../models/user.model';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterModule],
  templateUrl: './login.component.html',
  styleUrl: './login.component.css'
})
export class LoginComponent {
  loginData: LoginRequest = {
    email: '',
    password: ''
  };

  errorMessage = '';
  isLoading = false;
  showPassword = false;
  returnUrl = '/dashboard';

  constructor(
    private authService: AuthService,
    private router: Router,
    private route: ActivatedRoute,
    private toastService: ToastService
  ) {
    // Check if there is a return URL from route query params
    this.returnUrl = this.route.snapshot.queryParams['returnUrl'] || '/dashboard';
  }

  onSubmit(): void {
    this.errorMessage = '';

    // Client-side validation
    if (!this.loginData.email || !this.loginData.password) {
      this.errorMessage = 'Please fill in all fields.';
      return;
    }

    if (!this.isValidIdentifier(this.loginData.email)) {
      this.errorMessage = 'Please enter a valid email or username.';
      return;
    }

    this.isLoading = true;

    this.authService.login(this.loginData).subscribe({
      next: () => {
        this.isLoading = false;
        this.toastService.success('Welcome back! Successfully logged in.');
        this.router.navigate([this.returnUrl]);
      },
      error: (err) => {
        this.isLoading = false;
        
        const errorMsg = err.error?.message || '';
        
        if (errorMsg === 'USER_NOT_FOUND') {
          this.errorMessage = 'No account found with this email. Please sign up first.';
          this.toastService.warning('Account not found. Trying to log in? Create an account first!');
          return;
        }

        if (errorMsg.includes('verification') || err.message === 'VERIFY_REQUIRED') {
          this.toastService.warning('Email not verified. Please check your inbox.');
          this.router.navigate(['/verify-email'], { state: { email: this.loginData.email } });
          return;
        }

        if (err.status === 401 || err.status === 400 || err.code === 'auth/invalid-credential') {
          this.errorMessage = 'Invalid email or password. Please try again.';
          this.toastService.error('Invalid email or password.');
        } else if (err.status === 0) {
          this.errorMessage = 'Unable to connect to server. Please check your connection.';
          this.toastService.error('Unable to connect to server.');
        } else {
          this.errorMessage = errorMsg || 'An unexpected error occurred. Please try again.';
          this.toastService.error('Login failed. Please try again.');
        }
      }
    });
  }

  onOtpLogin(): void {
    // OTP login disabled in favor of Firebase Link Verification
    this.toastService.info('Please use standard login. Email verification is mandatory.');
  }

  togglePassword(): void {
    this.showPassword = !this.showPassword;
  }

  private isValidIdentifier(value: string): boolean {
    if (!value || !value.trim()) return false;

    const trimmed = value.trim();

    // Accept username or email
    if (trimmed.includes('@')) {
      const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
      return emailRegex.test(trimmed);
    }

    // Username must be at least 3 chars and contain no spaces
    return trimmed.length >= 3 && !/\s/.test(trimmed);
  }
}
