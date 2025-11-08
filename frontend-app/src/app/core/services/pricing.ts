import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { DateTime } from 'luxon';

import { environment } from '../../../environments/environment';
import type { VehicleCategory } from './car';

export type PricingUnit = 'MINUTE' | 'HOUR' | 'DAY';

export interface PriceBreakdownEntry {
  unit: PricingUnit;
  quantity: number;
  pricePerUnit: number;
  subtotal: number;
}

export interface PriceEstimateResponse {
  totalCost: number;
  totalDuration: string;
  vehicleCategory: VehicleCategory;
  pickupDatetime: string;
  returnDatetime: string;
  breakdown: PriceBreakdownEntry[];
  calculatedAt: string;
}

export interface CalculatePricePayload {
  vehicleCategory: VehicleCategory;
  pickupDatetime: string;
  returnDatetime: string;
}

@Injectable({
  providedIn: 'root',
})
export class PricingService {
  constructor(private readonly http: HttpClient) {}

  estimate(payload: CalculatePricePayload): Observable<PriceEstimateResponse> {
    return this.http.post<PriceEstimateResponse>(`${environment.apiUrl}/api/v1/pricing/calculate`, payload);
  }

  toLocalDateTime(isoUtc: string): DateTime {
    return DateTime.fromISO(isoUtc, { zone: 'utc' }).setZone(DateTime.local().zoneName ?? 'local');
  }
}
