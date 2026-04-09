import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';

@Component({
  selector: 'app-not-found',
  standalone: true,
  imports: [CommonModule, RouterModule],
  template: `
    <div class="min-vh-100 d-flex align-items-center justify-content-center bg-gradient-to-br from-[#f5f7fa] to-[#c3cfe2]">
      <div class="text-center px-4">
        <div class="mb-4">
          <i class="bi bi-exclamation-triangle display-1 text-warning"></i>
        </div>
        <h1 class="display-1 fw-bold text-primary">404</h1>
        <h3 class="fw-bold text-dark mb-3">Page Not Found</h3>
        <p class="text-muted mb-4 fs-5">
          Oops! The page you're looking for doesn't exist or has been moved.
        </p>
        <div class="d-flex gap-3 justify-content-center flex-wrap">
          <a routerLink="/dashboard" class="btn btn-primary btn-lg px-4 transition-all duration-200 hover:-translate-y-0.5 hover:shadow-lg">
            <i class="bi bi-house me-2"></i> Go to Dashboard
          </a>
          <a routerLink="/login" class="btn btn-outline-secondary btn-lg px-4 transition-all duration-200 hover:-translate-y-0.5">
            <i class="bi bi-box-arrow-in-right me-2"></i> Login
          </a>
        </div>
      </div>
    </div>
  `,
  styles: []
})
export class NotFoundComponent {}
