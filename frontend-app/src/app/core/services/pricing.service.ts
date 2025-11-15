/**
 * Pricing Service
 * 
 * HTTP client service for pricing calculation operations.
 * Communicates with the backend pricing-rules-service via API Gateway.
 * 
 * @module PricingService
 */

import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import {
  PricingCalculationRequest,
  PricingCalculationResponse
} from '../models/pricing.model';

/**
 * Pricing Service
 * 
 * Provides real-time pricing calculation based on:
 * - Base daily rate
 * - Number of rental days
 * - Seasonal pricing rules
 * - Weekend multipliers
 * - Insurance fees
 */
@Injectable({
  providedIn: 'root'
})
export class PricingService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiUrl}/pricing`;

  /**
   * Calculate rental price
   * 
   * Sends a POST request to /api/v1/pricing/calculate with:
   * - carId
   * - startDate (UTC ISO 8601)
   * - endDate (UTC ISO 8601)
   * - includeInsurance (optional)
   * 
   * @param request - Pricing calculation request
   * @returns Observable of pricing calculation response with breakdown
   */
  calculatePrice(request: PricingCalculationRequest): Observable<PricingCalculationResponse> {
    return this.http.post<PricingCalculationResponse>(
      `${this.baseUrl}/calculate`,
      request
    );
  }

  /**
   * Estimate price based on car's base daily rate
   * 
   * Simple estimation without backend call (for quick UI feedback).
   * Does not include dynamic pricing rules.
   * 
   * @param dailyRate - Car's base price per day
   * @param startDate - Start date (local)
   * @param endDate - End date (local)
   * @param includeInsurance - Whether to add insurance (10% of base)
   * @returns Estimated total price
   */
  estimatePrice(
    dailyRate: number,
    startDate: Date,
    endDate: Date,
    includeInsurance: boolean = false
  ): number {
    const days = Math.ceil(
      (endDate.getTime() - startDate.getTime()) / (1000 * 60 * 60 * 24)
    );
    
    if (days <= 0) {
      return 0;
    }
    
    let total = dailyRate * days;
    
    if (includeInsurance) {
      total += total * 0.1; // 10% insurance fee
    }
    
    return total;
  }
}
