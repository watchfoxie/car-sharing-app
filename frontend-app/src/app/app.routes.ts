/**
 * Application Routes Configuration
 * 
 * Defines all routes for the Car Sharing application with:
 * - Lazy-loaded feature modules
 * - Route guards for authentication and authorization
 * - Redirect rules and fallback routes
 */

import { Routes } from '@angular/router';
import { authGuard } from './core/guards/auth.guard';
import { ownerGuard, renterGuard } from './core/guards/role.guard';

export const routes: Routes = [
  // Home/Landing Page
  {
    path: '',
    redirectTo: '/cars',
    pathMatch: 'full'
  },

  // Car Listing (Public)
  {
    path: 'cars',
    loadComponent: () =>
      import('./features/cars/car-list/car-list.component').then((m) => m.CarListComponent)
  },

  // Car Details (Public)
  {
    path: 'cars/:id',
    loadComponent: () =>
      import('./features/cars/car-detail/car-detail.component').then((m) => m.CarDetailComponent)
  },

  // Booking Form (Authenticated)
  {
    path: 'booking/:carId',
    loadComponent: () =>
      import('./features/booking/booking-form/booking-form.component').then(
        (m) => m.BookingFormComponent
      ),
    canActivate: [authGuard, renterGuard]
  },

  // Owner Dashboard (Owner/Admin only)
  {
    path: 'dashboard/owner',
    loadComponent: () =>
      import('./features/dashboard/owner-dashboard/owner-dashboard.component').then(
        (m) => m.OwnerDashboardComponent
      ),
    canActivate: [authGuard, ownerGuard]
  },

  // Renter Dashboard (Renter/Admin only)
  {
    path: 'dashboard/renter',
    loadComponent: () =>
      import('./features/dashboard/renter-dashboard/renter-dashboard.component').then(
        (m) => m.RenterDashboardComponent
      ),
    canActivate: [authGuard, renterGuard]
  },

  // Fallback 404
  {
    path: '**',
    redirectTo: '/cars'
  }
];
