#!/usr/bin/env bash
# Ejecuta las pruebas de la presentación y guarda la salida real en evidencia/*.txt
# Uso (con el stack arriba, desde la raíz del repo):  bash scripts/evidencia.sh
# Variables opcionales: API=http://localhost:8080  KC=http://localhost:9090  RMQ=http://localhost:15672
# En AWS: API=<ApiUrl de la pila> COGNITO_CLIENT_ID=<CognitoClientId> AWS_REGION=<región> USER_PASS=<clave de los usuarios>
#         RMQ=https://rabbit.<dominio>  (y COMPOSE_FILE=docker-compose.yml:docker-compose.aws.yml COMPOSE_ENV_FILES=.env.aws)
set -u
export PATH="$PATH:/c/Program Files/Docker/Docker/resources/bin"
API=${API:-http://localhost:8080}
KC=${KC:-http://localhost:9090}
RMQ=${RMQ:-http://localhost:15672}
RMQ_USER=${RABBITMQ_USER:-donaciones_mq}
RMQ_PASS=${RABBITMQ_PASS:-donaciones_mq_pass}
OUT=evidencia
mkdir -p "$OUT"
J="Content-Type: application/json"

# ── utilidades ────────────────────────────────────────────────────────────────
log() { # log <archivo> <titulo>  (el contenido llega por stdin y se muestra y guarda)
  { echo "=== $2"; echo "    $(date '+%Y-%m-%d %H:%M:%S')"; cat; echo; } | tee -a "$OUT/$1"
}
new() { : > "$OUT/$1"; }
# Tokens: Cognito (AWS) si hay COGNITO_CLIENT_ID; si no, Keycloak (local)
tok() {
  if [ -n "${COGNITO_CLIENT_ID:-}" ]; then
    curl -s -X POST "https://cognito-idp.${AWS_REGION:-us-east-1}.amazonaws.com/" \
      -H 'Content-Type: application/x-amz-json-1.1' -H 'X-Amz-Target: AWSCognitoIdentityProviderService.InitiateAuth' \
      -d "{\"AuthFlow\":\"USER_PASSWORD_AUTH\",\"ClientId\":\"$COGNITO_CLIENT_ID\",\"AuthParameters\":{\"USERNAME\":\"$1\",\"PASSWORD\":\"$2\"}}" \
      | python -c "import sys,json;print(json.load(sys.stdin)['AuthenticationResult']['AccessToken'])"
  else
    curl -s -d "client_id=donaciones-frontend&grant_type=password&username=$1&password=$2" \
      "$KC/realms/donaciones/protocol/openid-connect/token" \
      | python -c "import sys,json;print(json.load(sys.stdin)['access_token'])"
  fi
}
code() { curl -s -o /tmp/ev_body.txt -w "%{http_code}" "$@"; }
rq() { docker exec donaciones-rabbitmq rabbitmqctl "$@" 2>&1 | grep -v "^Timeout\|^Listing\|^Error: unable"; }
jget() { python -c "import sys,json;d=json.load(sys.stdin);print($1)"; }

echo "Obteniendo tokens de Keycloak…"
A=$(tok admin "${USER_PASS:-admin123}"); O=$(tok operador "${USER_PASS:-operador123}"); U=$(tok usuario "${USER_PASS:-usuario123}")
[ -z "$A" ] && { echo "No se pudo obtener token: ¿está levantado Keycloak en $KC?"; exit 1; }

# ── DIAPOSITIVA 2: arquitectura / servicios activos ───────────────────────────
new slide2_arquitectura.txt
docker compose ps --format "table {{.Name}}\t{{.Status}}\t{{.Ports}}" | log slide2_arquitectura.txt "Contenedores del sistema (backend, frontend, brokers, IDaaS)"
python - <<EOF | log slide2_arquitectura.txt "Claims del JWT emitido por Keycloak (usuario admin)"
import base64,json
t="$A".split('.')[1]; t+='='*(-len(t)%4); c=json.loads(base64.urlsafe_b64decode(t))
for k in ('iss','preferred_username','realm_access','exp'): print(f"{k}: {c.get(k)}")
EOF

# ── DIAPOSITIVA 3: topología de mensajería ────────────────────────────────────
new slide3_topologia.txt
rq cluster_status | grep -E "Cluster name|^rabbit@|Running Nodes" | log slide3_topologia.txt "Clúster RabbitMQ (2 nodos)"
rq list_exchanges name type durable | grep -E "name|donaciones" | log slide3_topologia.txt "Exchanges (topic y direct)"
rq list_queues name messages consumers policy | log slide3_topologia.txt "Colas y sus DLQ (consumidores, política de réplica)"
rq list_bindings source_name source_kind destination_name routing_key | grep -E "donaciones" | log slide3_topologia.txt "Bindings: routing key → cola"

# ── DIAPOSITIVA 4: configuración centralizada ─────────────────────────────────
new slide4_codigo.txt
{ for m in ms-logistica ms-necesidades; do echo "--- backend/$m/src/main/resources/application.yml"; sed -n '/^app:/,/^ms:/p' backend/$m/src/main/resources/application.yml | head -20; done
  echo "--- ¿Nombres de colas/exchanges escritos en código de negocio? (debe salir vacío)"
  grep -rnE '"(donaciones\.|logistica\.|necesidades\.)' backend/*/src/main/java --include=*.java | grep -v "/messaging/config/" || echo "(ninguno)"
} | log slide4_codigo.txt "Nombres centralizados en application.yml"
grep -rln "class RabbitConfig" backend/*/src/main/java | log slide4_codigo.txt "Beans Queue/Exchange/Binding solo en RabbitConfig"

# ── DIAPOSITIVA 5: flujo feliz, ACK, reintentos y DLQ ─────────────────────────
new slide5_consumidores.txt
code -X POST -H "Authorization: Bearer $A" -H "$J" -d '{"nombre":"Centro Evidencia","direccion":"Calle 1","region":"RM","capacidad":100}' $API/api/logistica/centros >/dev/null
echo "Donación con centro existente → HTTP $(code -X POST -H "Authorization: Bearer $O" -H "$J" -d '{"tipo":"Alimentos","cantidad":12,"unidad":"cajas","origen":"Particular","centroAcopio":"Centro Evidencia","usuarioId":1}' $API/api/donaciones)  $(cat /tmp/ev_body.txt | head -c 160)" | log slide5_consumidores.txt "1) Éxito: se crea la donación y se publica donacion.created"
sleep 5
{ docker logs ms-donaciones 2>&1 | grep "publicado" | tail -2 | cut -c1-230
  docker logs ms-logistica 2>&1 | grep -E "Evento recibido|creado automáticamente" | tail -2 | cut -c1-230
  docker logs ms-necesidades 2>&1 | grep "NOTIFICACI" | tail -1 | cut -c1-230; } | log slide5_consumidores.txt "   Logs: publicó → logística consumió y creó envío → necesidades notificó (ACK)"
curl -s -H "Authorization: Bearer $O" $API/api/logistica/envios | jget "'\n'.join(f\"envío {e['id']}  donación {e['donacionId']}  {e['estado']}\" for e in d[-3:])" | log slide5_consumidores.txt "   Envío creado por el consumidor (GET /api/logistica/envios)"

echo "Donación con centro INEXISTENTE → HTTP $(code -X POST -H "Authorization: Bearer $O" -H "$J" -d '{"tipo":"Ropa","cantidad":1,"origen":"X","centroAcopio":"CENTRO QUE NO EXISTE"}' $API/api/donaciones)" | log slide5_consumidores.txt "2) Error NO recuperable: rechazo directo a la DLQ (sin reintentos)"
sleep 5
docker logs ms-logistica 2>&1 | grep "MENSAJE A DLQ" | tail -1 | cut -c1-420 | log slide5_consumidores.txt "   Log del consumidor"
rq list_queues name messages | grep "creada.queue" | log slide5_consumidores.txt "   Mensajes en cola principal y en DLQ"

# error recuperable: se apaga MySQL y se publica un evento válido directamente al exchange
docker stop donaciones-mysql >/dev/null 2>&1
DID=$((90000 + RANDOM % 9000))
EVT='{"eventId":"11111111-1111-1111-1111-111111111111","eventType":"DONACION_CREADA","occurredAt":"2026-01-01T00:00:00Z","donacionId":'$DID',"tipo":"Prueba","cantidad":1,"unidad":"u","origen":"o","centroAcopio":"Centro Evidencia","estado":"Recibida","estadoAnterior":null,"usuarioId":1,"fecha":"2026-01-01"}'
BODY=$(python -c "import json;print(json.dumps({'properties':{'content_type':'application/json'},'routing_key':'donacion.created','payload':json.dumps($EVT) if False else '''$EVT''','payload_encoding':'string'}))")
echo "Publicación de un evento válido con MySQL caído → $(curl -s -u $RMQ_USER:$RMQ_PASS -H "$J" -X POST -d "$BODY" $RMQ/api/exchanges/%2F/donaciones.exchange/publish)" | log slide5_consumidores.txt "3) Error RECUPERABLE: MySQL caído → hasta 3 reintentos y luego DLQ"
sleep 40
docker logs ms-logistica 2>&1 | grep -E "Error recuperable|MENSAJE A DLQ.*intentos=3" | tail -5 | cut -c1-260 | log slide5_consumidores.txt "   Logs de reintentos (x-retry-count) y envío final a la DLQ"
docker start donaciones-mysql >/dev/null 2>&1
echo "(MySQL reiniciado; los servicios se recuperan solos en ~30-60 s)" | log slide5_consumidores.txt "   Restauración"

# ── DIAPOSITIVA 6: API de administración de RabbitMQ ──────────────────────────
new slide6_rabbit_admin.txt
sleep 45
A=$(tok admin "${USER_PASS:-admin123}")
{ echo "POST   /api/rabbit/queues                → $(code -X POST -H "Authorization: Bearer $A" -H "$J" -d '{"name":"evidencia.queue"}' $API/api/rabbit/queues)"
  echo "POST   /api/rabbit/exchanges             → $(code -X POST -H "Authorization: Bearer $A" -H "$J" -d '{"name":"evidencia.exchange","type":"DIRECT"}' $API/api/rabbit/exchanges)"
  echo "POST   /api/rabbit/bindings              → $(code -X POST -H "Authorization: Bearer $A" -H "$J" -d '{"queue":"evidencia.queue","exchange":"evidencia.exchange","routingKey":"k"}' $API/api/rabbit/bindings)"
  echo "GET    /api/rabbit/queues/evidencia.queue → $(code -H "Authorization: Bearer $A" $API/api/rabbit/queues/evidencia.queue) $(cat /tmp/ev_body.txt)"
  echo "-- existen en RabbitMQ:"; rq list_queues name | grep evidencia; rq list_bindings source_name destination_name routing_key | grep evidencia
  echo "DELETE /api/rabbit/bindings              → $(code -X DELETE -H "Authorization: Bearer $A" "$API/api/rabbit/bindings?queue=evidencia.queue&exchange=evidencia.exchange&routingKey=k")"
  echo "DELETE /api/rabbit/exchanges/evidencia.exchange → $(code -X DELETE -H "Authorization: Bearer $A" $API/api/rabbit/exchanges/evidencia.exchange)"
  echo "DELETE /api/rabbit/queues/evidencia.queue → $(code -X DELETE -H "Authorization: Bearer $A" $API/api/rabbit/queues/evidencia.queue)"
} | log slide6_rabbit_admin.txt "Crear, consultar y eliminar cola, exchange y binding por API"
{ echo "nombre vacío              → $(code -X POST -H "Authorization: Bearer $A" -H "$J" -d '{"name":""}' $API/api/rabbit/queues)  $(cat /tmp/ev_body.txt | head -c 200)"
  echo "prefijo reservado amq.    → $(code -X POST -H "Authorization: Bearer $A" -H "$J" -d '{"name":"amq.x"}' $API/api/rabbit/queues)  $(cat /tmp/ev_body.txt | head -c 200)"
  echo "tipo de exchange inválido → $(code -X POST -H "Authorization: Bearer $A" -H "$J" -d '{"name":"x","type":"MAL"}' $API/api/rabbit/exchanges)"
  echo "caracteres inválidos      → $(code -X POST -H "Authorization: Bearer $A" -H "$J" -d '{"name":"mala cola!"}' $API/api/rabbit/queues)"
} | log slide6_rabbit_admin.txt "Validación de entrada (deben ser 400)"
{ echo "USER      → $(code -H "Authorization: Bearer $U" $API/api/rabbit/queues/x)"
  echo "OPERADOR  → $(code -H "Authorization: Bearer $O" $API/api/rabbit/queues/x)"
  echo "sin token → $(code $API/api/rabbit/queues/x)"; } | log slide6_rabbit_admin.txt "Solo rol ADMIN (403 / 401 para el resto)"

# ── DIAPOSITIVA 7: API Manager / gateway, JWT e IDaaS ─────────────────────────
new slide7_jwt_idaas.txt
{ echo "sin token                 → $(code $API/api/donaciones)"
  echo "token inventado           → $(code -H 'Authorization: Bearer abc.def.ghi' $API/api/donaciones)"
  echo "token con firma adulterada→ $(code -H "Authorization: Bearer ${A%.*}.AAAA" $API/api/donaciones)"
  echo "token válido (OPERADOR)   → $(code -H "Authorization: Bearer $O" $API/api/donaciones)"
} | log slide7_jwt_idaas.txt "El gateway rechaza JWT inválidos y acepta los correctos"
{ echo "USER GET donaciones       → $(code -H "Authorization: Bearer $U" $API/api/donaciones)  (200)"
  echo "USER POST donaciones      → $(code -X POST -H "Authorization: Bearer $U" -H "$J" -d '{}' $API/api/donaciones)  (403)"
  echo "OPERADOR DELETE donaciones→ $(code -X DELETE -H "Authorization: Bearer $O" $API/api/donaciones/1)  (403)"
} | log slide7_jwt_idaas.txt "Autorización por rol"
echo "GET con Origin del frontend (como lo hace el navegador) → HTTP $(code -H "Authorization: Bearer $O" -H 'Origin: http://localhost:4200' $API/api/donaciones)  (200; un 403 aquí indicaría CORS rechazado por un microservicio)" | log slide7_jwt_idaas.txt "Petición real del navegador (con cabecera Origin) a través del gateway"
echo "Origen http://localhost:4200 → $(curl -s -i -X OPTIONS -H 'Origin: http://localhost:4200' -H 'Access-Control-Request-Method: GET' $API/api/donaciones | grep -i 'HTTP/\|allow-origin' | tr -d '\r' | tr '\n' ' ')" | log slide7_jwt_idaas.txt "CORS del frontend"

# ── DIAPOSITIVA 8: failover del clúster ───────────────────────────────────────
new slide8_failover.txt
docker stop donaciones-rabbitmq >/dev/null 2>&1; sleep 25
rq() { docker exec donaciones-rabbitmq2 rabbitmqctl "$@" 2>&1 | grep -v "^Timeout\|^Listing"; }
rq list_queues name messages consumers | log slide8_failover.txt "Nodo 1 APAGADO: el nodo 2 sigue sirviendo las colas"
echo "Donación con el nodo 1 caído → HTTP $(code -X POST -H "Authorization: Bearer $O" -H "$J" -d '{"tipo":"Agua","cantidad":20,"unidad":"bidones","origen":"P","centroAcopio":"Centro Evidencia"}' $API/api/donaciones)" | log slide8_failover.txt "El sistema sigue funcionando"
sleep 8
curl -s -H "Authorization: Bearer $O" $API/api/logistica/envios | jget "d[-1]['descripcion']" | log slide8_failover.txt "El evento igual llegó a logística (envío creado)"
docker start donaciones-rabbitmq >/dev/null 2>&1; sleep 30
rq() { docker exec donaciones-rabbitmq rabbitmqctl "$@" 2>&1 | grep -v "^Timeout\|^Listing"; }
rq cluster_status | grep -E "Running Nodes|^rabbit@" | log slide8_failover.txt "Nodo 1 de vuelta: clúster con los 2 nodos"

echo; echo "Evidencia guardada en ./$OUT/  →  $(ls $OUT | tr '\n' ' ')"
