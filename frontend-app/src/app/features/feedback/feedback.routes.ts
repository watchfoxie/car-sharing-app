import { Routes } from '@angular/router';
import { authGuard } from '../../core/guards/auth.guard';
import { roleGuard } from '../../core/guards/role.guard';

/**
 * Feedback feature routes.
 * 
 * Protected routes - require authentication (RENTER role).
 */
export const feedbackRoutes: Routes = [
  {
    path: 'my',
    loadComponent: () => import('./pages/my-feedback/my-feedback.component').then(m => m.MyFeedbackComponent),
    canActivate: [authGuard, roleGuard],
    data: { roles: ['RENTER'] }
  }
];
