import { Component, DestroyRef, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute } from '@angular/router';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatDatepickerModule } from '@angular/material/datepicker';
import { MatNativeDateModule } from '@angular/material/core';
import { MatDividerModule } from '@angular/material/divider';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { DateTime, Duration } from 'luxon';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { finalize, map, distinctUntilChanged } from 'rxjs/operators';

import { Car, CarService, VehicleCategory } from '../../../core/services/car';
import { PricingService, PriceEstimateResponse, PriceBreakdownEntry } from '../../../core/services/pricing';
import { RentalService, CreateRentalPayload, RentalResponse } from '../../../core/services/rental';

interface TemporalPayload {
  pickupUtc: string;
  returnUtc: string;
}

const VEHICLE_CATEGORIES = new Set<VehicleCategory>(['ECONOM', 'STANDARD', 'PREMIUM']);

function isVehicleCategory(value: string | null | undefined): value is VehicleCategory {
  return value != null && VEHICLE_CATEGORIES.has(value.toUpperCase() as VehicleCategory);
}

function parseNumber(value: string | null): number | null {
  if (!value) {
    return null;
  }
  const numeric = Number.parseInt(value, 10);
  return Number.isFinite(numeric) ? numeric : null;
}

function generateIdempotencyKey(): string {
  if (typeof crypto !== 'undefined' && typeof crypto.randomUUID === 'function') {
    return crypto.randomUUID();
  }
  const random = Math.random().toString(36).slice(2, 10);
  return `${Date.now().toString(36)}-${random}`;
}

@Component({
  selector: 'app-booking-form',
  imports: [
    CommonModule,
    ReactiveFormsModule,
    MatCardModule,
    MatFormFieldModule,
    MatInputModule,
    MatButtonModule,
    MatIconModule,
    MatDatepickerModule,
    MatNativeDateModule,
    MatDividerModule,
    MatProgressSpinnerModule
  ],
  templateUrl: './booking-form.html',
  styleUrl: './booking-form.css',
})
export class BookingForm {
  private readonly route = inject(ActivatedRoute);
  private readonly carService = inject(CarService);
  private readonly pricingService = inject(PricingService);
  private readonly rentalService = inject(RentalService);
  private readonly destroyRef = inject(DestroyRef);

  protected readonly form = new FormGroup({
    carId: new FormControl<number | null>(null, { validators: [Validators.required] }),
    vehicleCategory: new FormControl<VehicleCategory | null>(null, {
      validators: [Validators.required]
    }),
    pickupDate: new FormControl<Date | null>(null, {
      validators: [Validators.required]
    }),
    pickupTime: new FormControl<string | null>('09:00', {
      validators: [Validators.required, Validators.pattern(/^\d{2}:\d{2}$/)]
    }),
    returnDate: new FormControl<Date | null>(null, {
      validators: [Validators.required]
    }),
    returnTime: new FormControl<string | null>('18:00', {
      validators: [Validators.required, Validators.pattern(/^\d{2}:\d{2}$/)]
    }),
    pickupLocation: new FormControl<string | null>(''),
    returnLocation: new FormControl<string | null>('')
  });

  protected readonly car = signal<Car | null>(null);
  protected readonly loadingCar = signal(false);
  protected readonly carError = signal<string | null>(null);
  protected readonly estimate = signal<PriceEstimateResponse | null>(null);
  protected readonly estimateError = signal<string | null>(null);
  protected readonly rentalSuccess = signal<RentalResponse | null>(null);
  protected readonly submittingEstimate = signal(false);
  protected readonly submittingBooking = signal(false);

  constructor() {
    this.listenToQueryParams();

    const initialPickup = DateTime.local().plus({ hours: 2 }).startOf('hour');
    const initialReturn = initialPickup.plus({ days: 1 });

    this.form.patchValue(
      {
        pickupDate: initialPickup.toJSDate(),
        pickupTime: initialPickup.toFormat('HH:mm'),
        returnDate: initialReturn.toJSDate(),
        returnTime: initialReturn.toFormat('HH:mm')
      },
      { emitEvent: false }
    );

    this.form.valueChanges.pipe(takeUntilDestroyed(this.destroyRef)).subscribe(() => {
      this.estimate.set(null);
      this.estimateError.set(null);
      this.rentalSuccess.set(null);
    });
  }

  protected estimatePrice(): void {
    const payload = this.buildTemporalPayload();
    if (!payload) {
      return;
    }

    const category = this.form.controls.vehicleCategory.value;
    if (!category) {
      this.form.controls.vehicleCategory.markAsTouched();
      return;
    }

    this.submittingEstimate.set(true);
    this.estimateError.set(null);
    this.estimate.set(null);
  this.rentalSuccess.set(null);

    this.pricingService
      .estimate({
        vehicleCategory: category,
        pickupDatetime: payload.pickupUtc,
        returnDatetime: payload.returnUtc
      })
      .pipe(finalize(() => this.submittingEstimate.set(false)))
      .subscribe({
        next: (estimate) => this.estimate.set(estimate),
        error: (error) => {
          console.error('Failed to calculate price estimate', error);
          this.estimateError.set('We could not calculate a price estimate right now. Please try again later.');
        }
      });
  }

  protected submitBooking(): void {
    const payload = this.buildTemporalPayload();
    if (!payload) {
      return;
    }

    const carId = this.form.controls.carId.value;
    if (!carId) {
      this.form.controls.carId.markAsTouched();
      return;
    }

    this.submittingBooking.set(true);
    this.estimateError.set(null);

    const request: CreateRentalPayload = {
      carsId: carId,
      pickupDatetime: payload.pickupUtc,
      returnDatetime: payload.returnUtc,
      pickupLocation: this.form.controls.pickupLocation.value?.trim() || undefined,
      returnLocation: this.form.controls.returnLocation.value?.trim() || undefined,
      idempotencyKey: generateIdempotencyKey()
    };

    this.rentalService
      .createRental(request)
      .pipe(finalize(() => this.submittingBooking.set(false)))
      .subscribe({
        next: (response) => {
          this.rentalSuccess.set(response);
          this.estimate.set(null);
          this.form.markAsPristine();
        },
        error: (error) => {
          console.error('Failed to create rental', error);
          this.estimateError.set('We could not create your booking. Please verify the details and try again.');
        }
      });
  }

  protected trackByBreakdown(_index: number, entry: PriceBreakdownEntry): string {
    return `${entry.unit}-${entry.quantity}`;
  }

  protected pickupDisplay(): string {
    const payload = this.buildTemporalPayload(false);
    if (!payload) {
      return '';
    }
    return this.formatLocalDisplay(payload.pickupUtc);
  }

  protected returnDisplay(): string {
    const payload = this.buildTemporalPayload(false);
    if (!payload) {
      return '';
    }
    return this.formatLocalDisplay(payload.returnUtc);
  }

  protected formatDuration(value: string): string {
    const duration = Duration.fromISO(value);
    if (!duration.isValid) {
      return value;
    }

    const segments: string[] = [];
    const normalized = duration.shiftTo('days', 'hours', 'minutes');
    const days = Math.trunc(normalized.days);
    const hours = Math.trunc(normalized.hours);
    const minutes = Math.trunc(normalized.minutes);

    if (days) {
      segments.push(`${days} day${days === 1 ? '' : 's'}`);
    }
    if (hours) {
      segments.push(`${hours} hour${hours === 1 ? '' : 's'}`);
    }
    if (minutes) {
      segments.push(`${minutes} minute${minutes === 1 ? '' : 's'}`);
    }

    return segments.length > 0 ? segments.join(', ') : 'Less than one minute';
  }

  protected formatInstant(isoUtc: string): string {
    return this.formatLocalDisplay(isoUtc);
  }

  private listenToQueryParams(): void {
    this.route.queryParamMap
      .pipe(
        map((params) => ({
          carId: parseNumber(params.get('carId')),
          category: params.get('category')?.toUpperCase() ?? null
        })),
        distinctUntilChanged((prev, next) => prev.carId === next.carId && prev.category === next.category),
        takeUntilDestroyed(this.destroyRef)
      )
      .subscribe(({ carId, category }) => {
        if (carId && carId > 0) {
          this.form.controls.carId.setValue(carId);
          this.loadCar(carId);
        }
        if (isVehicleCategory(category)) {
          this.form.controls.vehicleCategory.setValue(category);
        }
      });
  }

  private loadCar(carId: number): void {
    this.loadingCar.set(true);
    this.carError.set(null);

    this.carService
      .getCarById(carId)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (car) => {
          this.car.set(car);
          this.loadingCar.set(false);
          if (car.category) {
            this.form.controls.vehicleCategory.setValue(car.category);
          }
        },
        error: (error) => {
          console.error('Failed to load car data', error);
          this.loadingCar.set(false);
          this.car.set(null);
          this.form.controls.vehicleCategory.setValue(null);
          this.carError.set('We could not load the selected car. Please pick another vehicle from the catalog.');
        }
      });
  }

  private buildTemporalPayload(strictValidation = true): TemporalPayload | null {
    const pickupDate = this.form.controls.pickupDate.value;
    const pickupTime = this.form.controls.pickupTime.value;
    const returnDate = this.form.controls.returnDate.value;
    const returnTime = this.form.controls.returnTime.value;

    if (!pickupDate || !pickupTime || !returnDate || !returnTime) {
      if (strictValidation) {
        this.form.controls.pickupDate.markAsTouched();
        this.form.controls.pickupTime.markAsTouched();
        this.form.controls.returnDate.markAsTouched();
        this.form.controls.returnTime.markAsTouched();
      }
      return null;
    }

    const pickup = this.combine(pickupDate, pickupTime);
    const dropoff = this.combine(returnDate, returnTime);

    if (!pickup || !dropoff) {
      if (strictValidation) {
        this.estimateError.set('We could not parse the provided pickup or return times.');
      }
      return null;
    }

    if (dropoff.toMillis() <= pickup.toMillis()) {
      if (strictValidation) {
        this.estimateError.set('Return date and time must be after pickup date and time.');
      }
      return null;
    }

    const pickupUtc = this.rentalService.toIsoUtc(pickup);
    const returnUtc = this.rentalService.toIsoUtc(dropoff);

    return { pickupUtc, returnUtc };
  }

  private combine(date: Date, time: string | null): DateTime | null {
    if (!time) {
      return null;
    }

    const [hours, minutes] = time.split(':').map((value) => Number.parseInt(value, 10));
    if (Number.isNaN(hours) || Number.isNaN(minutes)) {
      return null;
    }

    return DateTime.fromJSDate(date).set({ hour: hours, minute: minutes, second: 0, millisecond: 0 });
  }

  private formatLocalDisplay(isoUtc: string): string {
    return this.pricingService
      .toLocalDateTime(isoUtc)
      .toLocaleString(DateTime.DATETIME_MED_WITH_WEEKDAY);
  }
}
