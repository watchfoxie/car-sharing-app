import { Component, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatPaginatorModule, PageEvent } from '@angular/material/paginator';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatChipsModule } from '@angular/material/chips';
import { RouterLink } from '@angular/router';
import {
  FeedbackService,
  FeedbackResponse,
} from '../../../../core/services/feedback';
import { DateTime } from 'luxon';

/**
 * MyFeedbackComponent displays the authenticated user's feedback history.
 * 
 * Features:
 * - List user's submitted feedback with pagination
 * - Display car details and rating for each feedback
 * - Navigate to car details from feedback entries
 * - Show loading state and empty state
 * - Date formatting with Luxon (UTC to local conversion)
 * 
 * @requires FeedbackService - for fetching user's feedback
 * @requires AuthService - implicit via route guard (authGuard + roleGuard)
 */
@Component({
  selector: 'app-my-feedback',
  standalone: true,
  imports: [
    CommonModule,
    MatCardModule,
    MatButtonModule,
    MatIconModule,
    MatPaginatorModule,
    MatProgressSpinnerModule,
    MatChipsModule,
    RouterLink
  ],
  templateUrl: './my-feedback.component.html',
  styleUrls: ['./my-feedback.component.css']
})
export class MyFeedbackComponent implements OnInit {
  /**
   * Signal holding the list of user's feedback entries.
   */
  feedbackList = signal<FeedbackResponse[]>([]);

  /**
   * Loading state signal.
   */
  loading = signal<boolean>(true);

  /**
   * Error message signal (if API call fails).
   */
  errorMessage = signal<string | null>(null);

  /**
   * Pagination state.
   */
  pageIndex = signal<number>(0);
  pageSize = signal<number>(10);
  totalElements = signal<number>(0);

  constructor(private feedbackService: FeedbackService) {}

  /**
   * Lifecycle hook: Initialize component by loading user's feedback.
   */
  ngOnInit(): void {
    this.loadMyFeedback();
  }

  /**
   * Load the authenticated user's feedback history from the backend.
   * 
   * Endpoint: GET /api/v1/feedback/my?page={pageIndex}&size={pageSize}
   * Auth: JWT Bearer (authenticated user, RENTER role)
   */
  loadMyFeedback(): void {
    this.loading.set(true);
    this.errorMessage.set(null);

    this.feedbackService
      .getMyFeedback(this.pageIndex(), this.pageSize())
      .subscribe({
        next: (response) => {
          this.feedbackList.set(response.content || []);
          this.totalElements.set(response.totalElements || 0);
          this.loading.set(false);
        },
        error: (error) => {
          console.error('Error loading my feedback:', error);
          this.errorMessage.set(
            'Failed to load your feedback. Please try again later.'
          );
          this.loading.set(false);
        }
      });
  }

  /**
   * Handle paginator page change event.
   * 
   * @param event PageEvent from MatPaginator
   */
  onPageChange(event: PageEvent): void {
    this.pageIndex.set(event.pageIndex);
    this.pageSize.set(event.pageSize);
    this.loadMyFeedback();
  }

  /**
   * Format date from UTC to local timezone with Luxon.
   * 
   * @param dateString ISO 8601 date string from backend (UTC)
   * @returns Formatted date string (e.g., "Nov 8, 2025, 3:45 PM")
   */
  formatDate(dateString: string): string {
    return DateTime.fromISO(dateString, { zone: 'utc' })
      .toLocal()
      .toLocaleString(DateTime.DATETIME_MED);
  }

  /**
   * Generate array of star icons based on rating (0-5).
   * 
   * @param rating Numeric rating value (0-5)
   * @returns Array of 5 elements: 'star' for filled, 'star_border' for empty
   */
  getRatingStars(rating: number): string[] {
    const stars: string[] = [];
    for (let i = 1; i <= 5; i++) {
      stars.push(i <= rating ? 'star' : 'star_border');
    }
    return stars;
  }
}
