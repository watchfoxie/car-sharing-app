import { Component, effect, inject } from '@angular/core';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';

import { Auth } from '../../../core/services/auth';

@Component({
  selector: 'app-login',
  imports: [MatCardModule, MatButtonModule, RouterLink],
  templateUrl: './login.html',
  styleUrl: './login.css',
})
export class Login {
  private readonly authService = inject(Auth);
  private readonly router = inject(Router);
  private readonly route = inject(ActivatedRoute);

  constructor() {
    effect(() => {
      if (!this.auth.loading() && this.auth.isAuthenticated()) {
        const redirect = this.route.snapshot.queryParamMap.get('redirect') ?? this.auth.consumeRedirect();
        void this.router.navigateByUrl(redirect ?? '/cars');
      }
    });
  }

  protected beginLogin(): void {
    const redirect =
      this.route.snapshot.queryParamMap.get('redirect') ?? this.router.routerState.snapshot.url ?? '/cars';
    this.auth.login(redirect);
  }

  protected get auth(): Auth {
    return this.authService;
  }

}
