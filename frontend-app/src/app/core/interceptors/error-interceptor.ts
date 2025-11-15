/**
 * Error Interceptor
 * 
 * Global HTTP error handler that:
 * - Parses RFC 7807 Problem Details responses
 * - Displays user-friendly error messages via MatSnackBar
 * - Logs errors for debugging
 * - Handles network errors gracefully
 * 
 * @module ErrorInterceptor
 */

import { HttpInterceptorFn, HttpErrorResponse } from '@angular/common/http';
import { inject } from '@angular/core';
import { MatSnackBar } from '@angular/material/snack-bar';
import { catchError, throwError } from 'rxjs';

/**
 * RFC 7807 Problem Details interface
 */
interface ProblemDetails {
  type?: string;
  title: string;
  status: number;
  detail?: string;
  instance?: string;
}

/**
 * Error HTTP Interceptor (Functional)
 * 
 * Catches HTTP errors and displays user-friendly messages.
 * Supports RFC 7807 Problem Details format from backend.
 */
export const errorInterceptor: HttpInterceptorFn = (req, next) => {
  const snackBar = inject(MatSnackBar);

  return next(req).pipe(
    catchError((error: HttpErrorResponse) => {
      const errorMessage = getErrorMessage(error);

      // Display error message to user (skip 401 - handled by auth interceptor)
      if (error.status !== 401) {
        snackBar.open(errorMessage, 'Close', {
          duration: 5000,
          horizontalPosition: 'right',
          verticalPosition: 'top',
          panelClass: ['error-snackbar']
        });
      }

      return throwError(() => error);
    })
  );
};

/**
 * Extract error message from HTTP error response
 */
function getErrorMessage(error: HttpErrorResponse): string {
  if (error.error instanceof ErrorEvent) {
    // Client-side or network error
    console.error('Client-side error:', error.error);
    return `Network error: ${error.error.message}`;
  }

  // Backend error
  console.error(`Backend error ${error.status}:`, error.error);

  // Try RFC 7807 Problem Details or generic message field
  if (error.error && typeof error.error === 'object') {
    const problemDetails = error.error as ProblemDetails;
    if (problemDetails.detail) return problemDetails.detail;
    if (problemDetails.title) return problemDetails.title;
    if (error.error.message) return error.error.message;
  }

  // Fallback to HTTP status-based messages
  return getHttpStatusMessage(error.status);
}

/**
 * Get user-friendly message based on HTTP status code
 */
function getHttpStatusMessage(status: number): string {
  const statusMessages: Record<number, string> = {
    400: 'Invalid request. Please check your input.',
    401: 'Authentication required. Please log in.',
    403: 'You do not have permission to perform this action.',
    404: 'The requested resource was not found.',
    409: 'Conflict: The operation could not be completed.',
    500: 'Server error. Please try again later.',
    503: 'Service temporarily unavailable. Please try again.'
  };

  return statusMessages[status] || 
         (status >= 500 ? 'Server error. Please try again later.' : 'An unexpected error occurred');
}
