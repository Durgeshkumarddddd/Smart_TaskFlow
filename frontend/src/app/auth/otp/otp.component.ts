import { Component, OnDestroy, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router, RouterModule } from '@angular/router';
import { AuthService } from '../../services/auth.service';
import { ToastService } from '../../services/toast.service';
import { auth } from '../../config/firebase.config';
import { sendEmailVerification } from 'firebase/auth';
import { Subscription } from 'rxjs';

@Component({
  selector: 'app-otp',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterModule],
  templateUrl: './otp.component.html',
  styleUrl: './otp.component.css'
})
export class OtpComponent implements OnInit, OnDestroy {
  isVerifying = false;
  isResending = false;
  resendTimer = 60;
  resendInterval: any = null;
  errorMessage = '';
  email = '';
  isSuccess = false;
  private pollingSub?: Subscription;

  constructor(private router: Router, private authService: AuthService, private toastService: ToastService) {
    const nav = this.router.getCurrentNavigation();
    const stateEmail = nav?.extras?.state?.['email'];
    if (stateEmail) {
      this.email = stateEmail;
      sessionStorage.setItem('otp_email', stateEmail);
    } else {
      this.email = sessionStorage.getItem('otp_email') || '';
    }
    if (!this.email) {
      this.router.navigate(['/login']);
    }
  }

  ngOnInit(): void {
    this.startResendTimer();
    this.startPolling();
  }

  private startPolling(): void {
    // Reactive verification detection using Firebase listener
    import('firebase/auth').then(({ onIdTokenChanged }) => {
      onIdTokenChanged(auth, async (user) => {
        if (user) {
          await user.reload();
          if (user.emailVerified && !this.isSuccess) {
            this.onVerified();
          }
        }
      });
    });

    // Fallback polling for browsers that don't trigger idTokenChanged on reload
    this.pollingSub = this.authService.pollForVerification().subscribe({
      next: (verified) => {
        if (verified && !this.isSuccess) {
          this.onVerified();
        }
      }
    });
  }

  checkManual(): void {
    this.isVerifying = true;
    const user = auth.currentUser;
    if (user) {
      user.reload().then(() => {
        if (user.emailVerified) {
          this.onVerified();
        } else {
          this.isVerifying = false;
          this.toastService.info('Email not yet verified. Please click the link in your email.');
        }
      }).catch(() => {
        this.isVerifying = false;
        this.errorMessage = 'Failed to check status. Is your internet connected?';
      });
    } else {
      this.isVerifying = false;
      this.errorMessage = 'Session lost. Please login again.';
    }
  }

  private onVerified(): void {
    this.isSuccess = true;
    this.isVerifying = true; 
    this.pollingSub?.unsubscribe();
    this.errorMessage = '';
    
    // Attempt to sync with backend (Save to DB) without starting session
    this.authService.syncVerifiedUser().subscribe({
      next: () => {
        this.toastService.success('Email verified successfully! Please log in now.');
        // Instant redirect back to login for manual sign-in
        setTimeout(() => {
          this.router.navigate(['/login']);
        }, 100);
      },
      error: (err) => {
        this.isVerifying = false;
        // Even if sync logic on backend has issues, user verified OK on Firebase.
        // Redirect to login for them to try manual entry.
        this.toastService.info('Verification complete. Redirecting to login.');
        setTimeout(() => {
          this.router.navigate(['/login']);
        }, 100);
      }
    });
  }

  resendLink(): void {
    if (this.resendTimer > 0) return;
    this.isResending = true;
    this.errorMessage = '';

    const user = auth.currentUser;
    if (user) {
      sendEmailVerification(user).then(() => {
        this.isResending = false;
        this.startResendTimer();
      }).catch(() => {
        this.isResending = false;
        this.errorMessage = 'Failed to resend link. Please try again later.';
      });
    } else {
      this.isResending = false;
      this.errorMessage = 'Session lost. Please go back and login again.';
    }
  }

  private startResendTimer(): void {
    this.resendTimer = 60;
    if (this.resendInterval) clearInterval(this.resendInterval);
    this.resendInterval = setInterval(() => {
      if (--this.resendTimer <= 0) clearInterval(this.resendInterval);
    }, 1000);
  }

  ngOnDestroy(): void {
    if (this.resendInterval) clearInterval(this.resendInterval);
    if (this.pollingSub) this.pollingSub.unsubscribe();
  }
}
