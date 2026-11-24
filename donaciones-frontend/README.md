# Donaciones – Frontend (Angular 20)

Frontend standalone/zoneless del sistema de donaciones.

- **Login**: OAuth 2.0 / OpenID Connect (Authorization Code + PKCE) contra Keycloak, con `angular-oauth2-oidc` (`src/app/core/auth.service.ts`).
- **JWT**: `authInterceptor` agrega `Authorization: Bearer <token>` a toda llamada al API Gateway.
- **Rutas protegidas**: `authGuard`; `/rabbit` exige rol `ADMIN`.
- **Vistas**: donaciones (crear / cambiar estado → publica eventos en RabbitMQ), necesidades, envíos (generados por el consumidor de logística) y administración de RabbitMQ (colas, exchanges, bindings).

## Configuración

`src/environments/environment.ts`: `apiUrl` (gateway, `http://localhost:8080`), `issuer` (`http://localhost:9090/realms/donaciones`) y `clientId` (`donaciones-frontend`).

## Ejecutar

Levantar primero el backend (`docker-compose up --build` en el repo del backend), luego:

```bash
npm install
npm start        # http://localhost:4200
```

Usuarios de prueba del realm: `admin/admin123` (ADMIN), `operador/operador123` (OPERADOR), `usuario/usuario123` (USER).

## IDaaS: Keycloak (local) o Amazon Cognito (AWS)

El mismo build sirve para ambos; solo cambia la configuración en tiempo de ejecución (`config.json`, generado en el contenedor
desde `API_URL`, `OIDC_ISSUER`, `OIDC_CLIENT_ID` y, solo con Cognito, `COGNITO_DOMAIN` para el cierre de sesión).
Los roles se leen de `realm_access.roles` (Keycloak) o `cognito:groups` (Cognito). Ver `DEPLOY-AWS.md` en el repo del backend.
