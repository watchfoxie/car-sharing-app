/**
 * Pricing Domain Models
 * 
 * TypeScript interfaces for pricing calculation operations.
 * 
 * @module PricingModels
 */

/**
 * Pricing Calculation Request
 */
export interface PricingCalculationRequest {
  carId: string;
  startDate: string; // ISO 8601 format (UTC)
  endDate: string;   // ISO 8601 format (UTC)
  includeInsurance?: boolean;
}

/**
 * Pricing Calculation Response
 */
export interface PricingCalculationResponse {
  carId: string;
  basePrice: number;
  numberOfDays: number;
  dailyRate: number;
  seasonalMultiplier?: number;
  weekendMultiplier?: number;
  insuranceFee?: number;
  totalPrice: number;
  currency: string;
  breakdown: PriceBreakdown[];
}

/**
 * Price Breakdown Item
 */
export interface PriceBreakdown {
  date: string; // ISO 8601 format
  dailyRate: number;
  multiplier?: number;
  description: string;
}
