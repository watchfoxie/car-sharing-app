/**
 * Renter Dashboard Component
 * 
 * Displays renter's bookings and allows management of rentals.
 * Features:
 * - Active bookings with pickup/return actions
 * - Pending approval status
 * - Completed rentals with feedback option
 * - Rental history
 * 
 * @module RenterDashboardComponent
 */

import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterLink } from '@angular/router';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatTabsModule } from '@angular/material/tabs';
import { MatChipsModule } from '@angular/material/chips';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatDividerModule } from '@angular/material/divider';

import { RentalService } from '../../../core/services/rental.service';
import { RentalListItem, RentalStatus } from '../../../core/models/rental.model';

@Component({
  selector: 'app-renter-dashboard',
  standalone: true,
  imports: [
    CommonModule,
    RouterLink,
    MatCardModule,
    MatButtonModule,
    MatIconModule,
    MatTabsModule,
    MatChipsModule,
    MatProgressSpinnerModule,
    MatSnackBarModule,
    MatDividerModule
  ],
  templateUrl: './renter-dashboard.component.html',
  styleUrl: './renter-dashboard.component.scss'
})
export class RenterDashboardComponent implements OnInit {
  private readonly rentalService = inject(RentalService);
  private readonly router = inject(Router);
  private readonly snackBar = inject(MatSnackBar);

  pendingRentals = signal<RentalListItem[]>([]);
  activeRentals = signal<RentalListItem[]>([]);
  completedRentals = signal<RentalListItem[]>([]);
  isLoading = signal<boolean>(true);

  readonly RentalStatus = RentalStatus;

  ngOnInit(): void {
    this.loadDashboardData();
  }

  /**
   * Load all dashboard data
   */
  loadDashboardData(): void {
    this.isLoading.set(true);

    // Load pending rentals
    this.rentalService.getMyRentals(0, 50, RentalStatus.PENDING).subscribe({
      next: (response) => {
        this.pendingRentals.set(response.content);
      },
      error: (error) => console.error('Error loading pending rentals:', error)
    });

    // Load active rentals
    this.rentalService.getMyRentals(0, 50, RentalStatus.ACTIVE).subscribe({
      next: (response) => {
        this.activeRentals.set(response.content);
        this.isLoading.set(false);
      },
      error: (error) => {
        console.error('Error loading active rentals:', error);
        this.isLoading.set(false);
      }
    });

    // Load completed rentals
    this.rentalService.getMyRentals(0, 20, RentalStatus.COMPLETED).subscribe({
      next: (response) => {
        this.completedRentals.set(response.content);
      },
      error: (error) => console.error('Error loading completed rentals:', error)
    });
  }

  /**
   * Cancel rental
   */
  cancelRental(rentalId: string): void {
    this.rentalService.cancelRental(rentalId, 'Cancelled by renter').subscribe({
      next: () => {
        this.snackBar.open('Rental cancelled successfully', 'Close', { duration: 3000 });
        this.loadDashboardData();
      },
      error: (error) => {
        console.error('Error cancelling rental:', error);
        this.snackBar.open('Failed to cancel rental', 'Close', { duration: 3000 });
      }
    });
  }

  /**
   * Navigate to car details
   */
  viewCarDetails(carId: string): void {
    this.router.navigate(['/cars', carId]);
  }

  /**
   * Navigate to feedback form
   */
  leaveFeedback(rentalId: string): void {
    this.router.navigate(['/feedback', rentalId]);
  }

  /**
   * Get status badge class
   */
  getStatusClass(status: RentalStatus): string {
    const statusClasses: Record<RentalStatus, string> = {
      [RentalStatus.PENDING]: 'status-pending',
      [RentalStatus.APPROVED]: 'status-approved',
      [RentalStatus.ACTIVE]: 'status-active',
      [RentalStatus.COMPLETED]: 'status-completed',
      [RentalStatus.CANCELLED]: 'status-cancelled'
    };
    return statusClasses[status] || '';
  }

  /**
   * TrackBy function
   */
  trackByRentalId(index: number, rental: RentalListItem): string {
    return rental.id;
  }
}
