/**
 * Car Domain Models
 * 
 * TypeScript interfaces matching the backend Car Service DTOs.
 * 
 * @module CarModels
 */

/**
 * Car Category Enum
 */
export enum CarCategory {
  ECONOMY = 'ECONOMY',
  COMPACT = 'COMPACT',
  SEDAN = 'SEDAN',
  SUV = 'SUV',
  LUXURY = 'LUXURY',
  VAN = 'VAN',
  SPORT = 'SPORT'
}

/**
 * Car Status Enum
 */
export enum CarStatus {
  AVAILABLE = 'AVAILABLE',
  RENTED = 'RENTED',
  MAINTENANCE = 'MAINTENANCE',
  UNAVAILABLE = 'UNAVAILABLE'
}

/**
 * Car Entity (full details)
 */
export interface Car {
  id: string;
  licensePlate: string;
  brand: string;
  model: string;
  year: number;
  category: CarCategory;
  pricePerDay: number;
  status: CarStatus;
  location: string;
  ownerId: string;
  specifications?: CarSpecifications;
  images?: string[];
  averageRating?: number;
  totalFeedback?: number;
  createdAt: string;
  updatedAt: string;
}

/**
 * Car Specifications (optional details)
 */
export interface CarSpecifications {
  fuelType?: string;
  transmission?: string;
  seats?: number;
  doors?: number;
  color?: string;
  mileage?: number;
  features?: string[];
}

/**
 * Car List Item (summary for list views)
 */
export interface CarListItem {
  id: string;
  licensePlate: string;
  brand: string;
  model: string;
  year: number;
  category: CarCategory;
  pricePerDay: number;
  status: CarStatus;
  location: string;
  averageRating?: number;
  totalFeedback?: number;
  thumbnailUrl?: string;
}

/**
 * Car Filter Parameters
 */
export interface CarFilterParams {
  brand?: string;
  category?: CarCategory;
  priceMin?: number;
  priceMax?: number;
  location?: string;
  status?: CarStatus;
}

/**
 * Car Sort Options
 */
export enum CarSortOption {
  BRAND_ASC = 'brand,asc',
  BRAND_DESC = 'brand,desc',
  PRICE_ASC = 'pricePerDay,asc',
  PRICE_DESC = 'pricePerDay,desc',
  YEAR_ASC = 'year,asc',
  YEAR_DESC = 'year,desc'
}

/**
 * Pagination Parameters
 */
export interface PaginationParams {
  page: number;
  size: number;
  sort?: CarSortOption;
}

/**
 * Paginated Car Response
 */
export interface PaginatedCarResponse {
  content: CarListItem[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
  first: boolean;
  last: boolean;
}

/**
 * Create Car Request
 */
export interface CreateCarRequest {
  licensePlate: string;
  brand: string;
  model: string;
  year: number;
  category: CarCategory;
  pricePerDay: number;
  location: string;
  specifications?: CarSpecifications;
  images?: string[];
}

/**
 * Update Car Request
 */
export interface UpdateCarRequest {
  pricePerDay?: number;
  status?: CarStatus;
  location?: string;
  specifications?: CarSpecifications;
  images?: string[];
}
