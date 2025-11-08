/**
 * Development environment configuration.
 * 
 * This configuration is used during local development (ng serve).
 * 
 * Backend services:
 * - API Gateway: http://localhost:8080 (Spring Cloud Gateway)
 * - Keycloak: http://localhost:8180 (OAuth2/OIDC provider)
 * 
 * OAuth2 Configuration:
 * - Authorization Code Flow with PKCE (RFC 7636)
 * - Silent refresh for seamless token renewal
 * - Realm: car-sharing
 * - Client ID: car-sharing-web
 */
export const environment = {
  production: false,
  apiUrl: 'http://localhost:8080/api',
  
  // OAuth2/OIDC configuration
  oauth: {
    issuer: 'http://localhost:8180/realms/car-sharing',
    clientId: 'car-sharing-web',
    redirectUri: window.location.origin + '/auth/callback',
    postLogoutRedirectUri: window.location.origin + '/cars',
    scope: 'openid profile email',
    responseType: 'code',
    
    // Silent refresh configuration
    silentRefreshRedirectUri: window.location.origin + '/silent-refresh.html',
    silentRefreshTimeout: 5000, // 5 seconds
    timeoutFactor: 0.75, // Refresh at 75% of token lifetime
    
    // Session checks
    sessionChecksEnabled: true,
    clearHashAfterLogin: true,
    
    // PKCE (RFC 7636) - recommended for public clients (SPAs)
    useSilentRefresh: true,
    showDebugInformation: true, // Enable OAuth2 debug logs in dev
    requireHttps: false // Allow HTTP in dev (ONLY for local development)
  },
  
  // Feature flags
  features: {
    realTimeUpdates: true, // SSE/WebSocket for availability updates
    advancedFilters: true,
    feedbackEnabled: true
  }
};
