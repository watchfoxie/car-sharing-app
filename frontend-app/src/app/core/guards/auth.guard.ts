/**
 * Auth Guard
 * 
 * Route guard to protect authenticated routes.
 * Redirects unauthenticated users to the login page.
 * 
 * @module AuthGuard
 */

import { inject } from '@angular/core';
import { CanActivateFn, ActivatedRouteSnapshot, RouterStateSnapshot } from '@angular/router';
import { Auth } from '../services/auth';

/**
 * Auth Guard (Functional)
 * 
 * Usage in routes:
 * ```typescript
 * {
 *   path: 'dashboard',
 *   component: DashboardComponent,
 *   canActivate: [authGuard]
 * }
 * ```
 * 
 * @param route - Activated route snapshot
 * @param state - Router state snapshot
 * @returns true if authenticated, otherwise redirects to login
 */
export const authGuard: CanActivateFn = (
  route: ActivatedRouteSnapshot,
  state: RouterStateSnapshot
) => {
  const authService = inject(Auth);
  
  if (authService.isAuthenticated()) {
    return true;
  }
  
  // Store the attempted URL for redirecting after login
  console.warn(`AuthGuard: Access denied to ${state.url}. Redirecting to login.`);
  authService.login(state.url);
  
  return false;
};
