#!/bin/sh
# Genera config.json a partir de variables de entorno (se ejecuta antes de iniciar nginx)
cat > /usr/share/nginx/html/config.json <<JSON
{
  "apiUrl": "${API_URL:-http://localhost:8080}",
  "issuer": "${OIDC_ISSUER:-http://localhost:9090/realms/donaciones}",
  "clientId": "${OIDC_CLIENT_ID:-donaciones-frontend}",
  "cognitoDomain": "${COGNITO_DOMAIN:-}"
}
JSON
