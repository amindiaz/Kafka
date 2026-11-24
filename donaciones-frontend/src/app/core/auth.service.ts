import { Injectable, inject } from '@angular/core';
import { OAuthService, AuthConfig } from 'angular-oauth2-oidc';
import { environment } from '../../environments/environment';

/** Login con OAuth 2.0 / OpenID Connect contra el IDaaS (Keycloak). */
@Injectable({ providedIn: 'root' })
export class AuthService {
  private oauth = inject(OAuthService);

  /** Se ejecuta al arrancar la app: descubre el IDaaS y procesa el retorno del login. */
  async init(): Promise<void> {
    try {
      Object.assign(environment, await (await fetch('/config.json', { cache: 'no-store' })).json());
    } catch {
      // sin config.json se usan los valores por defecto de environment.ts
    }
    const authConfig: AuthConfig = {
      issuer: environment.issuer,
      clientId: environment.clientId,
      redirectUri: window.location.origin + '/',
      postLogoutRedirectUri: window.location.origin + '/',
      responseType: 'code', // Authorization Code + PKCE
      scope: 'openid profile email',
      requireHttps: false, // solo desarrollo local (http)
      showDebugInformation: false,
      // Cognito publica los endpoints en otro host que el issuer
      strictDiscoveryDocumentValidation: false,
      // Cognito no tiene end_session_endpoint: el logout usa su propia URL
      ...(environment.cognitoDomain ? {
        logoutUrl: `https://${environment.cognitoDomain}/logout?client_id={{client_id}}&logout_uri=` +
          encodeURIComponent(window.location.origin + '/'),
      } : {}),
    };
    this.oauth.configure(authConfig);
    await this.oauth.loadDiscoveryDocumentAndTryLogin();
    this.oauth.setupAutomaticSilentRefresh();
  }

  get isLoggedIn(): boolean {
    return this.oauth.hasValidAccessToken();
  }

  get accessToken(): string {
    return this.oauth.getAccessToken();
  }

  get username(): string {
    const claims = this.oauth.getIdentityClaims() as Record<string, string> | null;
    return claims?.['preferred_username'] ?? claims?.['cognito:username'] ?? claims?.['email'] ?? '';
  }

  /** Roles de realm (ADMIN, OPERADOR, USER) del JWT. */
  get roles(): string[] {
    const token = this.accessToken;
    if (!token) return [];
    try {
      const payload = JSON.parse(atob(token.split('.')[1].replace(/-/g, '+').replace(/_/g, '/')));
      // Keycloak: realm_access.roles · Cognito: cognito:groups
      const roles: string[] = payload['cognito:groups'] ?? payload.realm_access?.roles ?? [];
      return roles.map(r => r.toUpperCase());
    } catch {
      return [];
    }
  }

  hasRole(...wanted: string[]): boolean {
    return this.roles.some(r => wanted.includes(r));
  }

  login(): void {
    this.oauth.initCodeFlow();
  }

  logout(): void {
    this.oauth.logOut();
  }
}
