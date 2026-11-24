# Despliegue en la nube

Todo el sistema (2 nodos RabbitMQ, MySQL, Keycloak, 5 microservicios, API Gateway y frontend) corre con
`docker compose` en **una VM Linux** de cualquier proveedor (AWS EC2, Azure VM, GCP, DigitalOcean, etc.).
`docker-compose.cloud.yml` se aplica sobre `docker-compose.yml` y agrega HTTPS (Caddy) y cierra los puertos internos.

> El login OIDC/PKCE del frontend **solo funciona en HTTPS** (o localhost): el navegador no expone `crypto.subtle`
> en HTTP plano. Por eso el despliegue en la nube usa Caddy con certificados Let's Encrypt automáticos.

## 1. Requisitos

- VM Ubuntu 22.04+ con **4 GB de RAM o más** (son ~12 contenedores, varios con JVM) y 20 GB de disco.
- Puertos **80 y 443** abiertos en el firewall / security group (y 22 para SSH). Nada más.
- Un dominio con 4 registros A hacia la IP pública de la VM:
  `app.<dominio>`, `api.<dominio>`, `auth.<dominio>`, `rabbit.<dominio>`.
  Sin dominio propio sirve **nip.io** (ver abajo).

## 2. Preparar la VM

```bash
curl -fsSL https://get.docker.com | sh
sudo usermod -aG docker $USER && newgrp docker
mkdir ~/donaciones && cd ~/donaciones
git clone <URL-repo-backend>  donaciones-backend
git clone <URL-repo-frontend> donaciones-frontend
cd donaciones-backend
```

Los dos repositorios deben quedar **uno al lado del otro** (el compose construye el frontend desde `../donaciones-frontend`).

## 3. Configurar secretos

```bash
cp .env.cloud.example .env.cloud
nano .env.cloud
```

- `DOMAIN`: tu dominio base. Con nip.io: `DOMAIN=34-123-45-67.nip.io` (IP con guiones). Los nombres
  `app.34-123-45-67.nip.io`, `api.…`, `auth.…`, `rabbit.…` resuelven solos a esa IP.
- `PUBLIC_API_URL`, `PUBLIC_OIDC_ISSUER`, `PUBLIC_FRONTEND_URL`: las mismas URLs con `https://app.`, `https://api.`, `https://auth.`.
- Contraseñas (`MYSQL_ROOT_PASSWORD`, `DB_PASS`, `RABBITMQ_PASS`, `RABBITMQ_ERLANG_COOKIE`, `KEYCLOAK_ADMIN_PASSWORD`):
  generar cada una con `openssl rand -base64 24`.
- `.env.cloud` está en `.gitignore`: **no lo subas a GitHub**.

## 4. Levantar

```bash
docker compose -f docker-compose.yml -f docker-compose.cloud.yml --env-file .env.cloud up -d --build
docker compose -f docker-compose.yml -f docker-compose.cloud.yml --env-file .env.cloud ps
```

El primer arranque tarda varios minutos (compila los 6 módulos Java y el frontend). Si algún contenedor
aparece como `unhealthy` en el primer intento por el orden de arranque, repite el `up -d`.

## 5. Verificar

| Qué | Dónde |
|---|---|
| Frontend | `https://app.<dominio>` → redirige al login de Keycloak |
| Keycloak (admin) | `https://auth.<dominio>` (usuario/clave de `.env.cloud`) |
| Consola RabbitMQ | `https://rabbit.<dominio>` (`RABBITMQ_USER` / `RABBITMQ_PASS`) → Overview → Nodes: 2 nodos |
| API | `https://api.<dominio>/api/donaciones` → 401 sin token |

Usuarios de prueba del realm (cámbialos o elimínalos desde la consola de Keycloak antes de una demo pública):
`admin/admin123`, `operador/operador123`, `usuario/usuario123`.

Comprobaciones por consola:

```bash
docker exec donaciones-rabbitmq rabbitmqctl cluster_status      # 2 nodos en Running Nodes
docker exec donaciones-rabbitmq rabbitmqctl list_queues name policy consumers   # 3 colas + 3 DLQ, política ha-donaciones
docker exec donaciones-rabbitmq rabbitmqctl list_exchanges name type | grep donaciones   # topic y direct
docker logs ms-logistica 2>&1 | grep "MENSAJE A DLQ"
```

## 6. Demo de la presentación (EP4)

1. Mostrar la VM / consola del proveedor y `docker compose ps` (backend, frontend y brokers activos).
2. `https://app.<dominio>` → login con Keycloak (OAuth 2.0 / OIDC) → se obtiene el JWT.
3. Crear una donación con un centro existente → aparece el envío PLANIFICADO.
4. Crear otra con un centro inexistente → el mensaje queda en `logistica.donacion.creada.queue.dlq` y en el log.
5. `https://rabbit.<dominio>` → 2 nodos, exchanges `donaciones.exchange` (topic) y `donaciones.dlx` (direct), 3 colas + 3 DLQ.
6. Apagar un nodo (`docker stop donaciones-rabbitmq`) y crear otra donación: el sistema sigue funcionando.

## Notas

- **API Manager**: el punto de entrada es el `api-gateway` (Spring Cloud Gateway). Valida el JWT del IDaaS (JWKS de Keycloak)
  y rechaza los inválidos con 401. Si la evaluación exige un API Manager de un proveedor cloud (p. ej. Azure API Management),
  se coloca delante de `https://api.<dominio>` apuntando a este gateway.
- **Keycloak** corre en modo `start-dev` (base H2 dentro del contenedor): válido para el curso, no para producción real.
  El realm se reimporta desde `docker/keycloak/donaciones-realm.json` si se recrea el contenedor.
- **Eventos si RabbitMQ cae por completo**: la donación se guarda pero el evento no se publica (diseño deliberado: la
  mensajería nunca rompe el flujo de negocio). Con un solo nodo caído el clúster sigue sirviendo.
- **Actualizar**: `git pull` en ambos repos y repetir el `up -d --build`.
