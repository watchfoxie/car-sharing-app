import { CanActivateFn, Router } from '@angular/router';
import { inject } from '@angular/core';

import { Auth } from '../services/auth';

export const roleGuard: CanActivateFn = (route) => {
  const auth = inject(Auth);
  const router = inject(Router);
  const roles = route.data?.['roles'] as string[] | undefined;

  if (!roles || roles.length === 0) {
    return true;
  }

  if (!auth.isAuthenticated()) {
    return router.parseUrl('/login');
  }

  return auth.hasAnyRole(roles) ? true : router.parseUrl('/cars');
};
