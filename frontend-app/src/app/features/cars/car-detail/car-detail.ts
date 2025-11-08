import { Component, DestroyRef, inject, signal } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { CommonModule } from '@angular/common';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatChipsModule } from '@angular/material/chips';
import { MatDividerModule } from '@angular/material/divider';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { filter, map, switchMap, tap } from 'rxjs/operators';

import { Car, CarService } from '../../../core/services/car';
import { FeedbackService, CarFeedbackSummary } from '../../../core/services/feedback';
import { Auth } from '../../../core/services/auth';
import { FeedbackForm } from '../../feedback/feedback-form/feedback-form';
import { FeedbackList } from '../../feedback/feedback-list/feedback-list';

@Component({
  selector: 'app-car-detail',
  imports: [
    CommonModule,
    RouterLink,
    MatCardModule,
    MatButtonModule,
    MatIconModule,
    MatChipsModule,
    MatDividerModule,
    MatProgressSpinnerModule,
    FeedbackForm,
    FeedbackList
  ],
  templateUrl: './car-detail.html',
  styleUrl: './car-detail.css',
})
export class CarDetail {
  private readonly route = inject(ActivatedRoute);
  private readonly carService = inject(CarService);
  private readonly feedbackService = inject(FeedbackService);
  private readonly destroyRef = inject(DestroyRef);
  protected readonly auth = inject(Auth);

  protected readonly car = signal<Car | null>(null);
  protected readonly feedbackSummary = signal<CarFeedbackSummary | null>(null);
  protected readonly loading = signal(true);
  protected readonly errorMessage = signal<string | null>(null);

  constructor() {
    this.route.paramMap
      .pipe(
        map((params) => Number(params.get('id'))),
        filter((id): id is number => Number.isInteger(id) && id > 0),
        tap(() => {
          this.loading.set(true);
          this.errorMessage.set(null);
        }),
        switchMap((id) => this.carService.getCarById(id)),
        takeUntilDestroyed(this.destroyRef)
      )
      .subscribe({
        next: (car) => {
          this.car.set(car);
          this.loading.set(false);
          this.loadSummary(car.id);
        },
        error: (error) => {
          console.error('Failed to fetch car details', error);
          this.errorMessage.set('We could not load this vehicle. It might be unavailable now.');
          this.loading.set(false);
        }
      });
  }

  protected onFeedbackCreated(): void {
    const current = this.car();
    if (current) {
      this.loadSummary(current.id);
    }
  }

  private loadSummary(carId: number): void {
    this.feedbackService
      .getSummaryForCar(carId)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (summary) => this.feedbackSummary.set(summary),
        error: () => this.feedbackSummary.set(null)
      });
  }
}
