import { Component, DestroyRef, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { FormControl, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatChipsModule } from '@angular/material/chips';
import { MatDividerModule } from '@angular/material/divider';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { finalize } from 'rxjs/operators';
import { DateTime } from 'luxon';

import { FeedbackService, TopCarRating } from '../../../core/services/feedback';
import { Car, CarService } from '../../../core/services/car';
import { RentalResponse, RentalService, RentalStatus } from '../../../core/services/rental';

@Component({
  selector: 'app-owner-dashboard',
  imports: [
    CommonModule,
    RouterLink,
    ReactiveFormsModule,
    MatCardModule,
    MatFormFieldModule,
    MatInputModule,
    MatButtonModule,
    MatIconModule,
    MatChipsModule,
    MatDividerModule,
    MatProgressSpinnerModule
  ],
  templateUrl: './owner-dashboard.html',
  styleUrl: './owner-dashboard.css',
})
export class OwnerDashboard {
  private readonly feedbackService = inject(FeedbackService);
  private readonly carService = inject(CarService);
  private readonly rentalService = inject(RentalService);
  private readonly destroyRef = inject(DestroyRef);

  protected readonly topCars = signal<TopCarRating[]>([]);
  protected readonly topCarsLoading = signal(false);
  protected readonly topCarsError = signal<string | null>(null);

  protected readonly carIdControl = new FormControl<string>('', {
    nonNullable: false,
    validators: [Validators.required, Validators.pattern(/^\d+$/)]
  });

  protected readonly selectedCar = signal<Car | null>(null);
  protected readonly carError = signal<string | null>(null);
  protected readonly carLoading = signal(false);

  protected readonly rentals = signal<RentalResponse[]>([]);
  protected readonly rentalsLoading = signal(false);
  protected readonly rentalsError = signal<string | null>(null);
  protected readonly rentalsPage = signal(0);
  protected readonly rentalsTotalPages = signal(0);
  protected readonly rentalsTotalElements = signal(0);

  private currentCarId: number | null = null;

  constructor() {
    this.loadTopCars();
  }

  protected lookupCar(): void {
    if (this.carIdControl.invalid) {
      this.carIdControl.markAllAsTouched();
      return;
    }

    const raw = this.carIdControl.value?.trim();
    const id = raw ? Number.parseInt(raw, 10) : NaN;
    if (!Number.isFinite(id) || id <= 0) {
      this.carError.set('Provide a valid car ID.');
      return;
    }

    this.loadCarInsights(id);
  }

  protected loadNext(): void {
    if (this.currentCarId == null) {
      return;
    }
    if (this.rentalsPage() + 1 < this.rentalsTotalPages()) {
      this.loadRentals(this.currentCarId, this.rentalsPage() + 1);
    }
  }

  protected loadPrevious(): void {
    if (this.currentCarId == null) {
      return;
    }
    if (this.rentalsPage() > 0) {
      this.loadRentals(this.currentCarId, this.rentalsPage() - 1);
    }
  }

  protected statusLabel(status: RentalStatus): string {
    switch (status) {
      case 'PENDING':
        return 'Pending';
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

  private loadTopCars(): void {
    this.topCarsLoading.set(true);
    this.topCarsError.set(null);

    this.feedbackService
      .getTopCars(5, 3)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (cars) => {
          this.topCars.set(cars);
          this.topCarsLoading.set(false);
        },
        error: (error) => {
          console.error('Failed to fetch top cars', error);
          this.topCarsLoading.set(false);
          this.topCarsError.set('Unable to load leaderboard right now.');
        }
      });
  }

  private loadCarInsights(carId: number): void {
    this.carLoading.set(true);
    this.carError.set(null);
    this.selectedCar.set(null);
    this.rentals.set([]);
  this.rentalsError.set(null);
  this.rentalsPage.set(0);
  this.rentalsTotalPages.set(0);
  this.rentalsTotalElements.set(0);
    this.currentCarId = carId;

    this.carService
      .getCarById(carId)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (car) => {
          this.selectedCar.set(car);
          this.carLoading.set(false);
          this.loadRentals(carId, 0);
        },
        error: (error) => {
          console.error('Failed to load car', error);
          this.carLoading.set(false);
          this.carError.set('Car not found or not accessible.');
        }
      });
  }

  private loadRentals(carId: number, page: number): void {
    this.rentalsLoading.set(true);
    this.rentalsError.set(null);

    this.rentalService
      .getRentalsForCar(carId, page, 5)
      .pipe(
        takeUntilDestroyed(this.destroyRef),
        finalize(() => this.rentalsLoading.set(false))
      )
      .subscribe({
        next: (response) => {
          this.rentals.set(response.content ?? []);
          this.rentalsPage.set(response.number ?? page);
          this.rentalsTotalPages.set(response.totalPages ?? 0);
          this.rentalsTotalElements.set(response.totalElements ?? this.rentals().length);
        },
        error: (error) => {
          console.error('Failed to load rentals for car', error);
          this.rentalsError.set('Unable to fetch rental history.');
        }
      });
  }

}
