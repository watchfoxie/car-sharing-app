/**
 * Feedback Service
 * 
 * HTTP client service for feedback/rating operations.
 * Communicates with the backend feedback-service via API Gateway.
 * 
 * @module FeedbackService
 */

import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import {
  Feedback,
  CreateFeedbackRequest,
  FeedbackAggregation,
  PaginatedFeedbackResponse
} from '../models/feedback.model';

/**
 * Feedback Service
 * 
 * Provides methods for:
 * - Submitting feedback after rental completion (RENTER role)
 * - Viewing feedback for specific cars
 * - Getting aggregated ratings
 * - Listing all feedback (ADMIN/MANAGER role)
 */
@Injectable({
  providedIn: 'root'
})
export class FeedbackService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiUrl}/feedback`;

  /**
   * Submit feedback for a completed rental (RENTER role)
   * 
   * @param request - Feedback creation request
   * @returns Observable of created feedback
   */
  submitFeedback(request: CreateFeedbackRequest): Observable<Feedback> {
    return this.http.post<Feedback>(this.baseUrl, request);
  }

  /**
   * Get feedback aggregation for a specific car
   * 
   * @param carId - Car unique identifier
   * @returns Observable of feedback aggregation
   */
  getCarFeedbackAggregation(carId: string): Observable<FeedbackAggregation> {
    return this.http.get<FeedbackAggregation>(
      `${this.baseUrl}/car/${carId}/aggregation`
    );
  }

  /**
   * Get feedback list for a specific car
   * 
   * @param carId - Car unique identifier
   * @param page - Page number (0-indexed)
   * @param size - Page size
   * @returns Observable of paginated feedback
   */
  getCarFeedback(
    carId: string,
    page: number = 0,
    size: number = 10
  ): Observable<PaginatedFeedbackResponse> {
    const params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString());

    return this.http.get<PaginatedFeedbackResponse>(
      `${this.baseUrl}/car/${carId}`,
      { params }
    );
  }

  /**
   * Get all feedback (ADMIN/MANAGER role)
   * 
   * @param page - Page number (0-indexed)
   * @param size - Page size
   * @returns Observable of paginated feedback
   */
  getAllFeedback(
    page: number = 0,
    size: number = 10
  ): Observable<PaginatedFeedbackResponse> {
    const params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString());

    return this.http.get<PaginatedFeedbackResponse>(this.baseUrl, { params });
  }

  /**
   * Check if renter can submit feedback for a rental
   * 
   * @param rentalId - Rental unique identifier
   * @returns Observable of boolean (true if feedback can be submitted)
   */
  canSubmitFeedback(rentalId: string): Observable<boolean> {
    return this.http.get<boolean>(
      `${this.baseUrl}/can-submit/${rentalId}`
    );
  }
}
