/**
 * Authentication Service
 * 
 * Handles OAuth2/OIDC authentication flow using Keycloak with PKCE.
 * Provides methods for login, logout, token refresh, and user profile access.
 * 
 * @module AuthService
 */

import { Injectable, inject } from '@angular/core';
import { Router } from '@angular/router';
import { OAuthService, OAuthErrorEvent } from 'angular-oauth2-oidc';
import { BehaviorSubject, filter } from 'rxjs';
import { authConfig } from '../config/auth.config';

/**
 * User Profile Interface
 * Maps to Keycloak JWT claims
 */
export interface UserProfile {
  sub: string; // User ID
  email?: string;
  email_verified?: boolean;
  name?: string;
  preferred_username?: string;
  given_name?: string;
  family_name?: string;
  realm_access?: {
    roles: string[];
  };
}

/**
 * User Roles Enum
 * Matches backend RBAC roles
 */
export enum UserRole {
  ADMIN = 'ADMIN',
  OWNER = 'OWNER',
  RENTER = 'RENTER',
  MANAGER = 'MANAGER'
}

@Injectable({
  providedIn: 'root'
})
export class Auth {
  private readonly oauthService = inject(OAuthService);
  private readonly router = inject(Router);
  
  private readonly isAuthenticatedSubject = new BehaviorSubject<boolean>(false);
  private readonly userProfileSubject = new BehaviorSubject<UserProfile | null>(null);
  
  /**
   * Observable stream of authentication state
   */
  public readonly isAuthenticated$ = this.isAuthenticatedSubject.asObservable();
  
  /**
   * Observable stream of user profile
   */
  public readonly userProfile$ = this.userProfileSubject.asObservable();
  
  constructor() {
    this.configureOAuth();
    this.setupEventHandlers();
  }
  
  /**
   * Configure OAuth Service with Keycloak settings
   */
  private configureOAuth(): void {
    this.oauthService.configure(authConfig);
    this.oauthService.setupAutomaticSilentRefresh();
  }
  
  /**
   * Setup event handlers for OAuth events
   */
  private setupEventHandlers(): void {
    // Listen for successful token events
    this.oauthService.events
      .pipe(filter(e => e.type === 'token_received'))
      .subscribe(() => {
        this.handleAuthenticationSuccess();
      });
    
    // Listen for logout events
    this.oauthService.events
      .pipe(filter(e => e.type === 'logout'))
      .subscribe(() => {
        this.handleLogout();
      });
    
    // Listen for token refresh events
    this.oauthService.events
      .pipe(filter(e => e.type === 'token_refreshed'))
      .subscribe(() => {
        console.log('Access token refreshed successfully');
      });
    
    // Listen for errors
    this.oauthService.events
      .pipe(filter(e => e instanceof OAuthErrorEvent))
      .subscribe((event: OAuthErrorEvent) => {
        console.error('OAuth Error:', event);
        if (event.type === 'token_refresh_error') {
          // Token refresh failed - force re-login
          this.login();
        }
      });
  }
  
  /**
   * Initialize authentication
   * Should be called in app initialization
   * 
   * @returns Promise<boolean> - true if user is authenticated
   */
  public async initializeAuth(): Promise<boolean> {
    try {
      // Load discovery document and try to login silently
      await this.oauthService.loadDiscoveryDocumentAndTryLogin();
      
      if (this.oauthService.hasValidAccessToken()) {
        this.handleAuthenticationSuccess();
        return true;
      }
      
      return false;
    } catch (error) {
      console.error('Failed to initialize authentication:', error);
      return false;
    }
  }
  
  /**
   * Initiate login flow
   * Redirects to Keycloak login page
   * 
   * @param targetUrl - URL to redirect after successful login
   */
  public login(targetUrl?: string): void {
    if (targetUrl) {
      sessionStorage.setItem('auth_redirect_url', targetUrl);
    }
    this.oauthService.initCodeFlow();
  }
  
  /**
   * Handle successful authentication
   */
  private handleAuthenticationSuccess(): void {
    const claims = this.oauthService.getIdentityClaims() as UserProfile;
    
    if (claims) {
      this.userProfileSubject.next(claims);
      this.isAuthenticatedSubject.next(true);
      
      // Redirect to stored URL or home
      const redirectUrl = sessionStorage.getItem('auth_redirect_url') || '/';
      sessionStorage.removeItem('auth_redirect_url');
      this.router.navigateByUrl(redirectUrl);
    }
  }
  
  /**
   * Logout user
   * Clears tokens and redirects to Keycloak logout
   */
  public logout(): void {
    this.oauthService.logOut();
  }
  
  /**
   * Handle logout event
   */
  private handleLogout(): void {
    this.isAuthenticatedSubject.next(false);
    this.userProfileSubject.next(null);
    this.router.navigate(['/']);
  }
  
  /**
   * Check if user is authenticated
   */
  public isAuthenticated(): boolean {
    return this.oauthService.hasValidAccessToken();
  }
  
  /**
   * Get current access token
   * 
   * @returns string | null
   */
  public getAccessToken(): string | null {
    return this.oauthService.getAccessToken();
  }
  
  /**
   * Get current user profile
   * 
   * @returns UserProfile | null
   */
  public getUserProfile(): UserProfile | null {
    return this.userProfileSubject.value;
  }
  
  /**
   * Get user ID (sub claim)
   * 
   * @returns string | null
   */
  public getUserId(): string | null {
    return this.getUserProfile()?.sub || null;
  }
  
  /**
   * Get user roles from realm_access claim
   * 
   * @returns string[]
   */
  public getUserRoles(): string[] {
    const profile = this.getUserProfile();
    return profile?.realm_access?.roles || [];
  }
  
  /**
   * Check if user has specific role
   * 
   * @param role - Role to check (e.g., 'ADMIN', 'OWNER', 'RENTER')
   * @returns boolean
   */
  public hasRole(role: string): boolean {
    return this.getUserRoles().includes(role);
  }
  
  /**
   * Check if user has any of the specified roles
   * 
   * @param roles - Array of roles to check
   * @returns boolean
   */
  public hasAnyRole(roles: string[]): boolean {
    const userRoles = this.getUserRoles();
    return roles.some(role => userRoles.includes(role));
  }
  
  /**
   * Refresh access token
   * Automatically handled by silent refresh, but can be called manually
   */
  public async refreshToken(): Promise<void> {
    try {
      await this.oauthService.refreshToken();
    } catch (error) {
      console.error('Failed to refresh token:', error);
      this.login();
    }
  }
}

