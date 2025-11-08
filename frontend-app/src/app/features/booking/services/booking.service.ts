import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../../environments/environment';

/**
 * Booking model representing a car rental.
 */
export interface Booking {
  id?: string;
  carId: string;
  renterId: string;
  startDate: string; // ISO 8601 format (UTC)
  endDate: string;   // ISO 8601 format (UTC)
  totalCost?: number;
  status?: 'PENDING' | 'CONFIRMED' | 'ACTIVE' | 'COMPLETED' | 'CANCELLED';
  createdAt?: string;
  updatedAt?: string;
}

/**
 * Cost estimation request.
 */
export interface CostEstimationRequest {
  carId: string;
  startDate: string; // ISO 8601 format (UTC)
  endDate: string;   // ISO 8601 format (UTC)
}

/**
 * Cost estimation response from pricing service.
 */
export interface CostEstimation {
  basePrice: number;
  durationDays: number;
  discounts: Array<{
    name: string;
    amount: number;
  }>;
  surcharges: Array<{
    name: string;
    amount: number;
  }>;
  totalCost: number;
  currency: string;
}

/**
 * Service for managing booking-related API operations.
 * 
 * Communicates with rental-service and pricing-service via API Gateway.
 */
@Injectable({
  providedIn: 'root'
})
export class BookingService {
  private readonly http = inject(HttpClient);
  private readonly rentalBaseUrl = `${environment.apiUrl}/v1/rentals`;
  private readonly pricingBaseUrl = `${environment.apiUrl}/v1/pricing`;

  /**
   * Estimates the cost of a booking.
   * 
   * @param request Cost estimation request with carId, startDate, endDate
   * @returns Observable of cost estimation details
   */
  estimateCost(request: CostEstimationRequest): Observable<CostEstimation> {
    return this.http.post<CostEstimation>(`${this.pricingBaseUrl}/estimate`, request);
  }

  /**
   * Creates a new booking.
   * 
   * @param booking Booking data
   * @returns Observable of created booking with assigned ID
   */
  createBooking(booking: Booking): Observable<Booking> {
    return this.http.post<Booking>(this.rentalBaseUrl, booking);
  }

  /**
   * Fetches details of a specific booking.
   * 
   * @param bookingId Booking identifier
   * @returns Observable of booking details
   */
  getBookingById(bookingId: string): Observable<Booking> {
    return this.http.get<Booking>(`${this.rentalBaseUrl}/${bookingId}`);
  }

  /**
   * Fetches all bookings for the authenticated renter.
   * 
   * @returns Observable of booking array
   */
  getMyBookings(): Observable<Booking[]> {
    return this.http.get<Booking[]>(`${this.rentalBaseUrl}/my`);
  }

  /**
   * Cancels an existing booking.
   * 
   * @param bookingId Booking identifier
   * @returns Observable of void
   */
  cancelBooking(bookingId: string): Observable<void> {
    return this.http.delete<void>(`${this.rentalBaseUrl}/${bookingId}`);
  }

  /**
   * Updates booking status (admin/owner only).
   * 
   * @param bookingId Booking identifier
   * @param status New status
   * @returns Observable of updated booking
   */
  updateBookingStatus(bookingId: string, status: string): Observable<Booking> {
    return this.http.patch<Booking>(`${this.rentalBaseUrl}/${bookingId}/status`, { status });
  }
}
