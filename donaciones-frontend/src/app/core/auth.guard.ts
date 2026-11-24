import { CanActivateFn } from '@angular/router';
import { inject } from '@angular/core';
import { AuthService } from './auth.service';

/** Exige sesión; si no hay, redirige al login del IDaaS. Con `data.roles` exige además alguno de esos roles. */
export const authGuard: CanActivateFn = route => {
  const auth = inject(AuthService);
  if (!auth.isLoggedIn) {
    auth.login();
    return false;
  }
  const roles: string[] | undefined = route.data?.['roles'];
  return !roles || auth.hasRole(...roles);
};
