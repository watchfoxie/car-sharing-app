/**
 * Feedback Domain Models
 * 
 * TypeScript interfaces for feedback/rating operations.
 * 
 * @module FeedbackModels
 */

/**
 * Feedback Entity
 */
export interface Feedback {
  id: string;
  carId: string;
  rentalId: string;
  renterId: string;
  renterName?: string;
  rating: number; // 1-5
  comment?: string;
  createdAt: string; // ISO 8601 format (UTC)
}

/**
 * Create Feedback Request
 */
export interface CreateFeedbackRequest {
  carId: string;
  rentalId: string;
  rating: number; // 1-5
  comment?: string;
}

/**
 * Rating Distribution Type
 * Allows both numeric and string indexing for ratings 1-5
 */
export type RatingDistribution = {
  '1': number;
  '2': number;
  '3': number;
  '4': number;
  '5': number;
  [key: string]: number;
};

/**
 * Feedback Aggregation (for car rating summary)
 */
export interface FeedbackAggregation {
  carId: string;
  averageRating: number;
  totalFeedback: number;
  ratingDistribution: RatingDistribution;
}

/**
 * Paginated Feedback Response
 */
export interface PaginatedFeedbackResponse {
  content: Feedback[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
  first: boolean;
  last: boolean;
}
