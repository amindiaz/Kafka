# Despliegue en AWS (EP4)

Todo el sistema se crea con **una plantilla de CloudFormation** ([deploy/aws/template.yaml](deploy/aws/template.yaml)).
Cada requisito de la pauta queda cubierto por un servicio real de AWS:

| Pauta EP4 | Servicio / evidencia en AWS |
|---|---|
| API Manager en la nube, en funcionamiento | **Amazon API Gateway** (HTTP API `donaciones-api`) |
| Configuración que llama a los endpoints del backend | Ruta `ANY /api/{proxy+}` → integración HTTP hacia el backend en EC2 |
| El frontend consume los endpoints a través del API Manager | `API_URL` del frontend = URL de API Gateway (pestaña *Network* del navegador) |
| El API Manager valida el JWT: rechaza inválidos, acepta correctos | **JWT authorizer** de API Gateway con el issuer de Cognito (401 sin token) |
| Tenant en el IDaaS y usuarios registrados | **Amazon Cognito**: user pool `donaciones-users` con 3 usuarios y 3 grupos |
| El frontend usa OAuth 2.0 / OIDC y obtiene un JWT válido | Login con Cognito Hosted UI (Authorization Code + PKCE) |
| Backend y frontend desplegados, activos e integrados en la nube | **EC2** con `docker compose` + `https://app.<dominio>` |
| Dos nodos RabbitMQ en clúster, 3 colas con DLQ, exchanges direct y topic | Consola `https://rabbit.<dominio>` |
| `docker-compose` levantando RabbitMQ en la nube | `docker compose ps` dentro de la EC2 |

```
Navegador ──HTTPS──► app.<dominio>  (Caddy → frontend Angular)
    │
    └──JWT──► Amazon API Gateway ──(valida JWT con Cognito)──► origin.<dominio> (Caddy → api-gateway Spring)
                                                                    └► ms-usuarios · ms-donaciones · ms-necesidades · ms-logistica · ms-rabbit-admin
                                                                    └► RabbitMQ (2 nodos en clúster) · MySQL
```

> **Estado:** la plantilla pasa `cfn-lint` y el mismo `docker compose` se probó en local con HTTPS (Caddy), pero **la creación
> de la pila en AWS no se ha podido probar desde aquí** (no hay credenciales de AWS). Si algo falla, revisen *Solución de problemas*.

## 1. Subir los dos repositorios a GitHub (públicos)

La instancia clona el código desde GitHub, por eso deben ser **públicos** y **no contener secretos** (`.env*` ya está en `.gitignore`).

```bash
# backend (carpeta donaciones-fixed)
git init -b main && git add . && git commit -m "Backend donaciones"
git remote add origin https://github.com/<usuario>/donaciones-backend.git && git push -u origin main

# frontend (carpeta donaciones-frontend)
git init -b main && git add . && git commit -m "Frontend Angular"
git remote add origin https://github.com/<usuario>/donaciones-frontend.git && git push -u origin main
```

## 2. Crear la pila

1. Consola de AWS → región (ej. **us-east-1**) → *EC2 → Key Pairs* → crear un par de claves y guardar el `.pem`.
2. *CloudFormation → Create stack → With new resources → Upload a template file* → subir `deploy/aws/template.yaml`.
3. Parámetros: `KeyName`, `SshCidr` (su IP pública `/32`), `BackendRepoUrl`, `FrontendRepoUrl`. El resto puede quedar por defecto.
4. Marcar *Create stack* y esperar `CREATE_COMPLETE` (~5 min). **El despliegue dentro de la instancia sigue ~10-15 min más**
   (instala Docker, compila los 6 módulos Java y el frontend).
5. Seguimiento: `ssh -i clave.pem ubuntu@<IP>` y `tail -f /var/log/donaciones-deploy.log`; termina con `DESPLIEGUE TERMINADO`.

En la pestaña **Outputs** de la pila quedan `AppUrl`, `ApiUrl`, `RabbitConsoleUrl`, `UserPoolId`, `CognitoClientId` y `SshCommand`.

## 3. Crear los usuarios de prueba en Cognito

En **AWS CloudShell** (icono `>_` de la barra superior), subir `deploy/aws/crear-usuarios.sh` y ejecutar:

```bash
bash crear-usuarios.sh <UserPoolId> 'Donaciones2025'
```

Crea `admin` (grupo ADMIN), `operador` (OPERADOR) y `usuario` (USER) con esa clave.

## 4. Verificar

1. Abrir `AppUrl` → redirige al login de Cognito → entrar como `operador` → se ve la app con "operador (OPERADOR)".
2. Crear una donación (con un centro de acopio creado antes desde Postman/curl con el token de `admin`) → aparece el envío.
3. `RabbitConsoleUrl`: usuario `donaciones_mq`, clave en `~/donaciones/donaciones-backend/.env.aws` (`RABBITMQ_PASS`).
4. Rechazo de tokens inválidos en el API Manager:
   ```bash
   curl -i <ApiUrl>/api/donaciones                      # 401 Unauthorized (lo responde API Gateway, no el backend)
   curl -i -H "Authorization: Bearer abc.def.ghi" <ApiUrl>/api/donaciones   # 401
   ```

## 5. Sacar la evidencia (todo desde la nube)

Ejecutar en la instancia, en `~/donaciones/donaciones-backend`:

```bash
export COMPOSE_FILE=docker-compose.yml:docker-compose.aws.yml COMPOSE_ENV_FILES=.env.aws
export API=<ApiUrl> COGNITO_CLIENT_ID=<CognitoClientId> AWS_REGION=<región> USER_PASS='Donaciones2025'
export RMQ=https://rabbit.<dominio> RABBITMQ_USER=donaciones_mq RABBITMQ_PASS=<RABBITMQ_PASS de .env.aws>
bash scripts/evidencia.sh
```

Genera `evidencia/slide*.txt` con los mismos resultados de la presentación, pero **contra API Gateway y Cognito reales**.
Capturas de pantalla que además deben mostrar (con la consola de AWS abierta):

| Qué | Dónde |
|---|---|
| Instancia en ejecución | EC2 → Instances (`donaciones-backend`, *running*) |
| `docker compose` levantando todo | `docker compose ps` por SSH (11 contenedores, 2 nodos RabbitMQ) |
| API Manager creado y activo | API Gateway → `donaciones-api` → Stages (`$default`) |
| Su configuración | API Gateway → Routes (`ANY /api/{proxy+}`) → Integration (URL del backend) y Authorization (JWT authorizer `cognito-jwt`) |
| JWT validado | `curl` con y sin token (401 / 200) |
| Tenant IDaaS y usuarios | Cognito → User pools → `donaciones-users` → Users y Groups |
| Login OAuth2/OIDC | La pantalla de Cognito Hosted UI y la app ya con sesión; *Network* mostrando `Authorization: Bearer …` hacia `execute-api` |
| Backend y frontend integrados | `AppUrl` creando una donación y el envío en la pestaña Envíos |
| RabbitMQ | `RabbitConsoleUrl`: Overview (2 nodos), Exchanges (topic y direct), Queues (3 colas + 3 DLQ) |

## 6. Costos y limpieza

- `t3.large` ≈ US$0.08 por hora (≈ US$2 por día). API Gateway y Cognito cuestan centavos con este uso.
- Al terminar la evaluación: **CloudFormation → Delete stack** (elimina instancia, IP elástica, API Gateway y Cognito).
- Para pausar sin borrar: detener la instancia (EC2 → Stop). Al iniciarla de nuevo conserva la IP elástica y los contenedores se reinician solos.

## Solución de problemas

| Síntoma | Revisar |
|---|---|
| `https://app.<dominio>` no abre tras 20 min | `tail -n 50 /var/log/donaciones-deploy.log`; `docker compose ps`. Si un contenedor quedó `unhealthy` por el orden de arranque, repetir `docker compose up -d` |
| Let's Encrypt rechaza el certificado (límite de nip.io) | Crear la pila de nuevo con `DnsSuffix = sslip.io` |
| El login vuelve con `redirect_mismatch` | El *Callback URL* del cliente de Cognito debe ser exactamente `https://app.<dominio>/` (lo define la plantilla) |
| API Gateway devuelve 401 con un token válido | Usar el **access token** (no el id token); su `client_id` debe coincidir con el *audience* del authorizer |
| 403 al crear/borrar | El usuario no está en el grupo correcto (OPERADOR o ADMIN): revisar Cognito → Groups |
| La cuenta es de laboratorio (AWS Academy) | Confirmar que el laboratorio permite CloudFormation, Cognito y API Gateway; la plantilla **no crea roles IAM** |
