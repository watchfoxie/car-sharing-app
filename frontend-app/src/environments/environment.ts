/**
 * Production environment configuration.
 * 
 * This configuration is used in production builds (ng build).
 * 
 * IMPORTANT: Update these values before deploying to production!
 * 
 * Backend services:
 * - API Gateway: https://api.carsharing.com (replace with actual URL)
 * - Keycloak: https://auth.carsharing.com (replace with actual URL)
 */
export const environment = {
  production: true,
  apiUrl: 'https://api.carsharing.com/api', // TODO: Replace with production API Gateway URL
  
  // OAuth2/OIDC configuration
  oauth: {
    issuer: 'https://auth.carsharing.com/realms/car-sharing', // TODO: Replace with production Keycloak URL
    clientId: 'car-sharing-web',
    redirectUri: window.location.origin + '/auth/callback',
    postLogoutRedirectUri: window.location.origin + '/cars',
    scope: 'openid profile email',
    responseType: 'code',
    
    // Silent refresh configuration
    silentRefreshRedirectUri: window.location.origin + '/silent-refresh.html',
    silentRefreshTimeout: 5000,
    timeoutFactor: 0.75,
    
    // Session checks
    sessionChecksEnabled: true,
    clearHashAfterLogin: true,
    
    // PKCE (RFC 7636)
    useSilentRefresh: true,
    showDebugInformation: false, // Disable OAuth2 debug logs in production
    requireHttps: true // Enforce HTTPS in production
  },
  
  // Feature flags
  features: {
    realTimeUpdates: true,
    advancedFilters: true,
    feedbackEnabled: true
  }
};
