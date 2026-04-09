import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { AuthService } from '../services/auth.service';
import { ToastService } from '../services/toast.service';

export const roleGuard: CanActivateFn = (route, state) => {
  const authService = inject(AuthService);
  const router = inject(Router);
  const toast = inject(ToastService);

  const allowedRoles = route.data?.['roles'] as string[] | undefined;
  if (!allowedRoles || allowedRoles.length === 0) {
    return true;
  }

  const user = authService.getCurrentUser();
  const role = user?.role;

  if (role && allowedRoles.includes(role)) {
    return true;
  }

  toast.error('You do not have permission to view this page.');
  router.navigate(['/dashboard']);
  return false;
};
