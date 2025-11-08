import { Injectable, computed, inject, signal } from '@angular/core';
import { Router } from '@angular/router';
import { OAuthService, OAuthEvent, OAuthErrorEvent } from 'angular-oauth2-oidc';
import { filter } from 'rxjs/operators';
import { DateTime } from 'luxon';

import { environment } from '../../../environments/environment';

export interface AuthenticatedUser {
  id: string;
  fullName: string | null;
  email: string | null;
  roles: string[];
  accessTokenExpiresAt: DateTime | null;
}

const REDIRECT_STORAGE_KEY = 'car-sharing.redirect-uri';

@Injectable({
  providedIn: 'root',
})
export class Auth {
  private readonly oauthService = inject(OAuthService);
  private readonly router = inject(Router);

  private readonly profileSignal = signal<AuthenticatedUser | null>(null);
  private readonly loadingSignal = signal(true);

  private get sessionStorage(): Storage | null {
    if (typeof window === 'undefined') {
      return null;
    }
    try {
      return window.sessionStorage;
    } catch (error) {
      console.warn('Session storage unavailable', error);
      return null;
    }
  }

  readonly isAuthenticated = computed(() => this.profileSignal() !== null);
  readonly profile = computed(() => this.profileSignal());
  readonly loading = computed(() => this.loadingSignal());

  constructor() {
    this.oauthService.events
      .pipe(filter((event: OAuthEvent) => event.type === 'token_received' || event.type === 'token_refreshed'))
      .subscribe(() => this.updateAuthState());

    this.oauthService.events
      .pipe(filter((event) => event.type === 'logout' || event instanceof OAuthErrorEvent))
      .subscribe(() => this.profileSignal.set(null));
  }

  async initialize(): Promise<void> {
    const isBrowser = typeof window !== 'undefined';
    const origin = isBrowser ? window.location.origin : '';

    this.oauthService.configure({
      issuer: environment.auth.issuer,
      clientId: environment.auth.clientId,
      responseType: 'code',
      scope: environment.auth.scope,
      redirectUri: isBrowser ? `${origin}${environment.auth.redirectUri}` : environment.auth.redirectUri,
      postLogoutRedirectUri: isBrowser
        ? `${origin}${environment.auth.postLogoutRedirectUri}`
        : environment.auth.postLogoutRedirectUri,
      silentRefreshRedirectUri: isBrowser
        ? `${origin}${environment.auth.silentRefreshRedirectUri}`
        : environment.auth.silentRefreshRedirectUri,
      useSilentRefresh: isBrowser,
      showDebugInformation: !environment.production,
      strictDiscoveryDocumentValidation: false,
      requireHttps: environment.auth.requireHttps,
      disableAtHashCheck: true
    });

    if (isBrowser) {
      this.oauthService.setupAutomaticSilentRefresh();
    }

    try {
      if (isBrowser) {
        await this.oauthService.loadDiscoveryDocumentAndTryLogin();
      }
    } finally {
      this.updateAuthState();
      this.loadingSignal.set(false);
    }
  }

  login(redirectUrl?: string): void {
    const target = redirectUrl ?? this.router.url ?? '/';
    this.sessionStorage?.setItem(REDIRECT_STORAGE_KEY, target);
    if (typeof window !== 'undefined') {
      this.oauthService.initCodeFlow(target);
    }
  }

  async logout(): Promise<void> {
    this.sessionStorage?.removeItem(REDIRECT_STORAGE_KEY);
    this.profileSignal.set(null);
    if (typeof window !== 'undefined') {
      await this.oauthService.logOut();
    }
  }

  consumeRedirect(): string {
    const stored = this.sessionStorage?.getItem(REDIRECT_STORAGE_KEY) ?? null;
    this.sessionStorage?.removeItem(REDIRECT_STORAGE_KEY);
    if (stored && stored.startsWith('/')) {
      return stored;
    }
    return '/cars';
  }

  hasRole(role: string): boolean {
    return this.profileSignal()?.roles.includes(role) ?? false;
  }

  hasAnyRole(roles: ReadonlyArray<string>): boolean {
    return roles.some((role) => this.hasRole(role));
  }

  accessToken(): string | null {
    return this.oauthService.hasValidAccessToken() ? this.oauthService.getAccessToken() : null;
  }

  private updateAuthState(): void {
    if (!this.oauthService.hasValidAccessToken()) {
      this.profileSignal.set(null);
      return;
    }

    const accessToken = this.oauthService.getAccessToken();
    const decoded = this.decodeToken(accessToken);
    const identityClaims = (this.oauthService.getIdentityClaims() as Record<string, unknown> | null) ?? {};

    const fullName = this.firstNonEmpty(
      identityClaims['name'],
      identityClaims['preferred_username'],
      identityClaims['given_name']
    );

    const user: AuthenticatedUser = {
      id: String(decoded?.['sub'] ?? ''),
      fullName: fullName,
      email: this.firstNonEmpty(identityClaims['email']) ?? null,
      roles: this.extractRoles(decoded),
      accessTokenExpiresAt: this.oauthService.getAccessTokenExpiration()
        ? DateTime.fromMillis(this.oauthService.getAccessTokenExpiration())
        : null
    };

    this.profileSignal.set(user);
  }

  private decodeToken(token: string | null): Record<string, any> | null {
    if (!token || typeof window === 'undefined') {
      return null;
    }
    const payload = token.split('.')[1];
    if (!payload) {
      return null;
    }

    try {
      const normalized = payload.replace(/-/g, '+').replace(/_/g, '/');
      const decodedPayload = typeof atob === 'function'
        ? atob(normalized)
        : typeof globalThis !== 'undefined' && typeof (globalThis as any).Buffer !== 'undefined'
          ? (globalThis as any).Buffer.from(normalized, 'base64').toString('binary')
          : null;
      return decodedPayload ? JSON.parse(decodedPayload) : null;
    } catch (error) {
      console.error('Failed to decode access token payload', error);
      return null;
    }
  }

  private extractRoles(claims: Record<string, any> | null): string[] {
    if (!claims) {
      return [];
    }

  const realmRoles = Array.isArray(claims?.['realm_access']?.roles) ? claims['realm_access'].roles : [];
  const resourceRoles = this.collectResourceRoles(claims?.['resource_access'] ?? {});

    return Array.from(new Set<string>([...realmRoles, ...resourceRoles]));
  }

  private collectResourceRoles(resourceAccess: Record<string, any>): string[] {
    const roles: string[] = [];
    Object.values(resourceAccess ?? {}).forEach((entry: any) => {
      if (Array.isArray(entry?.roles)) {
        roles.push(...entry.roles);
      }
    });
    return roles;
  }

  private firstNonEmpty(...values: Array<unknown>): string | null {
    for (const value of values) {
      if (typeof value === 'string' && value.trim().length > 0) {
        return value.trim();
      }
    }
    return null;
  }
}
