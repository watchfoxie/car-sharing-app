import { inject } from '@angular/core';
import { Router, CanActivateFn } from '@angular/router';
import { AuthService } from '../services/auth.service';

/**
 * Route guard for role-based authorization.
 * 
 * Prevents users without required roles from accessing protected routes.
 * Redirects to unauthorized page (403) or home if role missing.
 * 
 * Usage:
 * ```typescript
 * {
 *   path: 'dashboard/owner',
 *   component: OwnerDashboardComponent,
 *   canActivate: [authGuard, roleGuard],
 *   data: { roles: ['OWNER', 'ADMIN'] } // User must have at least one role
 * }
 * ```
 */
export const roleGuard: CanActivateFn = (route, state) => {
  const authService = inject(AuthService);
  const router = inject(Router);
  
  // Check authentication first
  if (!authService.isAuthenticated()) {
    router.navigate(['/auth/login'], { queryParams: { returnUrl: state.url } });
    return false;
  }
  
  // Get required roles from route data
  const requiredRoles = route.data['roles'] as string[] | undefined;
  
  // If no roles specified, allow access (only authentication required)
  if (!requiredRoles || requiredRoles.length === 0) {
    return true;
  }
  
  // Check if user has at least one required role
  const userRoles = authService.roles();
  const hasRole = requiredRoles.some(role => userRoles.includes(role));
  
  if (hasRole) {
    return true;
  }
  
  // User doesn't have required role - redirect to unauthorized page
  console.warn(`Access denied: User does not have required role. Required: [${requiredRoles.join(', ')}], Has: [${userRoles.join(', ')}]`);
  router.navigate(['/cars']); // TODO: Create unauthorized (403) page
  return false;
};
