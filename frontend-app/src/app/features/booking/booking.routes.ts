import { Routes } from '@angular/router';
import { authGuard } from '../../core/guards/auth.guard';
import { roleGuard } from '../../core/guards/role.guard';

/**
 * Booking feature routes.
 * 
 * Protected routes - require authentication (RENTER role).
 */
export const bookingRoutes: Routes = [
  {
    path: 'new',
    loadComponent: () => import('./pages/create-booking/create-booking.component').then(m => m.CreateBookingComponent),
    canActivate: [authGuard, roleGuard],
    data: { roles: ['RENTER'] }
  },
  {
    path: ':id',
    loadComponent: () => import('./pages/booking-details/booking-details.component').then(m => m.BookingDetailsComponent),
    canActivate: [authGuard, roleGuard],
    data: { roles: ['RENTER'] }
  }
];
