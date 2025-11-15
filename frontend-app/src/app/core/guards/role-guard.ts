import { CanActivateChildFn } from '@angular/router';

export const roleGuard: CanActivateChildFn = (childRoute, state) => {
  return true;
};
