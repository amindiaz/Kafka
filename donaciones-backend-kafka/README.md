# 🚀 Donaciones Backend — Guía de arranque

## ❌ Error frecuente: `Access denied for user 'root'@'localhost'`

Este error ocurre cuando intentas correr los microservicios **directamente con Java/Maven**
sin pasar las variables de entorno de la base de datos. Los `application.yml` usan
`${DB_PASS:}` (contraseña vacía por defecto), por lo que Spring intenta conectarse como
`root` sin contraseña.

**La solución: usar siempre Docker Compose.**

---

## ✅ Cómo correr el proyecto correctamente

### 1. Requisitos previos
- Docker Desktop instalado y corriendo
- Docker Compose v2+

### 2. Arrancar todo el stack

```bash
# Desde la raíz del proyecto (donde está docker-compose.yml)
docker-compose up --build
```

Esto levanta automáticamente:
| Servicio       | Puerto | Base de datos   |
|----------------|--------|-----------------|
| MySQL          | 3306   | (todas)         |
| ms-usuarios    | 8081   | usuarios_db     |
| ms-donaciones  | 8082   | donaciones_db   |
| ms-necesidades | 8083   | necesidades_db  |
| ms-logistica   | 8084   | logistica_db    |
| ms-rabbit-admin| 8085   | —               |
| RabbitMQ nodo 1 | 5672 / 15672 (consola web) | — |
| RabbitMQ nodo 2 | 5673 / 15673 (consola web) | — |
| api-gateway    | 8080   | —               |

### 3. Verificar que todo esté OK

```bash
# Ver logs en tiempo real
docker-compose logs -f

# Ver estado de los contenedores
docker-compose ps

# Probar el health del gateway
curl http://localhost:8080/actuator/health
```

### 4. Detener el stack

```bash
docker-compose down        # Detiene pero conserva los datos
docker-compose down -v     # Detiene Y borra los datos (reset total)
```

---

## 🐇 Mensajería asíncrona con RabbitMQ (Evaluación Parcial N°3)

```
ms-donaciones ──(donacion.created / donacion.estado.changed)──► donaciones.exchange (topic)
                                                                   │
        ┌──────────────────────────────────────────────────────────┼───────────────────────────┐
        ▼ donacion.created                    ▼ donacion.estado.changed        ▼ donacion.created
 logistica.donacion.creada.queue      logistica.donacion.estado.queue   necesidades.notificacion.donacion.queue
 (ms-logistica: crea el envío)        (ms-logistica: sincroniza envío)  (ms-necesidades: notificación)
        │ rechazados                          │ rechazados                       │ rechazados
        └────────────────► donaciones.dlx (direct) ──► <cola>.dlq  ◄──────────────┘
```

- **Configuración centralizada**: nombres de exchanges, colas, routing keys, DLX y reintentos en
  `application.yml` (`app.messaging.*`), leídos por `MessagingProperties`.
- **Beans de Queue/Exchange/Binding** solo en `messaging/config/RabbitConfig.java` de cada microservicio.
- **Productor**: `ms-donaciones` → `DonacionEventPublisher` (publica después del commit; si RabbitMQ cae solo
  registra el error, la API de donaciones sigue funcionando).
- **Consumidores** (`@RabbitListener`, paquete `messaging/consumer`, agrupados por dominio): logística y notificaciones.
- **ACK manual** (`acknowledge-mode: manual`): éxito → `basicAck`; error no recuperable
  (`IllegalArgumentException`) → `basicReject(requeue=false)` → **DLQ**; error recuperable → hasta 3 reintentos
  (header `x-retry-count`) y luego DLQ. Cada mensaje enviado a la DLQ se registra en el log (`MENSAJE A DLQ → ...`).
- **ms-rabbit-admin** (solo rol ADMIN) administra colas, exchanges y bindings por REST (ver abajo).

### Clúster RabbitMQ (2 nodos)

`rabbitmq` (rabbit@rabbitmq) y `rabbitmq2` (rabbit@rabbitmq2) forman un clúster por `docker/rabbitmq/rabbitmq.conf`
(peer discovery `classic_config`, mismo Erlang cookie). El servicio `rabbitmq-policy` aplica la política
`ha-donaciones` que replica las colas `logistica.*` y `necesidades.*` (incluidas sus DLQ) en ambos nodos.
Los microservicios se conectan con `RABBITMQ_ADDRESSES=rabbitmq:5672,rabbitmq2:5672` (failover).
Verificar: `docker exec donaciones-rabbitmq rabbitmqctl cluster_status` o la consola http://localhost:15672 → Overview → Nodes.

### Autenticación (IDaaS Keycloak)

Los JWT los emite Keycloak (realm `donaciones`, consola http://localhost:9090, admin/admin). El gateway y los
microservicios validan firma (RS256) y expiración con el JWKS del realm (`JWT_JWK_SET_URI`); los roles salen de
`realm_access.roles` (ADMIN, OPERADOR, USER). Usuarios de prueba: `admin/admin123`, `operador/operador123`,
`usuario/usuario123`. Token por consola:
`curl -d "client_id=donaciones-frontend&grant_type=password&username=admin&password=admin123" http://localhost:9090/realms/donaciones/protocol/openid-connect/token`.
`/api/auth/login` de ms-usuarios (HS256 propio) quedó como legado: sus tokens ya no son aceptados.

### API de administración (vía gateway `:8080` o directo `:8085`, header `Authorization: Bearer <JWT de ADMIN>`)

| Método | Ruta | Descripción |
|--------|------|-------------|
| POST   | `/api/rabbit/queues` | Crea cola `{ "name": "pedidos.queue", "durable": true, "deadLetterExchange": "donaciones.dlx", "deadLetterRoutingKey": "pedidos.queue.dlq" }` |
| GET    | `/api/rabbit/queues/{name}` | Mensajes y consumidores de la cola |
| DELETE | `/api/rabbit/queues/{name}` | Elimina la cola |
| POST   | `/api/rabbit/exchanges` | Crea exchange `{ "name": "eventos.exchange", "type": "TOPIC" }` (DIRECT, TOPIC, FANOUT, HEADERS) |
| DELETE | `/api/rabbit/exchanges/{name}` | Elimina el exchange |
| POST   | `/api/rabbit/bindings` | Crea binding `{ "queue": "pedidos.queue", "exchange": "eventos.exchange", "routingKey": "pedido.#" }` |
| DELETE | `/api/rabbit/bindings?queue=&exchange=&routingKey=` | Elimina el binding |

Validaciones: nombre obligatorio, solo `[A-Za-z0-9._-]`, máx. 255, prefijo `amq.` reservado, TTL > 0, tipo de exchange válido.

### Cómo probar el flujo

```bash
# 1) Crear una donación (token OPERADOR/ADMIN) → se publica donacion.created
curl -X POST localhost:8080/api/donaciones -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" \
  -d '{"tipo":"Ropa","cantidad":10,"unidad":"prendas","origen":"Particular","centroAcopio":"<nombre de un centro existente>","usuarioId":1}'
# 2) docker-compose logs -f ms-logistica ms-necesidades   → "Evento recibido..." y "NOTIFICACIÓN →..."
# 3) GET /api/logistica/envios                            → aparece el envío PLANIFICADO
# 4) Con un centroAcopio que NO exista en logística → el mensaje termina en
#    logistica.donacion.creada.queue.dlq (ver http://localhost:15672, usuario/clave de .env)
```

### Tests

```bash
mvn test        # incluye tests del publisher, consumidores/ACK/DLQ, servicio de eventos y RabbitAdminService
```

---

## 🔐 Credenciales de base de datos

Están definidas en el archivo `.env` (raíz del proyecto):

| Variable              | Valor             | Descripción                           |
|-----------------------|-------------------|---------------------------------------|
| `DB_USER`             | `donaciones_user` | Usuario de la aplicación              |
| `DB_PASS`             | `donaciones_pass` | Contraseña de la aplicación           |
| `MYSQL_ROOT_PASSWORD` | `root_secret`     | Root del contenedor MySQL (solo admin)|

> ⚠️ **Nunca conectes los microservicios con `root`.**
> El usuario `donaciones_user` tiene permisos en todas las bases de datos de la app.

---

## 🛠️ Si necesitas correr un microservicio fuera de Docker (desarrollo local)

Asegúrate de tener el contenedor MySQL de Docker corriendo, y pasa las variables manualmente:

```bash
# Opción A: variables de entorno en el comando
DB_HOST=localhost DB_PORT=3306 DB_USER=donaciones_user DB_PASS=donaciones_pass \
  ./mvnw spring-boot:run -pl backend/ms-usuarios

# Opción B: exportar variables primero
export DB_HOST=localhost
export DB_PORT=3306
export DB_USER=donaciones_user
export DB_PASS=donaciones_pass
./mvnw spring-boot:run -pl backend/ms-usuarios
```

> El puerto 3306 del contenedor MySQL está mapeado a tu localhost,
> por eso `DB_HOST=localhost` funciona desde fuera de Docker.

---

## 📁 Estructura del proyecto

```
donaciones-backend-mejorado/
├── .env                        ← Variables de entorno (NO subir a git)
├── docker-compose.yml
├── docker/
│   └── mysql/
│       └── init.sql            ← Crea las 4 bases de datos al iniciar
└── backend/
    ├── api-gateway/            ← Puerto 8080 (entrada única)
    ├── ms-usuarios/            ← Puerto 8081
    ├── ms-donaciones/          ← Puerto 8082
    ├── ms-necesidades/         ← Puerto 8083
    ├── ms-logistica/           ← Puerto 8084
    └── ms-rabbit-admin/        ← Puerto 8085 (administración de RabbitMQ)
```

---

## 🔒 Seguridad — Agregar .env al .gitignore

```bash
echo ".env" >> .gitignore
```
