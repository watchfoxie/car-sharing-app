import { CanActivateFn } from '@angular/router';
import { inject } from '@angular/core';

import { Auth } from '../services/auth';

export const authGuard: CanActivateFn = (route, state) => {
  const auth = inject(Auth);

  if (auth.isAuthenticated()) {
    return true;
  }

  auth.login(state.url);
  return false;
};
