import { Component, inject, OnInit } from '@angular/core';
import { Router, ActivatedRoute } from '@angular/router';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { CommonModule } from '@angular/common';

/**
 * OAuth2 callback page component.
 * 
 * Handles redirect from Keycloak after successful authentication.
 * 
 * Flow:
 * 1. Keycloak redirects to /auth/callback?code=xxx&state=yyy
 * 2. angular-oauth2-oidc exchanges code for tokens
 * 3. Redirect to returnUrl or default page (/cars)
 * 
 * Note: Token exchange is handled automatically by angular-oauth2-oidc (APP_INITIALIZER).
 */
@Component({
  selector: 'app-callback',
  imports: [
    CommonModule,
    MatProgressSpinnerModule
  ],
  template: `
    <div class="callback-container">
      <mat-spinner diameter="60"></mat-spinner>
      <h2>Processing login...</h2>
      <p>Please wait while we complete your authentication.</p>
    </div>
  `,
  styles: [`
    .callback-container {
      display: flex;
      flex-direction: column;
      align-items: center;
      justify-content: center;
      min-height: calc(100vh - 64px - 80px);
      gap: 24px;
      text-align: center;
    }
    
    h2 {
      margin: 0;
      font-size: 24px;
      font-weight: 500;
    }
    
    p {
      margin: 0;
      color: rgba(0, 0, 0, 0.6);
    }
  `]
})
export class CallbackComponent implements OnInit {
  private readonly router = inject(Router);
  private readonly route = inject(ActivatedRoute);
  
  ngOnInit(): void {
    // Wait for token exchange to complete (handled by APP_INITIALIZER)
    // Then redirect to returnUrl or default page
    setTimeout(() => {
      const returnUrl = this.route.snapshot.queryParams['returnUrl'] || '/cars';
      this.router.navigate([returnUrl]);
    }, 1000);
  }
}
