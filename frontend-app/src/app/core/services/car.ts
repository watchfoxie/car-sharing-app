import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';

import { environment } from '../../../environments/environment';
import { Page } from '../models/api';

export type VehicleCategory = 'ECONOM' | 'STANDARD' | 'PREMIUM';

export interface Car {
  id: number;
  brand: string;
  model: string;
  registrationNumber: string;
  description?: string | null;
  imageUrl?: string | null;
  seats?: number | null;
  transmissionType?: string | null;
  fuelType?: string | null;
  category: VehicleCategory;
  dailyPrice: number;
  ownerId?: string | null;
  shareable?: boolean | null;
  archived?: boolean | null;
  avgRating?: number | null;
  createdDate?: string | null;
  lastModifiedDate?: string | null;
}

export interface CarListFilters {
  brand?: string;
  category?: VehicleCategory | null;
  minPrice?: number | null;
  maxPrice?: number | null;
  sort?: string;
  page?: number;
  size?: number;
}

export interface CarAvailabilityEvent {
  carId: number;
  shareable: boolean;
  archived: boolean;
}

@Injectable({
  providedIn: 'root',
})
export class CarService {
  constructor(private readonly http: HttpClient) {}

  listPublicCars(filters: CarListFilters): Observable<Page<Car>> {
    let params = new HttpParams();

    if (filters.brand) {
      params = params.set('brand', filters.brand);
    }
    if (filters.category) {
      params = params.set('category', filters.category);
    }
    if (filters.minPrice != null) {
      params = params.set('priceMin', filters.minPrice);
    }
    if (filters.maxPrice != null) {
      params = params.set('priceMax', filters.maxPrice);
    }
    if (filters.sort) {
      params = params.set('sort', filters.sort);
    }
    if (filters.page != null) {
      params = params.set('page', filters.page);
    }
    if (filters.size != null) {
      params = params.set('size', filters.size);
    }

    return this.http.get<Page<Car>>(`${environment.apiUrl}/api/v1/cars`, { params });
  }

  getCarById(id: number): Observable<Car> {
    return this.http.get<Car>(`${environment.apiUrl}/api/v1/cars/${id}`);
  }

  listenToAvailability(): Observable<CarAvailabilityEvent> {
    const url = `${environment.apiUrl}/api/v1/cars/availability-stream`;

    return new Observable<CarAvailabilityEvent>((subscriber) => {
      let eventSource: EventSource | null = null;

      try {
        eventSource = new EventSource(url, { withCredentials: true });
      } catch (error) {
        subscriber.error(error);
        return undefined;
      }

      eventSource.onmessage = (event) => {
        try {
          const payload = JSON.parse(event.data) as CarAvailabilityEvent;
          subscriber.next(payload);
        } catch (parseError) {
          console.error('Failed to parse availability event', parseError);
        }
      };

      eventSource.onerror = (error) => {
        console.warn('Availability stream error', error);
        subscriber.error(error);
      };

      return () => {
        eventSource?.close();
      };
    });
  }
}
