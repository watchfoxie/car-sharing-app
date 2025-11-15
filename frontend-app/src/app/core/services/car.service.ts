/**
 * Car Service
 * 
 * HTTP client service for Car management operations.
 * Communicates with the backend car-service via API Gateway.
 * 
 * @module CarService
 */

import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import {
  Car,
  CarListItem,
  PaginatedCarResponse,
  CarFilterParams,
  PaginationParams,
  CreateCarRequest,
  UpdateCarRequest
} from '../models/car.model';

/**
 * Car Service
 * 
 * Provides methods for:
 * - Listing cars with filters, sorting, and pagination
 * - Getting car details by ID
 * - Creating, updating, and deleting cars (OWNER role)
 * - Searching available cars
 */
@Injectable({
  providedIn: 'root'
})
export class CarService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiUrl}/cars`;

  /**
   * Get paginated list of cars with optional filters and sorting
   * 
   * @param filters - Filter criteria (brand, category, priceMin, priceMax, location, status)
   * @param pagination - Pagination parameters (page, size, sort)
   * @returns Observable of paginated car response
   */
  getCars(
    filters?: CarFilterParams,
    pagination?: PaginationParams
  ): Observable<PaginatedCarResponse> {
    let params = new HttpParams();

    // Apply filters
    if (filters) {
      if (filters.brand) {
        params = params.set('brand', filters.brand);
      }
      if (filters.category) {
        params = params.set('category', filters.category);
      }
      if (filters.priceMin !== undefined) {
        params = params.set('priceMin', filters.priceMin.toString());
      }
      if (filters.priceMax !== undefined) {
        params = params.set('priceMax', filters.priceMax.toString());
      }
      if (filters.location) {
        params = params.set('location', filters.location);
      }
      if (filters.status) {
        params = params.set('status', filters.status);
      }
    }

    // Apply pagination
    if (pagination) {
      params = params.set('page', pagination.page.toString());
      params = params.set('size', pagination.size.toString());
      if (pagination.sort) {
        params = params.set('sort', pagination.sort);
      }
    } else {
      // Default pagination
      params = params.set('page', '0');
      params = params.set('size', '10');
    }

    return this.http.get<PaginatedCarResponse>(this.baseUrl, { params });
  }

  /**
   * Get car details by ID
   * 
   * @param carId - Car unique identifier
   * @returns Observable of car details
   */
  getCarById(carId: string): Observable<Car> {
    return this.http.get<Car>(`${this.baseUrl}/${carId}`);
  }

  /**
   * Create a new car (OWNER role required)
   * 
   * @param carData - Car creation request
   * @returns Observable of created car
   */
  createCar(carData: CreateCarRequest): Observable<Car> {
    return this.http.post<Car>(this.baseUrl, carData);
  }

  /**
   * Update car details (OWNER role required)
   * 
   * @param carId - Car unique identifier
   * @param updates - Partial car update request
   * @returns Observable of updated car
   */
  updateCar(carId: string, updates: UpdateCarRequest): Observable<Car> {
    return this.http.put<Car>(`${this.baseUrl}/${carId}`, updates);
  }

  /**
   * Delete a car (OWNER role required)
   * 
   * @param carId - Car unique identifier
   * @returns Observable of void
   */
  deleteCar(carId: string): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/${carId}`);
  }

  /**
   * Get cars owned by the current user (OWNER role)
   * 
   * @param pagination - Pagination parameters
   * @returns Observable of paginated car response
   */
  getOwnedCars(pagination?: PaginationParams): Observable<PaginatedCarResponse> {
    let params = new HttpParams();

    if (pagination) {
      params = params.set('page', pagination.page.toString());
      params = params.set('size', pagination.size.toString());
      if (pagination.sort) {
        params = params.set('sort', pagination.sort);
      }
    } else {
      params = params.set('page', '0');
      params = params.set('size', '10');
    }

    return this.http.get<PaginatedCarResponse>(`${this.baseUrl}/owned`, { params });
  }

  /**
   * Search available cars by location and date range
   * 
   * @param location - Location filter
   * @param startDate - Start date (ISO 8601 format)
   * @param endDate - End date (ISO 8601 format)
   * @param pagination - Pagination parameters
   * @returns Observable of paginated car response
   */
  searchAvailableCars(
    location: string,
    startDate: string,
    endDate: string,
    pagination?: PaginationParams
  ): Observable<PaginatedCarResponse> {
    let params = new HttpParams()
      .set('location', location)
      .set('startDate', startDate)
      .set('endDate', endDate);

    if (pagination) {
      params = params.set('page', pagination.page.toString());
      params = params.set('size', pagination.size.toString());
      if (pagination.sort) {
        params = params.set('sort', pagination.sort);
      }
    } else {
      params = params.set('page', '0');
      params = params.set('size', '10');
    }

    return this.http.get<PaginatedCarResponse>(`${this.baseUrl}/available`, { params });
  }
}
