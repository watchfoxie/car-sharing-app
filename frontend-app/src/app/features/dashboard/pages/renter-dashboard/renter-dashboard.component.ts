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
import { DateTime } from 'luxon';

import { BookingService, Booking } from '../../../booking/services/booking.service';
import { CarService, Car } from '../../../cars/services/car.service';
import { AuthService } from '../../../../core/services/auth.service';

interface BookingWithCar extends Booking {
  car?: Car;
}

/**
 * Renter Dashboard Component
 * 
 * Features:
 * - View all rentals (active, upcoming, past)
 * - Booking history with car details
 * - Quick actions (view details, cancel booking)
 * - Tabs for filtering by status
 * - Real-time data loading
 */
@Component({
  selector: 'app-renter-dashboard',
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
    MatTabsModule
  ],
  templateUrl: './renter-dashboard.component.html',
  styleUrl: './renter-dashboard.component.css'
})
export class RenterDashboardComponent implements OnInit {
  private readonly bookingService = inject(BookingService);
  private readonly carService = inject(CarService);
  private readonly authService = inject(AuthService);
  private readonly router = inject(Router);

  // Reactive state
  bookings = signal<BookingWithCar[]>([]);
  loading = signal<boolean>(false);
  error = signal<string | null>(null);

  // User info
  userProfile = this.authService.userProfile;

  // Table columns
  displayedColumns: string[] = ['car', 'dates', 'duration', 'cost', 'status', 'actions'];

  ngOnInit(): void {
    this.loadBookings();
  }

  /**
   * Loads all bookings for the authenticated renter.
   */
  private loadBookings(): void {
    this.loading.set(true);
    this.error.set(null);

    this.bookingService.getMyBookings().subscribe({
      next: (bookings) => {
        // Load car details for each booking
        const bookingsWithCars = bookings.map(booking => this.enrichBookingWithCar(booking));
        Promise.all(bookingsWithCars).then(enriched => {
          this.bookings.set(enriched);
          this.loading.set(false);
        });
      },
      error: (err) => {
        console.error('Failed to load bookings', err);
        this.error.set('Failed to load your bookings. Please try again later.');
        this.loading.set(false);
      }
    });
  }

  /**
   * Enriches booking with car details.
   */
  private async enrichBookingWithCar(booking: Booking): Promise<BookingWithCar> {
    try {
      const car = await this.carService.getCarById(booking.carId).toPromise();
      return { ...booking, car };
    } catch (err) {
      console.error(`Failed to load car ${booking.carId}`, err);
      return booking;
    }
  }

  /**
   * Filters bookings by status category.
   */
  getFilteredBookings(category: 'active' | 'upcoming' | 'past'): BookingWithCar[] {
    const now = DateTime.now();
    return this.bookings().filter(booking => {
      const startDate = DateTime.fromISO(booking.startDate);
      const endDate = DateTime.fromISO(booking.endDate);

      switch (category) {
        case 'active':
          return booking.status === 'ACTIVE' || (now >= startDate && now <= endDate && booking.status === 'CONFIRMED');
        case 'upcoming':
          return booking.status === 'CONFIRMED' && now < startDate;
        case 'past':
          return booking.status === 'COMPLETED' || booking.status === 'CANCELLED' || now > endDate;
        default:
          return true;
      }
    });
  }

  /**
   * Formats date to local string.
   */
  formatDate(isoDate: string): string {
    return DateTime.fromISO(isoDate).toLocaleString(DateTime.DATE_MED);
  }

  /**
   * Calculates booking duration in days.
   */
  calculateDuration(startDate: string, endDate: string): number {
    const start = DateTime.fromISO(startDate);
    const end = DateTime.fromISO(endDate);
    return Math.ceil(end.diff(start, 'days').days);
  }

  /**
   * Navigates to booking details.
   */
  viewBookingDetails(bookingId: string): void {
    this.router.navigate(['/booking', bookingId]);
  }

  /**
   * Navigates to car details.
   */
  viewCarDetails(carId: string): void {
    this.router.navigate(['/cars', carId]);
  }

  /**
   * Navigates to new booking page.
   */
  createNewBooking(): void {
    this.router.navigate(['/cars']);
  }

  /**
   * Gets status badge color.
   */
  getStatusColor(status: string): string {
    switch (status) {
      case 'PENDING':
        return 'accent';
      case 'CONFIRMED':
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
