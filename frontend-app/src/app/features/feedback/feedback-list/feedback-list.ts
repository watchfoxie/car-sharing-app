import { Component, DestroyRef, Input, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatCardModule } from '@angular/material/card';
import { MatIconModule } from '@angular/material/icon';
import { MatPaginatorModule, PageEvent } from '@angular/material/paginator';
import { MatButtonModule } from '@angular/material/button';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { BehaviorSubject, combineLatest, of } from 'rxjs';
import { catchError, filter, switchMap, tap } from 'rxjs/operators';

import { Auth } from '../../../core/services/auth';
import {
  FeedbackResponse,
  FeedbackService
} from '../../../core/services/feedback';
import { Page } from '../../../core/models/api';

@Component({
  selector: 'app-feedback-list',
  imports: [
    CommonModule,
    MatCardModule,
    MatIconModule,
    MatPaginatorModule,
    MatButtonModule,
    MatProgressSpinnerModule
  ],
  templateUrl: './feedback-list.html',
  styleUrl: './feedback-list.css',
})
export class FeedbackList {
  private readonly feedbackService = inject(FeedbackService);
  private readonly auth = inject(Auth);
  private readonly destroyRef = inject(DestroyRef);

  private readonly carId$ = new BehaviorSubject<number | null>(null);
  private readonly pageState$ = new BehaviorSubject<{ page: number; size: number }>({ page: 0, size: 5 });

  protected readonly feedback = signal<FeedbackResponse[]>([]);
  protected readonly total = signal(0);
  protected readonly loading = signal(false);
  protected readonly errorMessage = signal<string | null>(null);
  protected readonly pageSize = signal(5);

  @Input({ required: true })
  set carId(value: number | null) {
    this.carId$.next(value ?? null);
    this.pageState$.next({ page: 0, size: this.pageState$.value.size });
  }

  constructor() {
    combineLatest([this.carId$, this.pageState$])
      .pipe(
        filter(([carId]) => !!carId),
        tap(() => {
          this.loading.set(true);
          this.errorMessage.set(null);
        }),
        switchMap(([carId, pageState]) =>
          this.feedbackService.getFeedbackForCar(carId as number, pageState.page, pageState.size).pipe(
            catchError((error) => {
              console.error('Failed to load feedback', error);
              this.errorMessage.set('Unable to load feedback right now.');
              return of<Page<FeedbackResponse>>({
                content: [],
                totalElements: 0,
                totalPages: 0,
                number: pageState.page,
                size: pageState.size
              });
            })
          )
        ),
        takeUntilDestroyed(this.destroyRef)
      )
      .subscribe((page) => {
        this.feedback.set(page.content);
        this.total.set(page.totalElements);
        this.pageSize.set(page.size);
        this.loading.set(false);
      });
  }

  protected onPageChange(event: PageEvent): void {
    this.pageState$.next({ page: event.pageIndex, size: event.pageSize });
    this.pageSize.set(event.pageSize);
  }

  protected canDelete(entry: FeedbackResponse): boolean {
    return this.auth.isAuthenticated() && this.auth.profile()?.id === entry.reviewerId;
  }

  protected deleteFeedback(id: number): void {
    const carId = this.carId$.value;
    if (!carId) {
      return;
    }

    this.feedbackService
      .deleteFeedback(id)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: () => this.refreshCurrentPage(),
        error: (error) => console.error('Failed to delete feedback', error)
      });
  }

  private refreshCurrentPage(): void {
    const state = this.pageState$.value;
    this.pageState$.next({ page: state.page, size: state.size });
  }
}
