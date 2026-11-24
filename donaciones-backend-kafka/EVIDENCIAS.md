# Evidencias para la presentación

Regla: **cada afirmación de una diapositiva lleva una captura o una salida real**. Hay dos fuentes:

1. **`bash scripts/evidencia.sh`** ejecuta las pruebas y guarda la salida real en `evidencia/slideN_*.txt`
   (con fecha y hora). Sirve como respaldo y para copiar fragmentos a las diapositivas.
2. **Capturas de pantalla** de las consolas (RabbitMQ, Keycloak, navegador): lo que el script no puede mostrar.

## Cómo correrlo

```bash
docker compose up -d --build          # esperar ~3 min a que todo esté (healthy)
bash scripts/evidencia.sh             # ~4 min; apaga y enciende MySQL y el nodo 1 de RabbitMQ a propósito
```

Para repetir desde cero (colas y DLQ vacías): `docker compose down -v` y volver a levantar.
Para la versión en la nube, ejecutar el mismo script con `API=https://api.<dominio> KC=https://auth.<dominio> RMQ=https://rabbit.<dominio>`
(los `docker exec` deben correr en la VM).

## Qué mostrar por diapositiva

### Diapositiva 3 · "Un exchange topic, tres colas, tres DLQ"
Archivo: `evidencia/slide3_topologia.txt`

| Afirmación | Captura en la consola de RabbitMQ (`http://localhost:15672`, usuario/clave del `.env`) |
|---|---|
| Exchange `donaciones.exchange` topic, durable | pestaña **Exchanges** → filtrar "donaciones": columnas *Type* y *Features* (D) |
| `donaciones.dlx` direct | misma pantalla |
| 3 colas y 3 DLQ | pestaña **Queues**: las 6 colas, con *Consumers* = 1 en las 3 principales |
| Cada routing key llega a su cola | **Exchanges** → clic en `donaciones.exchange` → sección *Bindings* (3 filas con su routing key) |
| Cada cola principal manda sus rechazos a su DLQ | **Queues** → clic en una cola principal → *Features*: `x-dead-letter-exchange` y `x-dead-letter-routing-key` |

Pon en la diapositiva la tabla de bindings real (`rabbitmqctl list_bindings`) junto a la captura.

### Diapositiva 2 · Arquitectura
Archivo: `evidencia/slide2_arquitectura.txt` (11 contenedores activos y claims del JWT).
Captura: `docker compose ps` y la consola de Keycloak → *Users* (3 usuarios) y *Clients* → `donaciones-frontend`.

### Diapositiva 4 · Configuración centralizada
Archivo: `evidencia/slide4_codigo.txt`. Capturas del IDE: `application.yml` (bloque `app.messaging`) y `RabbitConfig.java`.
El archivo incluye la búsqueda que demuestra que **no hay nombres de colas en código de negocio**.

### Diapositiva 5 · Consumidores, ACK, reintentos y DLQ
Archivo: `evidencia/slide5_consumidores.txt` (tres escenarios con sus logs reales):

1. **Éxito → ACK**: el log muestra *publicó → logística consumió → envío creado → necesidades notificó*.
   Captura: frontend → Donaciones (crear) y luego pestaña Envíos con el envío PLANIFICADO.
2. **Error no recuperable → DLQ directa**: centro inexistente, `intentos=0`, mensaje en la DLQ.
   Captura: consola → Queues → `logistica.donacion.creada.queue.dlq` → **Get messages** (muestra el payload) y el log `MENSAJE A DLQ`.
3. **Error recuperable → 3 reintentos → DLQ**: con MySQL caído se ven `intento 1/3`, `2/3`, `3/3` y luego `MENSAJE A DLQ … intentos=3`.
   Captura: `docker logs ms-logistica | grep -E "recuperable|DLQ"`.

### Diapositiva 6 · ms-rabbit-admin
Archivo: `evidencia/slide6_rabbit_admin.txt`: crear/consultar/eliminar cola, exchange y binding (201/200/204),
validaciones (400 con mensaje claro) y solo ADMIN (403/401).
Captura: frontend con rol ADMIN → pestaña **RabbitMQ** creando una cola, y la consola de RabbitMQ mostrando que apareció.

### Diapositiva 7 · API Gateway, JWT e IDaaS
Archivo: `evidencia/slide7_jwt_idaas.txt`: sin token, token inventado y firma adulterada → 401; token válido → 200; roles → 403.
Capturas: Keycloak (realm, usuarios, cliente), el login del frontend (redirección a Keycloak) y la pestaña *Network* del navegador
con el header `Authorization: Bearer …` en una llamada a `localhost:8080`.

### Diapositiva 8 · Despliegue, clúster y failover
Archivo: `evidencia/slide8_failover.txt`.
Capturas: consola de RabbitMQ → **Overview → Nodes** con los 2 nodos; luego con `docker stop donaciones-rabbitmq` el nodo 1 aparece caído,
las colas siguen en el nodo 2 y una donación nueva sigue generando su envío.
Para "en la nube": `docker compose ps` dentro de la VM, la URL `https://app.<dominio>` en el navegador y la consola del proveedor.

## Orden sugerido para grabar la demo (5-10 min)

1. Frontend → login con Keycloak (JWT).  2. Crear donación → envío + notificación (logs).  3. Donación con centro inexistente → DLQ en la consola.
4. Consola de RabbitMQ: nodos, exchanges, colas + DLQ.  5. Apagar un nodo y crear otra donación.  6. (Si hay tiempo) crear una cola desde la pestaña RabbitMQ.

## Qué NO afirmar sin matizar

- "Si RabbitMQ cae, las donaciones se siguen registrando": cierto, pero **si cae el clúster completo, el evento no se publica** (no hay reintento del productor).
  Con un solo nodo caído, no se pierde nada (probado en `slide8_failover.txt`).
- "API Manager": el punto de entrada es el `api-gateway` de Spring; si la rúbrica exige uno de un proveedor cloud, hay que agregarlo.
