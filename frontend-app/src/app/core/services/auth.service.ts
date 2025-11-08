import { Injectable, inject, signal, computed } from '@angular/core';
import { Router } from '@angular/router';
import { OAuthService, AuthConfig, NullValidationHandler } from 'angular-oauth2-oidc';
import { filter } from 'rxjs/operators';
import { environment } from '../../../environments/environment.development';

/**
 * Authentication service using OAuth2/OIDC with Keycloak.
 * 
 * Features:
 * - Authorization Code Flow with PKCE (RFC 7636)
 * - Silent refresh for seamless token renewal
 * - JWT token management
 * - Role extraction from token claims
 * - Automatic login/logout flows
 * 
 * Token claims structure (from Keycloak):
 * {
 *   sub: "user-id",
 *   name: "John Doe",
 *   email: "john@example.com",
 *   realm_access: {
 *     roles: ["RENTER", "OWNER", "ADMIN"]
 *   }
 * }
 */
@Injectable({
  providedIn: 'root'
})
export class AuthService {
  private readonly oauthService = inject(OAuthService);
  private readonly router = inject(Router);
  
  // Reactive authentication state
  private readonly _isAuthenticated = signal(false);
  private readonly _userProfile = signal<any>(null);
  private readonly _roles = signal<string[]>([]);
  
  // Public readonly signals
  readonly isAuthenticated = this._isAuthenticated.asReadonly();
  readonly userProfile = this._userProfile.asReadonly();
  readonly roles = this._roles.asReadonly();
  
  // Computed signals for role checks
  readonly isRenter = computed(() => this.hasRole('RENTER'));
  readonly isOwner = computed(() => this.hasRole('OWNER'));
  readonly isAdmin = computed(() => this.hasRole('ADMIN'));
  
  constructor() {
    this.configureOAuth();
    this.setupAutomaticSilentRefresh();
  }
  
  /**
   * Configure OAuth2/OIDC with Keycloak.
   */
  private configureOAuth(): void {
    const authConfig: AuthConfig = {
      issuer: environment.oauth.issuer,
      clientId: environment.oauth.clientId,
      redirectUri: environment.oauth.redirectUri,
      postLogoutRedirectUri: environment.oauth.postLogoutRedirectUri,
      responseType: environment.oauth.responseType,
      scope: environment.oauth.scope,
      
      // Silent refresh
      silentRefreshRedirectUri: environment.oauth.silentRefreshRedirectUri,
      silentRefreshTimeout: environment.oauth.silentRefreshTimeout,
      timeoutFactor: environment.oauth.timeoutFactor,
      useSilentRefresh: environment.oauth.useSilentRefresh,
      
      // Session management
      sessionChecksEnabled: environment.oauth.sessionChecksEnabled,
      clearHashAfterLogin: environment.oauth.clearHashAfterLogin,
      
      // Development options
      showDebugInformation: environment.oauth.showDebugInformation,
      requireHttps: environment.oauth.requireHttps
    };
    
    this.oauthService.configure(authConfig);
    this.oauthService.tokenValidationHandler = new NullValidationHandler();
    this.oauthService.setupAutomaticSilentRefresh();
  }
  
  /**
   * Setup automatic silent refresh and event handlers.
   */
  private setupAutomaticSilentRefresh(): void {
    // Listen to successful token refresh
    this.oauthService.events
      .pipe(filter((e: any) => e.type === 'token_received'))
      .subscribe(() => {
        this.updateAuthenticationState();
      });
    
    // Listen to token expiration
    this.oauthService.events
      .pipe(filter((e: any) => e.type === 'token_expires'))
      .subscribe(() => {
        console.warn('Access token is about to expire');
      });
    
    // Listen to silent refresh errors
    this.oauthService.events
      .pipe(filter((e: any) => e.type === 'silent_refresh_error'))
      .subscribe(() => {
        console.error('Silent refresh failed, logging out');
        this.logout();
      });
  }
  
  /**
   * Initialize OAuth2 flow (discovery + check for existing session).
   * 
   * This should be called in app initialization (APP_INITIALIZER).
   */
  async initializeAuth(): Promise<void> {
    try {
      // Load discovery document (OIDC configuration)
      await this.oauthService.loadDiscoveryDocument();
      
      // Try to login with existing session (silent)
      await this.oauthService.tryLoginImplicitFlow();
      
      if (this.oauthService.hasValidAccessToken()) {
        this.updateAuthenticationState();
      }
    } catch (error) {
      console.error('OAuth initialization failed', error);
    }
  }
  
  /**
   * Start login flow (redirect to Keycloak).
   */
  login(): void {
    this.oauthService.initCodeFlow();
  }
  
  /**
   * Logout (revoke tokens + redirect to Keycloak logout).
   */
  logout(): void {
    this.oauthService.revokeTokenAndLogout();
    this._isAuthenticated.set(false);
    this._userProfile.set(null);
    this._roles.set([]);
  }
  
  /**
   * Get access token for API calls.
   */
  getAccessToken(): string {
    return this.oauthService.getAccessToken();
  }
  
  /**
   * Get ID token claims (user profile).
   */
  getIdTokenClaims(): any {
    return this.oauthService.getIdentityClaims();
  }
  
  /**
   * Check if user has specific role.
   */
  private hasRole(role: string): boolean {
    return this._roles().includes(role);
  }
  
  /**
   * Update authentication state from token.
   */
  private updateAuthenticationState(): void {
    const hasToken = this.oauthService.hasValidAccessToken();
    this._isAuthenticated.set(hasToken);
    
    if (hasToken) {
      const claims = this.getIdTokenClaims();
      this._userProfile.set(claims);
      
      // Extract roles from realm_access.roles claim
      const realmRoles = claims?.['realm_access']?.['roles'] || [];
      this._roles.set(realmRoles);
    }
  }
}
