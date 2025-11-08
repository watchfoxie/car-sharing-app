import { Component, OnInit, inject, signal, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router } from '@angular/router';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatChipsModule } from '@angular/material/chips';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatDividerModule } from '@angular/material/divider';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { DateTime } from 'luxon';

import { BookingService, Booking } from '../../services/booking.service';
import { CarService, Car } from '../../../cars/services/car.service';

/**
 * Booking Details Component
 * 
 * Displays comprehensive information about a specific booking.
 * Features:
 * - Booking details with status
 * - Car information
 * - Date range display (UTC → local timezone conversion)
 * - Cost breakdown
 * - Cancel booking action
 * - Success notification after creation
 */
@Component({
  selector: 'app-booking-details',
  standalone: true,
  imports: [
    CommonModule,
    MatCardModule,
    MatButtonModule,
    MatChipsModule,
    MatIconModule,
    MatProgressSpinnerModule,
    MatDividerModule,
    MatSnackBarModule
  ],
  templateUrl: './booking-details.component.html',
  styleUrl: './booking-details.component.css'
})
export class BookingDetailsComponent implements OnInit {
  private readonly bookingService = inject(BookingService);
  private readonly carService = inject(CarService);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly snackBar = inject(MatSnackBar);

  // Reactive state
  booking = signal<Booking | null>(null);
  car = signal<Car | null>(null);
  loading = signal<boolean>(false);
  cancelling = signal<boolean>(false);
  error = signal<string | null>(null);

  // Computed properties
  startDateLocal = computed(() => {
    const booking = this.booking();
    return booking ? DateTime.fromISO(booking.startDate).toLocaleString(DateTime.DATE_FULL) : '';
  });

  endDateLocal = computed(() => {
    const booking = this.booking();
    return booking ? DateTime.fromISO(booking.endDate).toLocaleString(DateTime.DATE_FULL) : '';
  });

  durationDays = computed(() => {
    const booking = this.booking();
    if (!booking) return 0;
    const start = DateTime.fromISO(booking.startDate);
    const end = DateTime.fromISO(booking.endDate);
    return Math.ceil(end.diff(start, 'days').days);
  });

  canCancel = computed(() => {
    const booking = this.booking();
    return booking?.status === 'PENDING' || booking?.status === 'CONFIRMED';
  });

  ngOnInit(): void {
    const bookingId = this.route.snapshot.paramMap.get('id');
    if (bookingId) {
      this.loadBookingDetails(bookingId);
    } else {
      this.error.set('Invalid booking ID');
    }

    // Show success message if redirected from booking creation
    const success = this.route.snapshot.queryParamMap.get('success');
    if (success === 'true') {
      this.snackBar.open('Booking created successfully!', 'Close', {
        duration: 5000,
        horizontalPosition: 'center',
        verticalPosition: 'top',
        panelClass: ['success-snackbar']
      });
    }
  }

  /**
   * Loads booking details and associated car.
   */
  private loadBookingDetails(bookingId: string): void {
    this.loading.set(true);
    this.error.set(null);

    this.bookingService.getBookingById(bookingId).subscribe({
      next: (booking) => {
        this.booking.set(booking);
        this.loadCarDetails(booking.carId);
      },
      error: (err) => {
        console.error('Failed to load booking', err);
        this.error.set('Failed to load booking details.');
        this.loading.set(false);
      }
    });
  }

  /**
   * Loads car details for the booking.
   */
  private loadCarDetails(carId: string): void {
    this.carService.getCarById(carId).subscribe({
      next: (car) => {
        this.car.set(car);
        this.loading.set(false);
      },
      error: (err) => {
        console.error('Failed to load car', err);
        // Don't set error - booking details are still valid
        this.loading.set(false);
      }
    });
  }

  /**
   * Cancels the booking.
   */
  cancelBooking(): void {
    const booking = this.booking();
    if (!booking || !this.canCancel()) return;

    if (!confirm('Are you sure you want to cancel this booking? This action cannot be undone.')) {
      return;
    }

    this.cancelling.set(true);

    this.bookingService.cancelBooking(booking.id!).subscribe({
      next: () => {
        this.snackBar.open('Booking cancelled successfully', 'Close', {
          duration: 3000,
          panelClass: ['success-snackbar']
        });
        this.router.navigate(['/dashboard/renter']);
      },
      error: (err) => {
        console.error('Failed to cancel booking', err);
        this.snackBar.open('Failed to cancel booking. Please try again.', 'Close', {
          duration: 5000,
          panelClass: ['error-snackbar']
        });
        this.cancelling.set(false);
      }
    });
  }

  /**
   * Navigates back to renter dashboard.
   */
  goBack(): void {
    this.router.navigate(['/dashboard/renter']);
  }

  /**
   * Gets status badge color.
   */
  getStatusColor(status: string): string {
    switch (status) {
      case 'PENDING':
        return 'accent';
      case 'CONFIRMED':
        return 'primary';
      case 'ACTIVE':
        return 'primary';
      case 'COMPLETED':
        return '';
      case 'CANCELLED':
        return 'warn';
      default:
        return '';
    }
  }
}
