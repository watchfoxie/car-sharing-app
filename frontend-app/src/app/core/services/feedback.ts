import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';

/**
 * Feedback response DTO from backend.
 */
export interface FeedbackResponse {
  id: number;
  rating: number; // 0-5
  comment: string | null;
  carsId: number;
  reviewerId: string;
  createdDate: string; // ISO 8601 UTC
  lastModifiedDate: string;
  createdBy: string;
  lastModifiedBy: string;
}

/**
 * Request DTO for creating feedback.
 */
export interface CreateFeedbackRequest {
  carsId: number;
  rating: number; // 0-5
  comment: string | null;
}

/**
 * Car feedback summary with aggregated rating.
 */
export interface CarFeedbackSummary {
  carsId: number;
  avgRating: number | null; // null if no feedback
  feedbackCount: number;
}

/**
 * Paginated response wrapper.
 */
export interface Page<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number; // page index (0-based)
}

/**
 * FeedbackService handles all feedback-related API calls.
 * 
 * Backend endpoints (via API Gateway):
 * - POST /api/v1/feedback - create feedback (authenticated)
 * - GET /api/v1/feedback/cars/{carsId} - list feedback per car (public)
 * - GET /api/v1/feedback/cars/{carsId}/summary - get aggregated rating (public)
 * - GET /api/v1/feedback/my - get user's feedback history (authenticated)
 * - DELETE /api/v1/feedback/{id} - delete feedback (owner only)
 */
@Injectable({
  providedIn: 'root',
})
export class FeedbackService {
  private readonly http = inject(HttpClient);
  private readonly apiUrl = `${environment.apiUrl}/feedback`;

  /**
   * Create new feedback for a car.
   * 
   * @param request CreateFeedbackRequest with carsId, rating, comment
   * @returns Observable<FeedbackResponse>
   */
  createFeedback(request: CreateFeedbackRequest): Observable<FeedbackResponse> {
    return this.http.post<FeedbackResponse>(this.apiUrl, request);
  }

  /**
   * Get feedback list for a specific car (public endpoint).
   * 
   * @param carsId Car ID
   * @param page Page index (0-based)
   * @param size Page size
   * @returns Observable<Page<FeedbackResponse>>
   */
  getFeedbackByCar(
    carsId: number,
    page: number = 0,
    size: number = 10
  ): Observable<Page<FeedbackResponse>> {
    const params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString());

    return this.http.get<Page<FeedbackResponse>>(
      `${this.apiUrl}/cars/${carsId}`,
      { params }
    );
  }

  /**
   * Get aggregated feedback summary for a car (public endpoint).
   * 
   * @param carsId Car ID
   * @returns Observable<CarFeedbackSummary>
   */
  getFeedbackSummary(carsId: number): Observable<CarFeedbackSummary> {
    return this.http.get<CarFeedbackSummary>(
      `${this.apiUrl}/cars/${carsId}/summary`
    );
  }

  /**
   * Get authenticated user's feedback history.
   * 
   * @param page Page index (0-based)
   * @param size Page size
   * @returns Observable<Page<FeedbackResponse>>
   */
  getMyFeedback(
    page: number = 0,
    size: number = 10
  ): Observable<Page<FeedbackResponse>> {
    const params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString());

    return this.http.get<Page<FeedbackResponse>>(`${this.apiUrl}/my`, {
      params,
    });
  }

  /**
   * Delete feedback (owner only).
   * 
   * @param feedbackId Feedback ID
   * @returns Observable<void>
   */
  deleteFeedback(feedbackId: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${feedbackId}`);
  }
}
