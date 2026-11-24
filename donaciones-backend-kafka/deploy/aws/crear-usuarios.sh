#!/usr/bin/env bash
# Crea en Amazon Cognito los 3 usuarios de prueba del sistema (admin, operador, usuario).
# Se ejecuta en AWS CloudShell (ya trae el AWS CLI con sus credenciales):
#
#     bash crear-usuarios.sh <UserPoolId> '<ClaveParaLosUsuarios>'
#
# La clave debe tener 8+ caracteres con mayúscula, minúscula y número (ej. Donaciones2025).
set -euo pipefail

POOL="${1:?Uso: bash crear-usuarios.sh <UserPoolId> '<Clave>'}"
PASS="${2:?Falta la clave de los usuarios}"

crear() { # usuario grupo
  aws cognito-idp admin-create-user --user-pool-id "$POOL" --username "$1" \
    --user-attributes Name=email,Value="$1@donaciones.cl" Name=email_verified,Value=true \
    --message-action SUPPRESS >/dev/null
  aws cognito-idp admin-set-user-password --user-pool-id "$POOL" --username "$1" --password "$PASS" --permanent
  aws cognito-idp admin-add-user-to-group --user-pool-id "$POOL" --username "$1" --group-name "$2"
  echo "✔ $1 → grupo $2"
}

crear admin ADMIN
crear operador OPERADOR
crear usuario USER
echo "Listo. Usuarios del pool:"
aws cognito-idp list-users --user-pool-id "$POOL" --query 'Users[].Username' --output text
