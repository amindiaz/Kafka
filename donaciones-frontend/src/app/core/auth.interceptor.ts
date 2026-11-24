import { HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { environment } from '../../environments/environment';
import { AuthService } from './auth.service';

/** Agrega el JWT (Authorization: Bearer) a toda llamada hacia el API Gateway. */
export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const auth = inject(AuthService);
  if (req.url.startsWith(environment.apiUrl) && auth.accessToken) {
    req = req.clone({ setHeaders: { Authorization: `Bearer ${auth.accessToken}` } });
  }
  return next(req);
};
