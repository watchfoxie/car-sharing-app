import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';

import { environment } from '../../../environments/environment';
import { Page } from '../models/api';

export interface FeedbackResponse {
  id: number;
  carsId: number;
  reviewerId: string | null;
  rating: number;
  comment?: string | null;
  createdDate: string;
}

export interface CreateFeedbackPayload {
  carsId: number;
  rating: number;
  comment?: string;
}

export interface CarFeedbackSummary {
  carsId: number;
  avgRating: number | null;
  feedbackCount: number;
}

export interface RatingDistribution {
  histogram: Record<number, number>;
}

export interface TopCarRating {
  carsId: number;
  avgRating: number;
  feedbackCount: number;
}

@Injectable({
  providedIn: 'root',
})
export class FeedbackService {
  constructor(private readonly http: HttpClient) {}

  getFeedbackForCar(carsId: number, page: number, size = 5): Observable<Page<FeedbackResponse>> {
    const params = new HttpParams().set('page', page).set('size', size);
    return this.http.get<Page<FeedbackResponse>>(`${environment.apiUrl}/api/v1/feedback/cars/${carsId}`, {
      params
    });
  }

  getSummaryForCar(carsId: number): Observable<CarFeedbackSummary> {
    return this.http.get<CarFeedbackSummary>(`${environment.apiUrl}/api/v1/feedback/cars/${carsId}/summary`);
  }

  createFeedback(payload: CreateFeedbackPayload): Observable<FeedbackResponse> {
    return this.http.post<FeedbackResponse>(`${environment.apiUrl}/api/v1/feedback`, payload);
  }

  deleteFeedback(id: number): Observable<void> {
    return this.http.delete<void>(`${environment.apiUrl}/api/v1/feedback/${id}`);
  }

  getTopCars(limit = 5, minFeedbackCount = 3): Observable<TopCarRating[]> {
    const params = new HttpParams().set('limit', limit).set('minFeedbackCount', minFeedbackCount);
    return this.http.get<TopCarRating[]>(`${environment.apiUrl}/api/v1/feedback/reports/top-cars`, { params });
  }

  getDistribution(carsId: number): Observable<RatingDistribution> {
    const params = new HttpParams().set('carsId', carsId);
    return this.http.get<RatingDistribution>(`${environment.apiUrl}/api/v1/feedback/reports/distribution`, { params });
  }
}
