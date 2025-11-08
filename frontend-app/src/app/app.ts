import { Component, signal, inject } from '@angular/core';
import { RouterOutlet, RouterLink, RouterLinkActive } from '@angular/router';
import { MatToolbarModule } from '@angular/material/toolbar';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatSidenavModule } from '@angular/material/sidenav';
import { MatListModule } from '@angular/material/list';
import { MatMenuModule } from '@angular/material/menu';
import { CommonModule } from '@angular/common';
import { AuthService } from './core/services/auth.service';

/**
 * Root application component with Material Design layout.
 * 
 * Features:
 * - Responsive sidenav with mobile breakpoint
 * - Top toolbar with navigation links
 * - User profile menu (authenticated users)
 * - Footer with copyright info
 * - Router outlet for lazy-loaded feature modules
 * 
 * Authentication:
 * - Displays login/logout buttons based on auth status
 * - Shows user profile info from JWT token
 * - Role-based navigation (RENTER, OWNER, ADMIN)
 */
@Component({
  selector: 'app-root',
  imports: [
    CommonModule,
    RouterOutlet,
    RouterLink,
    RouterLinkActive,
    MatToolbarModule,
    MatButtonModule,
    MatIconModule,
    MatSidenavModule,
    MatListModule,
    MatMenuModule
  ],
  templateUrl: './app.html',
  styleUrl: './app.css'
})
export class App {
  protected readonly authService = inject(AuthService);
  protected readonly title = signal('Car Sharing');
  protected readonly currentYear = new Date().getFullYear();
  
  // Sidenav state (for mobile)
  protected sidenavOpened = signal(false);
  
  /**
   * Toggle sidenav visibility (mobile only)
   */
  protected toggleSidenav(): void {
    this.sidenavOpened.update(v => !v);
  }
  
  /**
   * Close sidenav on navigation (mobile)
   */
  protected closeSidenav(): void {
    this.sidenavOpened.set(false);
  }
  
  /**
   * Logout user
   */
  protected logout(): void {
    this.authService.logout();
  }
}
