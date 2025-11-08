import { Routes } from '@angular/router';
import { authGuard } from '../../core/guards/auth.guard';
import { roleGuard } from '../../core/guards/role.guard';

/**
 * Dashboard feature routes.
 * 
 * Protected routes - require authentication:
 * - /dashboard/renter - renter dashboard (RENTER role)
 * - /dashboard/owner - owner dashboard (OWNER role)
 */
export const dashboardRoutes: Routes = [
  {
    path: 'renter',
    loadComponent: () => import('./pages/renter-dashboard/renter-dashboard.component').then(m => m.RenterDashboardComponent),
    canActivate: [authGuard, roleGuard],
    data: { roles: ['RENTER'] }
  },
  {
    path: 'owner',
    loadComponent: () => import('./pages/owner-dashboard/owner-dashboard.component').then(m => m.OwnerDashboardComponent),
    canActivate: [authGuard, roleGuard],
    data: { roles: ['OWNER'] }
  }
];
