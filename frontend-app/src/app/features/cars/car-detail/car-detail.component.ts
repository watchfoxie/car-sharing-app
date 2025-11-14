/**
 * Car Detail Component
 * 
 * Displays complete car information including:
 * - Full specifications and images
 * - Location and availability
 * - Feedback aggregations and reviews
 * - Booking action button
 * 
 * @module CarDetailComponent
 */

import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router } from '@angular/router';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatChipsModule } from '@angular/material/chips';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatTabsModule } from '@angular/material/tabs';
import { MatDividerModule } from '@angular/material/divider';

import { CarService } from '../../../core/services/car.service';
import { FeedbackService } from '../../../core/services/feedback.service';
import { Auth } from '../../../core/services/auth';
import { Car, CarStatus } from '../../../core/models/car.model';
import { FeedbackAggregation, Feedback } from '../../../core/models/feedback.model';

@Component({
  selector: 'app-car-detail',
  standalone: true,
  imports: [
    CommonModule,
    MatCardModule,
    MatButtonModule,
    MatIconModule,
    MatChipsModule,
    MatProgressSpinnerModule,
    MatTabsModule,
    MatDividerModule
  ],
  templateUrl: './car-detail.component.html',
  styleUrl: './car-detail.component.scss'
})
export class CarDetailComponent implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly carService = inject(CarService);
  private readonly feedbackService = inject(FeedbackService);
  private readonly authService = inject(Auth);

  car = signal<Car | null>(null);
  feedbackAggregation = signal<FeedbackAggregation | null>(null);
  feedbackList = signal<Feedback[]>([]);
  isLoading = signal<boolean>(true);
  selectedImageIndex = signal<number>(0);
  
  readonly CarStatus = CarStatus;

  ngOnInit(): void {
    const carId = this.route.snapshot.paramMap.get('id');
    if (carId) {
      this.loadCarDetails(carId);
      this.loadFeedback(carId);
      this.loadFeedbackList(carId);
    } else {
      this.router.navigate(['/cars']);
    }
  }

  /**
   * Load car details
   */
  loadCarDetails(carId: string): void {
    this.carService.getCarById(carId).subscribe({
      next: (car) => {
        this.car.set(car);
        this.isLoading.set(false);
      },
      error: (error) => {
        console.error('Error loading car details:', error);
        this.isLoading.set(false);
        this.router.navigate(['/cars']);
      }
    });
  }

  /**
   * Load feedback aggregation
   */
  loadFeedback(carId: string): void {
    this.feedbackService.getCarFeedbackAggregation(carId).subscribe({
      next: (aggregation) => {
        this.feedbackAggregation.set(aggregation);
      },
      error: (error) => {
        console.error('Error loading feedback:', error);
      }
    });
  }

  /**
   * Load recent feedback list
   */
  loadFeedbackList(carId: string): void {
    this.feedbackService.getCarFeedback(carId, 0, 5).subscribe({
      next: (response) => {
        this.feedbackList.set(response.content);
      },
      error: (error) => {
        console.error('Error loading feedback list:', error);
      }
    });
  }

  /**
   * Navigate to booking form
   */
  bookCar(): void {
    const car = this.car();
    if (car && car.status === CarStatus.AVAILABLE) {
      this.router.navigate(['/booking', car.id]);
    }
  }

  /**
   * Select image to display
   */
  selectImage(index: number): void {
    this.selectedImageIndex.set(index);
  }

  /**
   * Get status badge class
   */
  getStatusClass(status: CarStatus): string {
    const statusClasses: Record<CarStatus, string> = {
      [CarStatus.AVAILABLE]: 'status-available',
      [CarStatus.RENTED]: 'status-rented',
      [CarStatus.MAINTENANCE]: 'status-maintenance',
      [CarStatus.UNAVAILABLE]: 'status-unavailable'
    };
    return statusClasses[status] || '';
  }

  /**
   * Get rating stars array (for display)
   */
  getRatingStars(rating: number): boolean[] {
    return Array.from({ length: 5 }, (_, i) => i < Math.round(rating));
  }

  /**
   * Get rating percentage for distribution bars
   */
  getRatingPercentage(count: number, total: number): number {
    return total > 0 ? (count / total) * 100 : 0;
  }

  /**
   * Check if user is authenticated
   */
  isAuthenticated(): boolean {
    return this.authService.isAuthenticated();
  }

  /**
   * Navigate back to car list
   */
  goBack(): void {
    this.router.navigate(['/cars']);
  }

  /**
   * TrackBy for feedback list
   */
  trackByFeedbackId(index: number, feedback: Feedback): string {
    return feedback.id;
  }
}
