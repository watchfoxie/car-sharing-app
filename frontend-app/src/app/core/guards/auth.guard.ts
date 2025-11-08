import { inject } from '@angular/core';
import { Router, CanActivateFn } from '@angular/router';
import { AuthService } from '../services/auth.service';

/**
 * Route guard for authentication.
 * 
 * Prevents unauthenticated users from accessing protected routes.
 * Redirects to login page if not authenticated.
 * 
 * Usage:
 * ```typescript
 * {
 *   path: 'booking/new',
 *   component: CreateBookingComponent,
 *   canActivate: [authGuard]
 * }
 * ```
 */
export const authGuard: CanActivateFn = (route, state) => {
  const authService = inject(AuthService);
  const router = inject(Router);
  
  if (authService.isAuthenticated()) {
    return true;
  }
  
  // Store attempted URL for redirect after login
  const returnUrl = state.url;
  router.navigate(['/auth/login'], { queryParams: { returnUrl } });
  return false;
};
