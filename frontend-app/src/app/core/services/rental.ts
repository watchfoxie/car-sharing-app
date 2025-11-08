import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { DateTime } from 'luxon';

import { environment } from '../../../environments/environment';
import { Page } from '../models/api';

export type RentalStatus = 'PENDING' | 'CONFIRMED' | 'PICKED_UP' | 'RETURNED' | 'RETURN_APPROVED' | 'CANCELLED';

export interface RentalResponse {
  id: number;
  renterId: string;
  carsId: number;
  pickupDatetime: string;
  returnDatetime?: string | null;
  pickupLocation?: string | null;
  returnLocation?: string | null;
  status: RentalStatus;
  estimatedCost?: number | null;
  finalCost?: number | null;
  idempotencyKey?: string | null;
  createdDate?: string | null;
  lastModifiedDate?: string | null;
}

export interface CreateRentalPayload {
  carsId: number;
  pickupDatetime: string;
  returnDatetime: string;
  pickupLocation?: string;
  returnLocation?: string;
  idempotencyKey?: string;
}

export interface UpdatePickupPayload {
  pickupDatetime: string;
  pickupLocation?: string;
}

export interface UpdateReturnPayload {
  returnDatetime: string;
  returnLocation?: string;
}

export interface ApproveReturnPayload {
  finalCost?: number;
  notes?: string;
}

@Injectable({
  providedIn: 'root',
})
export class RentalService {
  constructor(private readonly http: HttpClient) {}

  createRental(payload: CreateRentalPayload): Observable<RentalResponse> {
    return this.http.post<RentalResponse>(`${environment.apiUrl}/api/v1/rentals`, payload);
  }

  getMyRentals(page: number, size = 10): Observable<Page<RentalResponse>> {
    const params = new HttpParams().set('page', page).set('size', size);
    return this.http.get<Page<RentalResponse>>(`${environment.apiUrl}/api/v1/rentals/my`, { params });
  }

  getRentalsForCar(carId: number, page: number, size = 10): Observable<Page<RentalResponse>> {
    const params = new HttpParams().set('page', page).set('size', size);
    return this.http.get<Page<RentalResponse>>(`${environment.apiUrl}/api/v1/rentals/car/${carId}`, { params });
  }

  cancelRental(id: number): Observable<void> {
    return this.http.delete<void>(`${environment.apiUrl}/api/v1/rentals/${id}`);
  }

  toIsoUtc(date: DateTime): string {
    return (
      date.toUTC().toISO({ suppressMilliseconds: true }) ??
      date.toUTC().toISO() ??
      date.toUTC().toString()
    );
  }

  toLocalDateTime(isoUtc: string): DateTime {
    return DateTime.fromISO(isoUtc, { zone: 'utc' }).setZone(DateTime.local().zoneName ?? 'local');
  }
}
