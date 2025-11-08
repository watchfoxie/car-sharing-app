import { Component, inject, OnInit } from '@angular/core';
import { Router, ActivatedRoute } from '@angular/router';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { CommonModule } from '@angular/common';
import { AuthService } from '../../../../core/services/auth.service';

/**
 * Login page component.
 * 
 * Initiates OAuth2 Authorization Code Flow with PKCE.
 * 
 * Flow:
 * 1. User clicks "Login with Keycloak"
 * 2. Redirect to Keycloak login page
 * 3. User authenticates
 * 4. Keycloak redirects back to /auth/callback with authorization code
 * 5. angular-oauth2-oidc exchanges code for tokens
 * 6. User is authenticated
 */
@Component({
  selector: 'app-login',
  imports: [
    CommonModule,
    MatCardModule,
    MatButtonModule,
    MatIconModule,
    MatProgressSpinnerModule
  ],
  template: `
    <div class="login-container">
      <mat-card class="login-card mat-elevation-z4">
        <mat-card-header>
          <mat-card-title>
            <mat-icon>lock</mat-icon>
            Sign In to Car Sharing
          </mat-card-title>
        </mat-card-header>
        
        <mat-card-content>
          <p>Sign in to access your bookings, manage your cars, and more.</p>
          
          @if (isLoading) {
            <div class="loading-container">
              <mat-spinner diameter="40"></mat-spinner>
              <p>Redirecting to login...</p>
            </div>
          }
        </mat-card-content>
        
        <mat-card-actions>
          <button 
            mat-raised-button 
            color="primary" 
            (click)="login()"
            [disabled]="isLoading"
            class="login-button">
            <mat-icon>login</mat-icon>
            Login with Keycloak
          </button>
        </mat-card-actions>
      </mat-card>
    </div>
  `,
  styles: [`
    .login-container {
      display: flex;
      justify-content: center;
      align-items: center;
      min-height: calc(100vh - 64px - 80px);
      padding: 24px;
    }
    
    .login-card {
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
    
    .loading-container {
      display: flex;
      flex-direction: column;
      align-items: center;
      gap: 16px;
      padding: 24px;
    }
    
    .login-button {
      width: 100%;
      height: 48px;
      font-size: 16px;
    }
    
    mat-card-actions {
      padding: 16px;
    }
  `]
})
export class LoginComponent implements OnInit {
  private readonly authService = inject(AuthService);
  private readonly router = inject(Router);
  private readonly route = inject(ActivatedRoute);
  
  protected isLoading = false;
  
  ngOnInit(): void {
    // Redirect to returnUrl if already authenticated
    if (this.authService.isAuthenticated()) {
      const returnUrl = this.route.snapshot.queryParams['returnUrl'] || '/cars';
      this.router.navigate([returnUrl]);
    }
  }
  
  /**
   * Initiate OAuth2 login flow.
   */
  login(): void {
    this.isLoading = true;
    this.authService.login();
    // User will be redirected to Keycloak, so we don't need to set isLoading back to false
  }
}
