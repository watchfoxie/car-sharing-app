import { Routes } from '@angular/router';

/**
 * Cars feature routes.
 * 
 * Public routes - no authentication required:
 * - /cars - list all available cars
 * - /cars/:id - car details
 */
export const carsRoutes: Routes = [
  {
    path: '',
    loadComponent: () => import('./pages/car-list/car-list.component').then(m => m.CarListComponent)
  },
  {
    path: ':id',
    loadComponent: () => import('./pages/car-details/car-details.component').then(m => m.CarDetailsComponent)
  }
];
