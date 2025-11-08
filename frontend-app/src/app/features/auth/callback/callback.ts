import { Component, effect, inject, signal } from '@angular/core';
import { Router } from '@angular/router';
import { MatCardModule } from '@angular/material/card';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';

import { Auth } from '../../../core/services/auth';

@Component({
  selector: 'app-callback',
  imports: [MatCardModule, MatProgressSpinnerModule],
  templateUrl: './callback.html',
  styleUrl: './callback.css',
})
export class Callback {
  private readonly auth = inject(Auth);
  private readonly router = inject(Router);
  protected readonly status = signal('Completing sign-in...');

  constructor() {
    effect(() => {
      if (this.auth.loading()) {
        return;
      }

      if (this.auth.isAuthenticated()) {
        const target = this.auth.consumeRedirect();
        this.status.set('Redirecting to your workspace...');
        void this.router.navigateByUrl(target);
      } else {
        this.status.set('Sign-in could not be completed. Please try again.');
      }
    });
  }

}
