/**
 * HTTP Auth Interceptor
 * 
 * Automatically attaches JWT access token to outgoing HTTP requests.
 * Handles token expiration and 401 Unauthorized responses.
 * 
 * @module AuthInterceptor
 */

import { HttpInterceptorFn, HttpErrorResponse } from '@angular/common/http';
import { inject } from '@angular/core';
import { Auth } from '../services/auth';
import { catchError, throwError } from 'rxjs';

/**
 * Auth HTTP Interceptor (Functional)
 * 
 * Features:
 * - Adds Authorization Bearer header with JWT access token
 * - Handles 401 Unauthorized responses by triggering re-authentication
 * - Skips token attachment for Keycloak OAuth endpoints
 * - Adds X-Request-ID header for tracing
 * 
 * @param req - HTTP request
 * @param next - Next handler in the chain
 * @returns Observable of HTTP response
 */
export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const authService = inject(Auth);
  
  // Skip adding token for OAuth endpoints (Keycloak)
  const isOAuthEndpoint = req.url.includes('/realms/') || 
                          req.url.includes('/protocol/openid-connect/');
  
  if (!isOAuthEndpoint && authService.isAuthenticated()) {
    const token = authService.getAccessToken();
    
    if (token) {
      // Clone request and add Authorization header
      req = req.clone({
        setHeaders: {
          Authorization: `Bearer ${token}`,
          'X-Request-ID': crypto.randomUUID() // For tracing
        }
      });
    }
  }
  
  // Handle the request and catch errors
  return next(req).pipe(
    catchError((error: HttpErrorResponse) => {
      if (error.status === 401) {
        // Unauthorized - token expired or invalid
        console.error('Unauthorized request - redirecting to login');
        authService.login(globalThis.location.pathname);
      }
      
      // Re-throw error for component-level handling
      return throwError(() => error);
    })
  );
};

