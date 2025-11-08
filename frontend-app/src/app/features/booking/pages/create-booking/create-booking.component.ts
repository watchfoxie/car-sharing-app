import { Component, OnInit, inject, signal, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, Validators, ReactiveFormsModule } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatDatepickerModule } from '@angular/material/datepicker';
import { MatButtonModule } from '@angular/material/button';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatIconModule } from '@angular/material/icon';
import { MatDividerModule } from '@angular/material/divider';
import { provideLuxonDateAdapter } from '@angular/material-luxon-adapter';
import { DateTime } from 'luxon';

import { BookingService, CostEstimation, Booking } from '../../services/booking.service';
import { CarService, Car } from '../../../cars/services/car.service';
import { AuthService } from '../../../../core/services/auth.service';

/**
 * Create Booking Component
 * 
 * Features:
 * - Date range picker with Material Datepicker (Luxon integration)
 * - Real-time cost estimation via pricing-service API
 * - Car availability validation
 * - Booking confirmation with detailed cost breakdown
 * - UTC ↔ local timezone conversions
 * - Form validation (start date < end date, min 1 day)
 */
@Component({
  selector: 'app-create-booking',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    MatCardModule,
    MatFormFieldModule,
    MatInputModule,
    MatDatepickerModule,
    MatButtonModule,
    MatProgressSpinnerModule,
    MatIconModule,
    MatDividerModule
  ],
  providers: [provideLuxonDateAdapter()],
  templateUrl: './create-booking.component.html',
  styleUrl: './create-booking.component.css'
})
export class CreateBookingComponent implements OnInit {
  private readonly bookingService = inject(BookingService);
  private readonly carService = inject(CarService);
  private readonly authService = inject(AuthService);
  private readonly fb = inject(FormBuilder);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);

  // Reactive state
  car = signal<Car | null>(null);
  costEstimation = signal<CostEstimation | null>(null);
  loading = signal<boolean>(false);
  estimating = signal<boolean>(false);
  submitting = signal<boolean>(false);
  error = signal<string | null>(null);

  // Form
  bookingForm!: FormGroup;

  // Computed properties
  userProfile = this.authService.userProfile;
  today = DateTime.now();
  hasEstimation = computed(() => this.costEstimation() !== null);
  canSubmit = computed(() => {
    return this.bookingForm?.valid && this.hasEstimation() && !this.submitting();
  });

  ngOnInit(): void {
    this.initializeForm();
    this.loadCarFromQueryParam();
    this.setupEstimationTrigger();
  }

  /**
   * Initializes the booking form with validation.
   */
  private initializeForm(): void {
    this.bookingForm = this.fb.group({
      startDate: [null, Validators.required],
      endDate: [null, Validators.required]
    }, {
      validators: this.dateRangeValidator
    });
  }

  /**
   * Custom validator to ensure end date is after start date.
   */
  private dateRangeValidator(group: FormGroup): { [key: string]: boolean } | null {
    const startDate = group.get('startDate')?.value as DateTime | null;
    const endDate = group.get('endDate')?.value as DateTime | null;

    if (startDate && endDate) {
      if (endDate <= startDate) {
        return { invalidDateRange: true };
      }
      
      // Minimum 1 day rental
      const diff = endDate.diff(startDate, 'days').days;
      if (diff < 1) {
        return { minDuration: true };
      }
    }

    return null;
  }

  /**
   * Loads car details from query parameter.
   */
  private loadCarFromQueryParam(): void {
    const carId = this.route.snapshot.queryParamMap.get('carId');
    if (!carId) {
      this.error.set('No car selected. Please select a car from the listing.');
      return;
    }

    this.loading.set(true);
    this.carService.getCarById(carId).subscribe({
      next: (car) => {
        if (car.status !== 'AVAILABLE') {
          this.error.set('This car is not available for booking.');
        }
        this.car.set(car);
        this.loading.set(false);
      },
      error: (err) => {
        console.error('Failed to load car', err);
        this.error.set('Failed to load car details.');
        this.loading.set(false);
      }
    });
  }

  /**
   * Sets up automatic cost estimation when dates change.
   */
  private setupEstimationTrigger(): void {
    this.bookingForm.valueChanges.subscribe(() => {
      if (this.bookingForm.valid && this.car()) {
        this.estimateCost();
      } else {
        this.costEstimation.set(null);
      }
    });
  }

  /**
   * Estimates booking cost via pricing service.
   */
  private estimateCost(): void {
    const car = this.car();
    if (!car) return;

    const startDate = this.bookingForm.value.startDate as DateTime;
    const endDate = this.bookingForm.value.endDate as DateTime;

    this.estimating.set(true);

    this.bookingService.estimateCost({
      carId: car.id,
      startDate: startDate.toUTC().toISO()!,
      endDate: endDate.toUTC().toISO()!
    }).subscribe({
      next: (estimation) => {
        this.costEstimation.set(estimation);
        this.estimating.set(false);
      },
      error: (err) => {
        console.error('Failed to estimate cost', err);
        this.costEstimation.set(null);
        this.estimating.set(false);
      }
    });
  }

  /**
   * Submits the booking.
   */
  onSubmit(): void {
    if (!this.canSubmit()) return;

    const car = this.car();
    const profile = this.userProfile();
    if (!car || !profile) return;

    this.submitting.set(true);
    this.error.set(null);

    const startDate = this.bookingForm.value.startDate as DateTime;
    const endDate = this.bookingForm.value.endDate as DateTime;

    const booking: Booking = {
      carId: car.id,
      renterId: profile.sub,
      startDate: startDate.toUTC().toISO()!,
      endDate: endDate.toUTC().toISO()!
    };

    this.bookingService.createBooking(booking).subscribe({
      next: (createdBooking) => {
        this.submitting.set(false);
        this.router.navigate(['/booking', createdBooking.id], {
          queryParams: { success: true }
        });
      },
      error: (err) => {
        console.error('Failed to create booking', err);
        this.error.set('Failed to create booking. The car may no longer be available.');
        this.submitting.set(false);
      }
    });
  }

  /**
   * Cancels booking and returns to car details.
   */
  cancel(): void {
    const car = this.car();
    if (car) {
      this.router.navigate(['/cars', car.id]);
    } else {
      this.router.navigate(['/cars']);
    }
  }

  /**
   * Date filter to disable past dates.
   */
  dateFilter = (date: DateTime | null): boolean => {
    if (!date) return false;
    return date >= this.today.startOf('day');
  };
}
