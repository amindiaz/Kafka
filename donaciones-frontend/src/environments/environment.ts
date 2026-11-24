/**
 * Valores por defecto (desarrollo local). Se sobrescriben al arrancar con /config.json,
 * que el contenedor genera desde las variables API_URL, OIDC_ISSUER, OIDC_CLIENT_ID y COGNITO_DOMAIN.
 */
export const environment = {
  /** API Gateway (único punto de entrada al backend). */
  apiUrl: 'http://localhost:8080',
  /** IDaaS: Keycloak, realm "donaciones". */
  issuer: 'http://localhost:9090/realms/donaciones',
  clientId: 'donaciones-frontend',
  /** Solo con Amazon Cognito: dominio del Hosted UI (sin https://) para el cierre de sesión. */
  cognitoDomain: '',
};
