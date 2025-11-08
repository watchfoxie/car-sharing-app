import { ApplicationConfig, provideBrowserGlobalErrorListeners, provideZonelessChangeDetection, APP_INITIALIZER } from '@angular/core';
import { provideRouter, withPreloading, PreloadAllModules } from '@angular/router';
import { provideHttpClient, withInterceptors, withFetch } from '@angular/common/http';
import { provideAnimations } from '@angular/platform-browser/animations';
import { provideOAuthClient } from 'angular-oauth2-oidc';

import { routes } from './app.routes';
import { provideClientHydration, withEventReplay } from '@angular/platform-browser';
import { jwtInterceptor } from './core/interceptors/jwt.interceptor';
import { AuthService } from './core/services/auth.service';

/**
 * Initialize authentication before app startup.
 * 
 * This ensures that:
 * - OAuth2 discovery document is loaded
 * - Existing session is restored (if valid)
 * - User is authenticated before rendering app
 */
export function initializeAuth(authService: AuthService) {
  return () => authService.initializeAuth();
}

/**
 * Application configuration with providers for:
 * - Zoneless change detection (Angular 20+ performance optimization)
 * - Router with preloading strategy (load all lazy modules after initial render)
 * - HTTP client with fetch API and JWT interceptor
 * - OAuth2 client for Keycloak integration
 * - Browser animations for Material components
 * - Client hydration with event replay for SSR
 * - Authentication initialization before app startup
 */
export const appConfig: ApplicationConfig = {
  providers: [
    provideBrowserGlobalErrorListeners(),
    provideZonelessChangeDetection(),
    provideRouter(
      routes,
      withPreloading(PreloadAllModules) // Strategic preloading: load all lazy modules after app bootstrap
    ),
    provideHttpClient(
      withFetch(), // Use Fetch API instead of XMLHttpRequest (Angular 17+)
      withInterceptors([jwtInterceptor]) // JWT token injection for API calls
    ),
    provideOAuthClient(), // OAuth2/OIDC client for Keycloak
    provideAnimations(),
    provideClientHydration(withEventReplay()),
    {
      provide: APP_INITIALIZER,
      useFactory: initializeAuth,
      deps: [AuthService],
      multi: true
    }
  ]
};
