/**
 * Rental Domain Models
 * 
 * TypeScript interfaces for rental/booking operations.
 * 
 * @module RentalModels
 */

/**
 * Rental Status Enum
 */
export enum RentalStatus {
  PENDING = 'PENDING',
  APPROVED = 'APPROVED',
  ACTIVE = 'ACTIVE',
  COMPLETED = 'COMPLETED',
  CANCELLED = 'CANCELLED'
}

/**
 * Rental Entity
 */
export interface Rental {
  id: string;
  carId: string;
  renterId: string;
  ownerId: string;
  startDate: string; // ISO 8601 format (UTC)
  endDate: string;   // ISO 8601 format (UTC)
  pickupDate?: string;
  returnDate?: string;
  status: RentalStatus;
  totalPrice: number;
  includeInsurance: boolean;
  createdAt: string;
  updatedAt: string;
}

/**
 * Create Rental Request (booking)
 */
export interface CreateRentalRequest {
  carId: string;
  startDate: string; // ISO 8601 format (UTC)
  endDate: string;   // ISO 8601 format (UTC)
  includeInsurance?: boolean;
}

/**
 * Update Rental Status Request
 */
export interface UpdateRentalStatusRequest {
  status: RentalStatus;
  notes?: string;
}

/**
 * Pickup Rental Request
 */
export interface PickupRentalRequest {
  pickupDate: string; // ISO 8601 format (UTC)
  mileage?: number;
  notes?: string;
}

/**
 * Return Rental Request
 */
export interface ReturnRentalRequest {
  returnDate: string; // ISO 8601 format (UTC)
  mileage?: number;
  damageNotes?: string;
}

/**
 * Rental List Item (summary)
 */
export interface RentalListItem {
  id: string;
  carId: string;
  carBrand: string;
  carModel: string;
  licensePlate: string;
  renterId: string;
  renterName?: string;
  ownerId: string;
  ownerName?: string;
  startDate: string;
  endDate: string;
  status: RentalStatus;
  totalPrice: number;
}

/**
 * Paginated Rental Response
 */
export interface PaginatedRentalResponse {
  content: RentalListItem[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
  first: boolean;
  last: boolean;
}
