import { HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { AuthService } from '../services/auth.service';

/**
 * HTTP interceptor for JWT token injection.
 * 
 * Automatically adds Authorization header to all HTTP requests:
 * Authorization: Bearer <access_token>
 * 
 * Skips token injection for:
 * - Keycloak/OAuth2 endpoints (to avoid conflicts)
 * - External URLs (different origin)
 * 
 * Note: This is a functional interceptor (Angular 15+ style).
 */
export const jwtInterceptor: HttpInterceptorFn = (req, next) => {
  const authService = inject(AuthService);
  
  // Skip token injection for OAuth2 endpoints
  if (req.url.includes('/realms/') || req.url.includes('/protocol/openid-connect/')) {
    return next(req);
  }
  
  // Skip token injection for external URLs
  if (!req.url.startsWith('/') && !req.url.includes(window.location.hostname)) {
    return next(req);
  }
  
  // Get access token
  const token = authService.getAccessToken();
  
  // Inject token if available
  if (token) {
    req = req.clone({
      setHeaders: {
        Authorization: `Bearer ${token}`
      }
    });
  }
  
  return next(req);
};
