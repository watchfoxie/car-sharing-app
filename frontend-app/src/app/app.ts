/**
 * App Root Component
 * 
 * Root component providing:
 * - Application header with navigation
 * - Authentication controls (login/logout)
 * - Router outlet for routed components
 * - User profile display
 */

import { Component, inject, OnInit, signal, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterLink, RouterOutlet } from '@angular/router';
import { MatToolbarModule } from '@angular/material/toolbar';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatMenuModule } from '@angular/material/menu';
import { MatSidenavModule } from '@angular/material/sidenav';
import { MatListModule } from '@angular/material/list';
import { MatDividerModule } from '@angular/material/divider';

import { Auth, UserRole, UserProfile } from './core/services/auth';

@Component({
  selector: 'app-root',
  imports: [
    RouterOutlet,
    RouterLink,
    CommonModule,
    MatToolbarModule,
    MatButtonModule,
    MatIconModule,
    MatMenuModule,
    MatSidenavModule,
    MatListModule,
    MatDividerModule
  ],
  templateUrl: './app.html',
  styleUrl: './app.scss'
})
export class App implements OnInit {
  private readonly authService = inject(Auth);
  private readonly router = inject(Router);

  // Authentication state as signals (converted from Observables)
  protected readonly isAuthenticated = signal<boolean>(false);
  protected readonly userProfile = signal<UserProfile | null>(null);

  // Role-based navigation visibility
  protected readonly isOwner = computed(() => 
    this.authService.hasRole(UserRole.OWNER) || this.authService.hasRole(UserRole.ADMIN)
  );
  
  protected readonly isRenter = computed(() =>
    this.authService.hasRole(UserRole.RENTER) || this.authService.hasRole(UserRole.ADMIN)
  );

  // Mobile navigation
  protected mobileMenuOpen = false;

  constructor() {
    // Subscribe to authentication state changes
    this.authService.isAuthenticated$.subscribe(isAuth => {
      this.isAuthenticated.set(isAuth);
    });

    this.authService.userProfile$.subscribe(profile => {
      this.userProfile.set(profile);
    });
  }

  ngOnInit(): void {
    // Initialize OAuth2/OIDC
    this.authService.initializeAuth();
  }

  protected login(): void {
    const currentUrl = this.router.url;
    this.authService.login(currentUrl);
  }

  protected logout(): void {
    this.authService.logout();
    this.router.navigate(['/cars']);
  }

  protected toggleMobileMenu(): void {
    this.mobileMenuOpen = !this.mobileMenuOpen;
  }

  protected closeMobileMenu(): void {
    this.mobileMenuOpen = false;
  }
}
