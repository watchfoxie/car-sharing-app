/**
 * Owner Dashboard Component
 * 
 * Displays owner's cars and rental requests for approval.
 * Features:
 * - List of owned cars with management actions
 * - Pending rental approvals
 * - Active and completed rentals
 * - Approve/reject rental requests
 * 
 * @module OwnerDashboardComponent
 */

import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatTabsModule } from '@angular/material/tabs';
import { MatChipsModule } from '@angular/material/chips';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatDividerModule } from '@angular/material/divider';

import { CarService } from '../../../core/services/car.service';
import { RentalService } from '../../../core/services/rental.service';
import { CarListItem } from '../../../core/models/car.model';
import { RentalListItem, RentalStatus } from '../../../core/models/rental.model';

@Component({
  selector: 'app-owner-dashboard',
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
  templateUrl: './owner-dashboard.component.html',
  styleUrl: './owner-dashboard.component.scss'
})
export class OwnerDashboardComponent implements OnInit {
  private readonly carService = inject(CarService);
  private readonly rentalService = inject(RentalService);
  private readonly snackBar = inject(MatSnackBar);

  ownedCars = signal<CarListItem[]>([]);
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

    // Load owned cars
    this.carService.getOwnedCars({ page: 0, size: 50 }).subscribe({
      next: (response) => {
        this.ownedCars.set(response.content);
      },
      error: (error) => console.error('Error loading cars:', error)
    });

    // Load pending rentals
    this.rentalService.getOwnerRentals(0, 50, RentalStatus.PENDING).subscribe({
      next: (response) => {
        this.pendingRentals.set(response.content);
      },
      error: (error) => console.error('Error loading pending rentals:', error)
    });

    // Load active rentals
    this.rentalService.getOwnerRentals(0, 50, RentalStatus.ACTIVE).subscribe({
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
    this.rentalService.getOwnerRentals(0, 20, RentalStatus.COMPLETED).subscribe({
      next: (response) => {
        this.completedRentals.set(response.content);
      },
      error: (error) => console.error('Error loading completed rentals:', error)
    });
  }

  /**
   * Approve rental request
   */
  approveRental(rentalId: string): void {
    this.rentalService.approveRental(rentalId).subscribe({
      next: () => {
        this.snackBar.open('Rental approved successfully', 'Close', { duration: 3000 });
        this.loadDashboardData();
      },
      error: (error) => {
        console.error('Error approving rental:', error);
        this.snackBar.open('Failed to approve rental', 'Close', { duration: 3000 });
      }
    });
  }

  /**
   * Reject rental request
   */
  rejectRental(rentalId: string): void {
    this.rentalService.cancelRental(rentalId, 'Rejected by owner').subscribe({
      next: () => {
        this.snackBar.open('Rental rejected', 'Close', { duration: 3000 });
        this.loadDashboardData();
      },
      error: (error) => {
        console.error('Error rejecting rental:', error);
        this.snackBar.open('Failed to reject rental', 'Close', { duration: 3000 });
      }
    });
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
   * TrackBy functions
   */
  trackByCarId(index: number, car: CarListItem): string {
    return car.id;
  }

  trackByRentalId(index: number, rental: RentalListItem): string {
    return rental.id;
  }
}
