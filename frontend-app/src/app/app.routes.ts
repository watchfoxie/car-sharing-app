import { Routes } from '@angular/router';

/**
 * Main application routes with lazy-loading for feature modules.
 * 
 * Strategy:
 * - Public routes: cars listing (no auth required)
 * - Protected routes: booking, dashboard (require authentication)
 * - Auth routes: login, callback (OAuth2/OIDC flow)
 * 
 * Lazy-loading reduces initial bundle size, preloading improves UX.
 */
export const routes: Routes = [
  {
    path: '',
    redirectTo: '/cars',
    pathMatch: 'full'
  },
  {
    path: 'cars',
    loadChildren: () => import('./features/cars/cars.routes').then(m => m.carsRoutes)
  },
  {
    path: 'booking',
    loadChildren: () => import('./features/booking/booking.routes').then(m => m.bookingRoutes)
  },
  {
    path: 'dashboard',
    loadChildren: () => import('./features/dashboard/dashboard.routes').then(m => m.dashboardRoutes)
  },
  {
    path: 'feedback',
    loadChildren: () => import('./features/feedback/feedback.routes').then(m => m.feedbackRoutes)
  },
  {
    path: 'auth',
    loadChildren: () => import('./features/auth/auth.routes').then(m => m.authRoutes)
  },
  {
    path: '**',
    redirectTo: '/cars'
  }
];
