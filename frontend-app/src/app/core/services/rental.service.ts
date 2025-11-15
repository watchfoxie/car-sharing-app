/**
 * Rental Service
 * 
 * HTTP client service for rental/booking operations.
 * Communicates with the backend rental-service via API Gateway.
 * 
 * @module RentalService
 */

import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import {
  Rental,
  CreateRentalRequest,
  UpdateRentalStatusRequest,
  PickupRentalRequest,
  ReturnRentalRequest,
  PaginatedRentalResponse,
  RentalStatus
} from '../models/rental.model';

/**
 * Rental Service
 * 
 * Provides methods for:
 * - Creating rental bookings (RENTER role)
 * - Approving/rejecting rentals (OWNER role)
 * - Pickup/return operations
 * - Listing rentals (by renter, by owner, by status)
 */
@Injectable({
  providedIn: 'root'
})
export class RentalService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiUrl}/rentals`;

  /**
   * Create a new rental booking (RENTER role)
   * 
   * @param request - Rental creation request
   * @returns Observable of created rental
   */
  createRental(request: CreateRentalRequest): Observable<Rental> {
    return this.http.post<Rental>(this.baseUrl, request);
  }

  /**
   * Get rental details by ID
   * 
   * @param rentalId - Rental unique identifier
   * @returns Observable of rental details
   */
  getRentalById(rentalId: string): Observable<Rental> {
    return this.http.get<Rental>(`${this.baseUrl}/${rentalId}`);
  }

  /**
   * Approve a rental (OWNER role)
   * 
   * @param rentalId - Rental unique identifier
   * @returns Observable of updated rental
   */
  approveRental(rentalId: string): Observable<Rental> {
    return this.http.put<Rental>(
      `${this.baseUrl}/${rentalId}/approve`,
      {}
    );
  }

  /**
   * Reject/Cancel a rental
   * 
   * @param rentalId - Rental unique identifier
   * @param reason - Cancellation reason
   * @returns Observable of updated rental
   */
  cancelRental(rentalId: string, reason?: string): Observable<Rental> {
    return this.http.put<Rental>(
      `${this.baseUrl}/${rentalId}/cancel`,
      { reason }
    );
  }

  /**
   * Mark rental as picked up
   * 
   * @param rentalId - Rental unique identifier
   * @param request - Pickup details
   * @returns Observable of updated rental
   */
  pickupRental(rentalId: string, request: PickupRentalRequest): Observable<Rental> {
    return this.http.put<Rental>(
      `${this.baseUrl}/${rentalId}/pickup`,
      request
    );
  }

  /**
   * Mark rental as returned
   * 
   * @param rentalId - Rental unique identifier
   * @param request - Return details
   * @returns Observable of updated rental
   */
  returnRental(rentalId: string, request: ReturnRentalRequest): Observable<Rental> {
    return this.http.put<Rental>(
      `${this.baseUrl}/${rentalId}/return`,
      request
    );
  }

  /**
   * Get rentals for the current renter (RENTER role)
   * 
   * @param page - Page number (0-indexed)
   * @param size - Page size
   * @param status - Optional status filter
   * @returns Observable of paginated rentals
   */
  getMyRentals(
    page: number = 0,
    size: number = 10,
    status?: RentalStatus
  ): Observable<PaginatedRentalResponse> {
    let params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString());

    if (status) {
      params = params.set('status', status);
    }

    return this.http.get<PaginatedRentalResponse>(
      `${this.baseUrl}/my-rentals`,
      { params }
    );
  }

  /**
   * Get rentals for the current owner's cars (OWNER role)
   * 
   * @param page - Page number (0-indexed)
   * @param size - Page size
   * @param status - Optional status filter
   * @returns Observable of paginated rentals
   */
  getOwnerRentals(
    page: number = 0,
    size: number = 10,
    status?: RentalStatus
  ): Observable<PaginatedRentalResponse> {
    let params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString());

    if (status) {
      params = params.set('status', status);
    }

    return this.http.get<PaginatedRentalResponse>(
      `${this.baseUrl}/owner-rentals`,
      { params }
    );
  }

  /**
   * Get all rentals (ADMIN/MANAGER role)
   * 
   * @param page - Page number (0-indexed)
   * @param size - Page size
   * @param status - Optional status filter
   * @returns Observable of paginated rentals
   */
  getAllRentals(
    page: number = 0,
    size: number = 10,
    status?: RentalStatus
  ): Observable<PaginatedRentalResponse> {
    let params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString());

    if (status) {
      params = params.set('status', status);
    }

    return this.http.get<PaginatedRentalResponse>(this.baseUrl, { params });
  }
}
