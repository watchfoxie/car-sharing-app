/**
 * Role Guard
 * 
 * Route guard for role-based access control.
 * Checks if the authenticated user has required role(s).
 * 
 * @module RoleGuard
 */

import { inject } from '@angular/core';
import { Router, CanActivateFn, ActivatedRouteSnapshot } from '@angular/router';
import { Auth, UserRole } from '../services/auth';

/**
 * Role Guard Factory
 * 
 * Creates a guard that checks if user has ANY of the specified roles.
 * 
 * Usage in routes:
 * ```typescript
 * {
 *   path: 'admin',
 *   component: AdminComponent,
 *   canActivate: [authGuard, roleGuard([UserRole.ADMIN])]
 * }
 * {
 *   path: 'owner-dashboard',
 *   component: OwnerDashboardComponent,
 *   canActivate: [authGuard, roleGuard([UserRole.OWNER, UserRole.ADMIN])]
 * }
 * ```
 * 
 * @param allowedRoles - Array of roles that can access the route
 * @returns CanActivateFn guard function
 */
export function roleGuard(allowedRoles: UserRole[]): CanActivateFn {
  return (route: ActivatedRouteSnapshot) => {
    const authService = inject(Auth);
    const router = inject(Router);
    
    if (!authService.isAuthenticated()) {
      console.warn('RoleGuard: User not authenticated. Redirecting to login.');
      const targetUrl = route.url.map(segment => segment.path).join('/');
      authService.login(targetUrl || '/cars');
      return false;
    }
    
    if (authService.hasAnyRole(allowedRoles)) {
      return true;
    }
    
    // Access denied - redirect to unauthorized page
    console.warn(
      `RoleGuard: Access denied. Required roles: [${allowedRoles.join(', ')}], ` +
      `User roles: [${authService.getUserRoles().join(', ')}]`
    );
    router.navigate(['/unauthorized']);
    
    return false;
  };
}

/**
 * Convenience guard for ADMIN-only routes
 */
export const adminGuard: CanActivateFn = roleGuard([UserRole.ADMIN]);

/**
 * Convenience guard for OWNER routes (OWNER or ADMIN)
 */
export const ownerGuard: CanActivateFn = roleGuard([UserRole.OWNER, UserRole.ADMIN]);

/**
 * Convenience guard for RENTER routes (RENTER or ADMIN)
 */
export const renterGuard: CanActivateFn = roleGuard([UserRole.RENTER, UserRole.ADMIN]);

/**
 * Convenience guard for MANAGER routes (MANAGER or ADMIN)
 */
export const managerGuard: CanActivateFn = roleGuard([UserRole.MANAGER, UserRole.ADMIN]);
