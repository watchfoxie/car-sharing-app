import { Routes } from '@angular/router';

/**
 * Authentication feature routes.
 * 
 * Public routes for OAuth2/OIDC flow.
 */
export const authRoutes: Routes = [
  {
    path: 'login',
    loadComponent: () => import('./pages/login/login.component').then(m => m.LoginComponent)
  },
  {
    path: 'callback',
    loadComponent: () => import('./pages/callback/callback.component').then(m => m.CallbackComponent)
  },
  {
    path: 'logout',
    loadComponent: () => import('./pages/logout/logout.component').then(m => m.LogoutComponent)
  }
];
