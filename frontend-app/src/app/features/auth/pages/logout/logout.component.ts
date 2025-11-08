import { Component, inject, OnInit } from '@angular/core';
import { Router } from '@angular/router';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { CommonModule } from '@angular/common';
import { AuthService } from '../../../../core/services/auth.service';

/**
 * Logout page component.
 * 
 * Handles user logout and token revocation.
 * 
 * Flow:
 * 1. Revoke access/refresh tokens
 * 2. Redirect to Keycloak logout
 * 3. Clear local session
 * 4. Redirect to home page
 */
@Component({
  selector: 'app-logout',
  imports: [
    CommonModule,
    MatCardModule,
    MatButtonModule,
    MatIconModule
  ],
  template: `
    <div class="logout-container">
      <mat-card class="logout-card mat-elevation-z4">
        <mat-card-header>
          <mat-card-title>
            <mat-icon>logout</mat-icon>
            Logged Out
          </mat-card-title>
        </mat-card-header>
        
        <mat-card-content>
          <p>You have been successfully logged out.</p>
          <p>Thank you for using Car Sharing!</p>
        </mat-card-content>
        
        <mat-card-actions>
          <button 
            mat-raised-button 
            color="primary" 
            (click)="goHome()"
            class="home-button">
            <mat-icon>home</mat-icon>
            Go to Home
          </button>
          <button 
            mat-button 
            (click)="login()"
            class="login-button">
            <mat-icon>login</mat-icon>
            Login Again
          </button>
        </mat-card-actions>
      </mat-card>
    </div>
  `,
  styles: [`
    .logout-container {
      display: flex;
      justify-content: center;
      align-items: center;
      min-height: calc(100vh - 64px - 80px);
      padding: 24px;
    }
    
    .logout-card {
      max-width: 400px;
      width: 100%;
    }
    
    mat-card-header {
      margin-bottom: 16px;
    }
    
    mat-card-title {
      display: flex;
      align-items: center;
      gap: 8px;
      font-size: 24px;
    }
    
    mat-card-content p {
      margin: 16px 0;
      color: rgba(0, 0, 0, 0.6);
    }
    
    mat-card-actions {
      padding: 16px;
      display: flex;
      gap: 8px;
    }
    
    .home-button {
      flex: 1;
    }
  `]
})
export class LogoutComponent implements OnInit {
  private readonly authService = inject(AuthService);
  private readonly router = inject(Router);
  
  ngOnInit(): void {
    // Logout on component init
    this.authService.logout();
  }
  
  /**
   * Navigate to home page.
   */
  goHome(): void {
    this.router.navigate(['/cars']);
  }
  
  /**
   * Navigate to login page.
   */
  login(): void {
    this.router.navigate(['/auth/login']);
  }
}
