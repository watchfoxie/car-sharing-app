import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../../environments/environment';

/**
 * Car model representing a vehicle in the system.
 */
export interface Car {
  id: string;
  brand: string;
  model: string;
  year: number;
  category: string;
  dailyRate: number;
  status: 'AVAILABLE' | 'RENTED' | 'MAINTENANCE';
  ownerId: string;
  registrationNumber: string;
  color?: string;
  fuelType?: string;
  transmission?: string;
  seats?: number;
  imageUrl?: string;
}

/**
 * Pagination metadata for car list responses.
 */
export interface PageMetadata {
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

/**
 * Paginated response for car listings.
 */
export interface CarPage {
  content: Car[];
  page: PageMetadata;
}

/**
 * Filter criteria for car search.
 */
export interface CarFilter {
  brand?: string;
  category?: string;
  minPrice?: number;
  maxPrice?: number;
  status?: string;
}

/**
 * Sort options for car listings.
 */
export interface CarSort {
  field: 'brand' | 'model' | 'dailyRate' | 'year';
  direction: 'asc' | 'desc';
}

/**
 * Service for managing car-related API operations.
 * 
 * Communicates with car-service via API Gateway.
 * Base URL: /v1/cars
 */
@Injectable({
  providedIn: 'root'
})
export class CarService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiUrl}/v1/cars`;

  /**
   * Fetches paginated list of cars with optional filters and sorting.
   * 
   * @param page Page number (0-indexed)
   * @param size Page size
   * @param filter Optional filter criteria
   * @param sort Optional sort configuration
   * @returns Observable of paginated car response
   */
  getCars(
    page: number = 0,
    size: number = 12,
    filter?: CarFilter,
    sort?: CarSort
  ): Observable<CarPage> {
    let params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString());

    // Apply filters
    if (filter) {
      if (filter.brand) {
        params = params.set('brand', filter.brand);
      }
      if (filter.category) {
        params = params.set('category', filter.category);
      }
      if (filter.minPrice !== undefined) {
        params = params.set('minPrice', filter.minPrice.toString());
      }
      if (filter.maxPrice !== undefined) {
        params = params.set('maxPrice', filter.maxPrice.toString());
      }
      if (filter.status) {
        params = params.set('status', filter.status);
      }
    }

    // Apply sorting
    if (sort) {
      params = params.set('sort', `${sort.field},${sort.direction}`);
    }

    return this.http.get<CarPage>(this.baseUrl, { params });
  }

  /**
   * Fetches details of a specific car by ID.
   * 
   * @param carId Car identifier
   * @returns Observable of car details
   */
  getCarById(carId: string): Observable<Car> {
    return this.http.get<Car>(`${this.baseUrl}/${carId}`);
  }

  /**
   * Creates a new car (owner only).
   * 
   * @param car Car data without ID
   * @returns Observable of created car with assigned ID
   */
  createCar(car: Omit<Car, 'id'>): Observable<Car> {
    return this.http.post<Car>(this.baseUrl, car);
  }

  /**
   * Updates an existing car (owner only).
   * 
   * @param carId Car identifier
   * @param car Updated car data
   * @returns Observable of updated car
   */
  updateCar(carId: string, car: Partial<Car>): Observable<Car> {
    return this.http.put<Car>(`${this.baseUrl}/${carId}`, car);
  }

  /**
   * Deletes a car (owner/admin only).
   * 
   * @param carId Car identifier
   * @returns Observable of void
   */
  deleteCar(carId: string): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/${carId}`);
  }

  /**
   * Gets list of unique brands available in the system.
   * 
   * @returns Observable of brand array
   */
  getBrands(): Observable<string[]> {
    return this.http.get<string[]>(`${this.baseUrl}/brands`);
  }

  /**
   * Gets list of unique categories available in the system.
   * 
   * @returns Observable of category array
   */
  getCategories(): Observable<string[]> {
    return this.http.get<string[]>(`${this.baseUrl}/categories`);
  }
}
