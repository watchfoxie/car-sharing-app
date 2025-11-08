import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterModule } from '@angular/router';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatTableModule } from '@angular/material/table';
import { MatChipsModule } from '@angular/material/chips';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatTabsModule } from '@angular/material/tabs';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';

import { CarService, Car } from '../../../cars/services/car.service';
import { AuthService } from '../../../../core/services/auth.service';

/**
 * Owner Dashboard Component
 * 
 * Features:
 * - View all owned cars
 * - Car management CRUD (Create, Read, Update, Delete)
 * - Filter by status (Available, Rented, Maintenance)
 * - Quick actions for car management
 * - Statistics overview
 */
@Component({
  selector: 'app-owner-dashboard',
  standalone: true,
  imports: [
    CommonModule,
    RouterModule,
    MatCardModule,
    MatButtonModule,
    MatTableModule,
    MatChipsModule,
    MatIconModule,
    MatProgressSpinnerModule,
    MatTabsModule,
    MatTooltipModule,
    MatSnackBarModule
  ],
  templateUrl: './owner-dashboard.component.html',
  styleUrl: './owner-dashboard.component.css'
})
export class OwnerDashboardComponent implements OnInit {
  private readonly carService = inject(CarService);
  private readonly authService = inject(AuthService);
  private readonly router = inject(Router);
  private readonly snackBar = inject(MatSnackBar);

  // Reactive state
  cars = signal<Car[]>([]);
  loading = signal<boolean>(false);
  error = signal<string | null>(null);

  // User info
  userProfile = this.authService.userProfile;

  // Table columns
  displayedColumns: string[] = ['image', 'car', 'category', 'dailyRate', 'status', 'actions'];

  ngOnInit(): void {
    this.loadMyCars();
  }

  /**
   * Loads all cars owned by the authenticated user.
   * 
   * Note: This is a simplified implementation. In a real scenario,
   * the backend would have an endpoint like GET /v1/cars/my-cars
   * that filters by ownerId. For now, we're loading all cars and
   * filtering client-side.
   */
  private loadMyCars(): void {
    this.loading.set(true);
    this.error.set(null);

    const userId = this.userProfile()?.sub;
    if (!userId) {
      this.error.set('User not authenticated');
      this.loading.set(false);
      return;
    }

    // Load all cars and filter by ownerId
    // In production, this should be done server-side
    this.carService.getCars(0, 100).subscribe({
      next: (response) => {
        const myCars = response.content.filter(car => car.ownerId === userId);
        this.cars.set(myCars);
        this.loading.set(false);
      },
      error: (err) => {
        console.error('Failed to load cars', err);
        this.error.set('Failed to load your cars. Please try again later.');
        this.loading.set(false);
      }
    });
  }

  /**
   * Filters cars by status.
   */
  getFilteredCars(status: 'AVAILABLE' | 'RENTED' | 'MAINTENANCE' | 'ALL'): Car[] {
    if (status === 'ALL') {
      return this.cars();
    }
    return this.cars().filter(car => car.status === status);
  }

  /**
   * Navigates to car creation form.
   */
  createNewCar(): void {
    // TODO: Implement car creation form
    this.snackBar.open('Car creation form coming soon!', 'Close', {
      duration: 3000
    });
  }

  /**
   * Navigates to car edit form.
   */
  editCar(carId: string): void {
    // TODO: Implement car edit form
    this.snackBar.open('Car edit form coming soon!', 'Close', {
      duration: 3000
    });
  }

  /**
   * Deletes a car after confirmation.
   */
  deleteCar(car: Car): void {
    if (!confirm(`Are you sure you want to delete ${car.brand} ${car.model}? This action cannot be undone.`)) {
      return;
    }

    this.carService.deleteCar(car.id).subscribe({
      next: () => {
        this.snackBar.open('Car deleted successfully', 'Close', {
          duration: 3000,
          panelClass: ['success-snackbar']
        });
        this.loadMyCars(); // Reload list
      },
      error: (err) => {
        console.error('Failed to delete car', err);
        this.snackBar.open('Failed to delete car. Please try again.', 'Close', {
          duration: 5000,
          panelClass: ['error-snackbar']
        });
      }
    });
  }

  /**
   * Navigates to car details.
   */
  viewCarDetails(carId: string): void {
    this.router.navigate(['/cars', carId]);
  }

  /**
   * Gets status badge color.
   */
  getStatusColor(status: string): string {
    switch (status) {
      case 'AVAILABLE':
        return 'primary';
      case 'RENTED':
        return 'accent';
      case 'MAINTENANCE':
        return 'warn';
      default:
        return '';
    }
  }

  /**
   * Gets count of cars by status.
   */
  getCountByStatus(status: 'AVAILABLE' | 'RENTED' | 'MAINTENANCE'): number {
    return this.cars().filter(car => car.status === status).length;
  }
}
