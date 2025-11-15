/**
 * OAuth2/OIDC Configuration for Keycloak Integration
 * 
 * This configuration enables PKCE flow with silent refresh for secure
 * authentication against the Car Sharing Keycloak realm.
 * 
 * @module AuthConfig
 */

import { AuthConfig } from 'angular-oauth2-oidc';

/**
 * Keycloak OIDC Configuration
 * 
 * Features:
 * - PKCE (Proof Key for Code Exchange) for enhanced security
 * - Silent refresh for seamless token renewal
 * - Automatic token validation
 * - Role mapping from realm_access.roles claim
 */
export const authConfig: AuthConfig = {
  /**
   * Keycloak issuer URL
   * Dev: http://localhost:8180/realms/car-sharing
   * Staging: https://auth-staging.carsharing.com/realms/car-sharing
   * Prod: https://auth.carsharing.com/realms/car-sharing
   */
  issuer: 'http://localhost:8180/realms/car-sharing',

  /**
   * OAuth2 endpoints (auto-discovered from issuer/.well-known/openid-configuration)
   */
  redirectUri: window.location.origin + '/auth/callback',
  postLogoutRedirectUri: window.location.origin + '/',
  clientId: 'car-sharing-angular-client',
  
  /**
   * Response type for Authorization Code Flow with PKCE
   */
  responseType: 'code',
  
  /**
   * Requested OAuth2 scopes
   * - openid: Required for OIDC
   * - profile: User profile information
   * - email: User email address
   * - roles: Custom scope for role claims
   */
  scope: 'openid profile email roles',
  
  /**
   * PKCE Configuration
   * S256 = SHA-256 hashing (more secure than 'plain')
   */
  useSilentRefresh: true,
  silentRefreshRedirectUri: window.location.origin + '/assets/silent-refresh.html',
  
  /**
   * Session checks - detect logout in other tabs
   */
  sessionChecksEnabled: true,
  
  /**
   * Token validation
   */
  showDebugInformation: false, // Set to true in development for debugging
  requireHttps: false, // Set to true in production
  
  /**
   * Silent refresh timeout (ms) - refresh token 5 minutes before expiration
   */
  silentRefreshTimeout: 5000,
  
  /**
   * Time factor for token refresh (0.75 = refresh at 75% of token lifetime)
   */
  timeoutFactor: 0.75,
  
  /**
   * Disable PKCE for older browsers (not recommended)
   */
  disablePKCE: false,
  
  /**
   * Clear hash after login (clean URL)
   */
  clearHashAfterLogin: true,
  
  /**
   * Non-standard claims to skip validation
   */
  skipIssuerCheck: false,
  strictDiscoveryDocumentValidation: false, // Keycloak compatibility
};

/**
 * Environment-specific configuration factory
 * 
 * @param environment - 'dev' | 'staging' | 'prod'
 * @returns AuthConfig with environment-specific values
 */
export function getAuthConfig(environment: 'dev' | 'staging' | 'prod'): AuthConfig {
  const baseConfig = { ...authConfig };
  
  switch (environment) {
    case 'dev':
      baseConfig.issuer = 'http://localhost:8180/realms/car-sharing';
      baseConfig.requireHttps = false;
      baseConfig.showDebugInformation = true;
      break;
    
    case 'staging':
      baseConfig.issuer = 'https://auth-staging.carsharing.com/realms/car-sharing';
      baseConfig.requireHttps = true;
      baseConfig.showDebugInformation = false;
      break;
    
    case 'prod':
      baseConfig.issuer = 'https://auth.carsharing.com/realms/car-sharing';
      baseConfig.requireHttps = true;
      baseConfig.showDebugInformation = false;
      break;
  }
  
  return baseConfig;
}
