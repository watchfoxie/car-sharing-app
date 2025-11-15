/**
 * Booking Form Component
 * 
 * Allows users to create a rental booking with:
 * - Date range selection (start/end dates)
 * - Real-time price calculation via PricingService
 * - Insurance option
 * - Form validation
 * - Submission to RentalService
 * 
 * @module BookingFormComponent
 */

import { Component, OnInit, inject, signal, computed, effect } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, Validators, ReactiveFormsModule } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatDatepickerModule } from '@angular/material/datepicker';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatDividerModule } from '@angular/material/divider';
import { provideNativeDateAdapter } from '@angular/material/core';
import { DateTime } from 'luxon';

import { CarService } from '../../../core/services/car.service';
import { PricingService } from '../../../core/services/pricing.service';
import { RentalService } from '../../../core/services/rental.service';
import { Car } from '../../../core/models/car.model';
import { PricingCalculationResponse } from '../../../core/models/pricing.model';

@Component({
  selector: 'app-booking-form',
  standalone: true,
  providers: [provideNativeDateAdapter()],
  imports: [
    CommonModule,
    ReactiveFormsModule,
    MatCardModule,
    MatButtonModule,
    MatFormFieldModule,
    MatInputModule,
    MatDatepickerModule,
    MatCheckboxModule,
    MatIconModule,
    MatProgressSpinnerModule,
    MatSnackBarModule,
    MatDividerModule
  ],
  templateUrl: './booking-form.component.html',
  styleUrl: './booking-form.component.scss'
})
export class BookingFormComponent implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly fb = inject(FormBuilder);
  private readonly carService = inject(CarService);
  private readonly pricingService = inject(PricingService);
  private readonly rentalService = inject(RentalService);
  private readonly snackBar = inject(MatSnackBar);

  car = signal<Car | null>(null);
  isLoadingCar = signal<boolean>(true);
  isCalculatingPrice = signal<boolean>(false);
  isSubmitting = signal<boolean>(false);
  pricingDetails = signal<PricingCalculationResponse | null>(null);

  bookingForm!: FormGroup;
  minDate = new Date();

  // Computed estimate for quick feedback
  estimatedPrice = computed(() => {
    const car = this.car();
    const startDate = this.bookingForm?.get('startDate')?.value;
    const endDate = this.bookingForm?.get('endDate')?.value;
    const includeInsurance = this.bookingForm?.get('includeInsurance')?.value;

    if (car && startDate && endDate && startDate < endDate) {
      return this.pricingService.estimatePrice(
        car.pricePerDay,
        startDate,
        endDate,
        includeInsurance
      );
    }
    return null;
  });

  constructor() {
    // Auto-calculate price when dates or insurance change
    effect(() => {
      const car = this.car();
      const pricing = this.pricingDetails();
      
      if (car && this.bookingForm) {
        const startDate = this.bookingForm.get('startDate')?.value;
        const endDate = this.bookingForm.get('endDate')?.value;
        
        if (startDate && endDate && startDate < endDate) {
          this.calculatePrice();
        }
      }
    }, { allowSignalWrites: true });
  }

  ngOnInit(): void {
    this.initializeForm();
    
    const carId = this.route.snapshot.paramMap.get('carId');
    if (carId) {
      this.loadCar(carId);
    } else {
      this.router.navigate(['/cars']);
    }
  }

  /**
   * Initialize booking form with validation
   */
  initializeForm(): void {
    this.bookingForm = this.fb.group({
      startDate: [null, Validators.required],
      endDate: [null, Validators.required],
      includeInsurance: [false]
    }, {
      validators: this.dateRangeValidator
    });
  }

  /**
   * Date range validator
   */
  dateRangeValidator(group: FormGroup): { [key: string]: boolean } | null {
    const startDate = group.get('startDate')?.value;
    const endDate = group.get('endDate')?.value;

    if (startDate && endDate && startDate >= endDate) {
      return { dateRangeInvalid: true };
    }

    return null;
  }

  /**
   * Load car details
   */
  loadCar(carId: string): void {
    this.carService.getCarById(carId).subscribe({
      next: (car) => {
        this.car.set(car);
        this.isLoadingCar.set(false);
      },
      error: (error) => {
        console.error('Error loading car:', error);
        this.snackBar.open('Error loading car details', 'Close', { duration: 3000 });
        this.router.navigate(['/cars']);
      }
    });
  }

  /**
   * Calculate precise price using PricingService
   */
  calculatePrice(): void {
    const car = this.car();
    const startDate = this.bookingForm.get('startDate')?.value as Date;
    const endDate = this.bookingForm.get('endDate')?.value as Date;
    const includeInsurance = this.bookingForm.get('includeInsurance')?.value as boolean;

    if (!car || !startDate || !endDate || startDate >= endDate) {
      return;
    }

    this.isCalculatingPrice.set(true);

    // Convert local dates to UTC ISO strings
    const startDateUTC = DateTime.fromJSDate(startDate).toUTC().toISO();
    const endDateUTC = DateTime.fromJSDate(endDate).toUTC().toISO();

    if (!startDateUTC || !endDateUTC) {
      console.error('Date conversion failed');
      this.isCalculatingPrice.set(false);
      return;
    }

    this.pricingService.calculatePrice({
      carId: car.id,
      startDate: startDateUTC,
      endDate: endDateUTC,
      includeInsurance
    }).subscribe({
      next: (pricing) => {
        this.pricingDetails.set(pricing);
        this.isCalculatingPrice.set(false);
      },
      error: (error) => {
        console.error('Error calculating price:', error);
        this.pricingDetails.set(null);
        this.isCalculatingPrice.set(false);
      }
    });
  }

  /**
   * Submit booking
   */
  submitBooking(): void {
    if (this.bookingForm.invalid || !this.car()) {
      return;
    }

    this.isSubmitting.set(true);

    const car = this.car()!;
    const startDate = this.bookingForm.get('startDate')?.value as Date;
    const endDate = this.bookingForm.get('endDate')?.value as Date;
    const includeInsurance = this.bookingForm.get('includeInsurance')?.value as boolean;

    // Convert to UTC
    const startDateUTC = DateTime.fromJSDate(startDate).toUTC().toISO();
    const endDateUTC = DateTime.fromJSDate(endDate).toUTC().toISO();

    if (!startDateUTC || !endDateUTC) {
      this.snackBar.open('Invalid date format', 'Close', { duration: 3000 });
      this.isSubmitting.set(false);
      return;
    }

    this.rentalService.createRental({
      carId: car.id,
      startDate: startDateUTC,
      endDate: endDateUTC,
      includeInsurance
    }).subscribe({
      next: (rental) => {
        this.snackBar.open('Booking created successfully!', 'Close', { duration: 3000 });
        this.isSubmitting.set(false);
        this.router.navigate(['/dashboard/renter']);
      },
      error: (error) => {
        console.error('Error creating booking:', error);
        this.snackBar.open(
          error.error?.message || 'Failed to create booking',
          'Close',
          { duration: 5000 }
        );
        this.isSubmitting.set(false);
      }
    });
  }

  /**
   * Get number of days
   */
  getNumberOfDays(): number {
    const startDate = this.bookingForm.get('startDate')?.value as Date;
    const endDate = this.bookingForm.get('endDate')?.value as Date;

    if (startDate && endDate && startDate < endDate) {
      return Math.ceil((endDate.getTime() - startDate.getTime()) / (1000 * 60 * 60 * 24));
    }

    return 0;
  }

  /**
   * Cancel booking
   */
  cancel(): void {
    const car = this.car();
    if (car) {
      this.router.navigate(['/cars', car.id]);
    } else {
      this.router.navigate(['/cars']);
    }
  }
}
