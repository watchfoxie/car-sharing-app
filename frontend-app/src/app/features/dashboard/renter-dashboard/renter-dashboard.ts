import { Component, DestroyRef, computed, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatChipsModule } from '@angular/material/chips';
import { MatDividerModule } from '@angular/material/divider';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { finalize } from 'rxjs/operators';

import { RentalResponse, RentalService, RentalStatus } from '../../../core/services/rental';
import { DateTime } from 'luxon';

@Component({
  selector: 'app-renter-dashboard',
  imports: [
    CommonModule,
    RouterLink,
    MatCardModule,
    MatButtonModule,
    MatIconModule,
    MatChipsModule,
    MatDividerModule,
    MatProgressSpinnerModule
  ],
  templateUrl: './renter-dashboard.html',
  styleUrl: './renter-dashboard.css',
})
export class RenterDashboard {
  private readonly rentalService = inject(RentalService);
  private readonly destroyRef = inject(DestroyRef);

  private readonly pageSize = 5;

  protected readonly rentals = signal<RentalResponse[]>([]);
  protected readonly loading = signal(false);
  protected readonly errorMessage = signal<string | null>(null);
  protected readonly pageIndex = signal(0);
  protected readonly totalPages = signal(0);
  protected readonly totalElements = signal(0);
  protected readonly cancellingId = signal<number | null>(null);

  protected readonly hasNext = computed(() => this.pageIndex() + 1 < this.totalPages());
  protected readonly hasPrev = computed(() => this.pageIndex() > 0);

  constructor() {
    this.loadPage(0);
  }

  protected refresh(): void {
    this.loadPage(this.pageIndex());
  }

  protected loadNext(): void {
    if (this.hasNext()) {
      this.loadPage(this.pageIndex() + 1);
    }
  }

  protected loadPrevious(): void {
    if (this.hasPrev()) {
      this.loadPage(this.pageIndex() - 1);
    }
  }

  protected canCancel(rental: RentalResponse): boolean {
    return rental.status === 'PENDING' || rental.status === 'CONFIRMED';
  }

  protected cancel(rental: RentalResponse): void {
    if (!this.canCancel(rental) || this.cancellingId()) {
      return;
    }

    this.cancellingId.set(rental.id);
    this.errorMessage.set(null);

    this.rentalService
      .cancelRental(rental.id)
      .pipe(finalize(() => this.cancellingId.set(null)))
      .subscribe({
        next: () => this.refresh(),
        error: (error) => {
          console.error('Failed to cancel rental', error);
          this.errorMessage.set('We could not cancel this booking. Please try again later.');
        }
      });
  }

  protected statusClass(status: RentalStatus): string {
    switch (status) {
      case 'PENDING':
        return 'pending';
      case 'CONFIRMED':
        return 'confirmed';
      case 'PICKED_UP':
        return 'picked';
      case 'RETURNED':
        return 'returned';
      case 'RETURN_APPROVED':
        return 'approved';
      case 'CANCELLED':
        return 'cancelled';
      default:
        return 'pending';
    }
  }

  protected statusLabel(status: RentalStatus): string {
    switch (status) {
      case 'PENDING':
        return 'Pending confirmation';
      case 'CONFIRMED':
        return 'Confirmed';
      case 'PICKED_UP':
        return 'Picked up';
      case 'RETURNED':
        return 'Returned';
      case 'RETURN_APPROVED':
        return 'Completed';
      case 'CANCELLED':
        return 'Cancelled';
      default:
        return status;
    }
  }

  protected formatInstant(value: string | undefined | null): string {
    if (!value) {
      return '—';
    }
    return this.rentalService
      .toLocalDateTime(value)
      .toLocaleString(DateTime.DATETIME_MED_WITH_WEEKDAY);
  }

  protected formatCost(rental: RentalResponse): string {
    const value = rental.finalCost ?? rental.estimatedCost ?? 0;
    return `${value.toFixed(2)} MDL`;
  }

  private loadPage(page: number): void {
    this.loading.set(true);
    this.errorMessage.set(null);

    this.rentalService
      .getMyRentals(page, this.pageSize)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (response) => {
          this.rentals.set(response.content ?? []);
          this.pageIndex.set(response.number ?? page);
          this.totalPages.set(response.totalPages ?? 0);
          this.totalElements.set(response.totalElements ?? this.rentals().length);
          this.loading.set(false);
        },
        error: (error) => {
          console.error('Failed to load renter bookings', error);
          this.loading.set(false);
          this.errorMessage.set('We could not load your bookings right now. Please try again later.');
        }
      });
  }

}
