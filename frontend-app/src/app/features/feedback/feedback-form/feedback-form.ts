import { Component, EventEmitter, Input, Output, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatSelectModule } from '@angular/material/select';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';

import { finalize } from 'rxjs/operators';

import { FeedbackService, CreateFeedbackPayload } from '../../../core/services/feedback';
import { Auth } from '../../../core/services/auth';

@Component({
  selector: 'app-feedback-form',
  imports: [
    CommonModule,
    ReactiveFormsModule,
    MatFormFieldModule,
    MatSelectModule,
    MatInputModule,
    MatButtonModule,
    MatIconModule
  ],
  templateUrl: './feedback-form.html',
  styleUrl: './feedback-form.css',
})
export class FeedbackForm {
  private readonly feedbackService = inject(FeedbackService);
  protected readonly auth = inject(Auth);

  protected readonly form = new FormGroup({
    rating: new FormControl<number | null>(null, {
      nonNullable: false,
      validators: [Validators.required, Validators.min(0), Validators.max(5)]
    }),
    comment: new FormControl<string | null>('', {
      nonNullable: false,
      validators: [Validators.maxLength(5000)]
    })
  });

  protected readonly submitting = signal(false);
  protected readonly errorMessage = signal<string | null>(null);
  protected readonly successMessage = signal<string | null>(null);

  private carIdInternal: number | null = null;

  @Input({ required: true })
  set carId(value: number | null) {
    this.carIdInternal = value ?? null;
  }

  @Output() readonly feedbackCreated = new EventEmitter<void>();

  protected readonly ratings = [5, 4, 3, 2, 1, 0];
  protected readonly starArray = (count: number) => Array.from({ length: Math.max(count, 0) });

  protected submit(): void {
    if (this.form.invalid || this.carIdInternal == null) {
      this.form.markAllAsTouched();
      return;
    }

    const rating = this.form.controls.rating.value;
    if (rating == null) {
      this.form.controls.rating.markAsTouched();
      return;
    }

    const payload: CreateFeedbackPayload = {
      carsId: this.carIdInternal,
      rating,
      comment: this.form.controls.comment.value?.trim() || undefined
    };

    this.submitting.set(true);
    this.errorMessage.set(null);
    this.successMessage.set(null);

    this.feedbackService
      .createFeedback(payload)
      .pipe(finalize(() => this.submitting.set(false)))
      .subscribe({
      next: () => {
        this.successMessage.set('Thank you! Your feedback was recorded.');
          this.form.reset({ rating: null, comment: '' });
        this.feedbackCreated.emit();
      },
      error: (error) => {
        console.error('Failed to submit feedback', error);
        this.errorMessage.set('We could not submit your feedback. Please try again later.');
        }
      });
  }
}
