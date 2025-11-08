import { Component, inject } from '@angular/core';
import { Router, RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { NgFor, NgIf } from '@angular/common';
import { MatToolbarModule } from '@angular/material/toolbar';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressBarModule } from '@angular/material/progress-bar';

import { Auth } from './core/services/auth';

interface NavLink {
  label: string;
  path: string;
  requiresAuth?: boolean;
  roles?: string[];
}

@Component({
  selector: 'app-root',
  imports: [
    RouterOutlet,
    RouterLink,
    RouterLinkActive,
    NgIf,
    NgFor,
    MatToolbarModule,
    MatButtonModule,
    MatIconModule,
    MatProgressBarModule
  ],
  templateUrl: './app.html',
  styleUrl: './app.css'
})
export class App {
  private readonly router = inject(Router);
  protected readonly auth = inject(Auth);

  protected readonly navLinks: NavLink[] = [
    { label: 'Cars', path: '/cars' },
    { label: 'Booking', path: '/booking', requiresAuth: true, roles: ['RENTER'] },
    { label: 'Owner Dashboard', path: '/dashboard/owner', requiresAuth: true, roles: ['OWNER'] },
    { label: 'Renter Dashboard', path: '/dashboard/renter', requiresAuth: true, roles: ['RENTER'] }
  ];

  protected login(): void {
    this.auth.login(this.router.url);
  }

  protected logout(): void {
    this.auth.logout();
  }

  protected canDisplay(link: NavLink): boolean {
    if (!link.requiresAuth) {
      return true;
    }

    if (!this.auth.isAuthenticated()) {
      return false;
    }

    if (!link.roles || link.roles.length === 0) {
      return true;
    }

    return this.auth.hasAnyRole(link.roles);
  }

  protected trackByPath(index: number, item: NavLink): string {
    return item.path;
  }
}
