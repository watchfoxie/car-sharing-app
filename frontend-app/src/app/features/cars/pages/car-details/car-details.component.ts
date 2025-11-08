import { Component, OnInit, inject, signal, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router } from '@angular/router';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatChipsModule } from '@angular/material/chips';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatDividerModule } from '@angular/material/divider';

import { CarService, Car } from '../../services/car.service';
import { AuthService } from '../../../../core/services/auth.service';

/**
 * Car Details Component
 * 
 * Displays comprehensive information about a specific car.
 * Features:
 * - Full car specifications and details
 * - High-resolution image display
 * - Owner information (if authenticated)
 * - Book button for renters
 * - Loading and error states
 */
@Component({
  selector: 'app-car-details',
  standalone: true,
  imports: [
    CommonModule,
    MatCardModule,
    MatButtonModule,
    MatChipsModule,
    MatIconModule,
    MatProgressSpinnerModule,
    MatDividerModule
  ],
  templateUrl: './car-details.component.html',
  styleUrl: './car-details.component.css'
})
export class CarDetailsComponent implements OnInit {
  private readonly carService = inject(CarService);
  private readonly authService = inject(AuthService);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);

  // Reactive state
  car = signal<Car | null>(null);
  loading = signal<boolean>(false);
  error = signal<string | null>(null);

  // Computed properties
  isAuthenticated = this.authService.isAuthenticated;
  isRenter = this.authService.isRenter;
  canBook = computed(() => {
    const car = this.car();
    return car?.status === 'AVAILABLE' && this.isRenter();
  });

  ngOnInit(): void {
    const carId = this.route.snapshot.paramMap.get('id');
    if (carId) {
      this.loadCarDetails(carId);
    } else {
      this.error.set('Invalid car ID');
    }
  }

  /**
   * Loads car details from the API.
   */
  private loadCarDetails(carId: string): void {
    this.loading.set(true);
    this.error.set(null);

    this.carService.getCarById(carId).subscribe({
      next: (car) => {
        this.car.set(car);
        this.loading.set(false);
      },
      error: (err) => {
        console.error('Failed to load car details', err);
        this.error.set('Failed to load car details. The car may not exist.');
        this.loading.set(false);
      }
    });
  }

  /**
   * Navigates back to car listing.
   */
  goBack(): void {
    this.router.navigate(['/cars']);
  }

  /**
   * Navigates to booking page for this car.
   */
  bookCar(): void {
    const car = this.car();
    if (!car) return;

    if (!this.isAuthenticated()) {
      this.authService.login();
      return;
    }

    this.router.navigate(['/booking/new'], { queryParams: { carId: car.id } });
  }

  /**
   * Gets status badge color based on car status.
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
}
