# Laboratorios Apache Kafka en el backend de Donaciones

Este backend implementa las dos guías de Kafka de Desarrollo Cloud Native:

| Guía | Qué cubre | Endpoints | Topic / grupo |
|------|-----------|-----------|---------------|
| Guía autónoma (secciones 1-22) | Topic, productor, consumidor, particiones, claves y offsets | `GET /api/kafka/health`, `POST /api/kafka/send` | `tutorial-particiones` / `mi-grupo-educativo` |
| Lab 3.3.2 | Tuning del productor, pruebas de carga, Consumer Lag y retención | `POST /api/test/start?count=N`, `GET /api/test/config` | `sensor-topic` / `analytics-group` |

Todo vive en el microservicio nuevo **`backend/ms-kafka-lab`** (puerto **8888**, igual que en las guías).
El resto del sistema (usuarios, donaciones, RabbitMQ, Keycloak, gateway) queda igual que la versión RabbitMQ.

## Puertos

| Servicio | Puerto | Nota |
|----------|--------|------|
| Kafka (desde Windows) | 9092 | `kafka:29092` desde otros contenedores |
| **Kafka UI** | **8090** | Las guías dicen 8080, pero aquí 8080 es el api-gateway |
| ms-kafka-lab | 8888 | |

> Donde la guía dice `http://localhost:8080` (Kafka UI), usa **`http://localhost:8090`**.

## ¿Por qué no se usó el zip `donaciones-backend-kafka.zip` tal cual?

El código Kafka del zip (KafkaController, KafkaConsumer y su test) estaba bien y se reutilizó, pero el proyecto como
tal tenía problemas:

1. **Era una versión antigua del backend**: no tenía RabbitMQ, Keycloak, ms-rabbit-admin ni los despliegues;
   usarlo habría deshecho el trabajo de la evaluación RabbitMQ.
2. **`docker-compose.yml` no tenía Kafka ni Kafka UI**, y ms-donaciones no recibía `KAFKA_BOOTSTRAP_SERVERS`:
   dentro de Docker intentaba conectarse a `localhost:9092` (el propio contenedor) y fallaba.
3. **Los Dockerfile no compilaban**: usaban `eclipse-temurin:21-jdk-alpine` (no trae Maven) y no copiaban los
   `pom.xml` de los demás módulos que exige el POM raíz.
4. **Faltaba entero el lab 3.3.2** (LoadTestController, SlowConsumer, `sensor-topic`, variables de tuning).
5. Kafka UI y el api-gateway chocaban en el puerto 8080.

## Opción 1: todo con Docker (recomendada)

Desde esta carpeta (`donaciones-backend-kafka`), en PowerShell. Solo necesitas levantar Kafka, Kafka UI y el lab;
el resto del stack es opcional.

```powershell
docker compose config --quiet
docker compose up -d kafka kafka-ui
docker compose ps
docker compose logs --tail=80 kafka
```

### Crear los topics (antes de arrancar ms-kafka-lab)

```powershell
docker compose exec kafka /opt/kafka/bin/kafka-topics.sh --bootstrap-server localhost:9092 --create --if-not-exists --topic tutorial-particiones --partitions 3 --replication-factor 1
docker compose exec kafka /opt/kafka/bin/kafka-topics.sh --bootstrap-server localhost:9092 --create --if-not-exists --topic sensor-topic --partitions 3 --replication-factor 1 --config retention.ms=300000 --config retention.bytes=52428800 --config segment.bytes=1048576
docker compose exec kafka /opt/kafka/bin/kafka-topics.sh --bootstrap-server localhost:9092 --describe --topic tutorial-particiones
docker compose exec kafka /opt/kafka/bin/kafka-topics.sh --bootstrap-server localhost:9092 --describe --topic sensor-topic
```

Si te saltas este paso, ms-kafka-lab crea los dos topics al arrancar con la misma configuración. Por eso, si lo
levantas antes, el `--create` de la guía responderá que el topic ya existe: es normal, usa `--describe`.

### Levantar el microservicio del laboratorio

```powershell
docker compose up -d --build ms-kafka-lab
docker compose logs -f ms-kafka-lab
```

Busca `Tomcat started on port 8888` y `Started MsKafkaLabApplication`. Para la evidencia de "BUILD SUCCESS" puedes
además compilar localmente (ver Opción 2).

## Opción 2: la app con Maven en Windows (como dicen las guías)

Kafka sigue en Docker; solo la aplicación corre en tu máquina. Detén primero el contenedor del lab para liberar el 8888:

```powershell
docker compose stop ms-kafka-lab
mvn -pl backend/ms-kafka-lab -am clean package -DskipTests
mvn -pl backend/ms-kafka-lab spring-boot:run
```

Requiere JDK 17 o superior; se probó con JDK 21. Si `mvn` no se reconoce, instala Maven o usa el que trae
IntelliJ (`plugins\maven-plugin\lib\maven3\bin\mvn.cmd`), y apunta `JAVA_HOME` a un JDK 21:

```powershell
$env:JAVA_HOME="$env:USERPROFILE\.jdks\ms-21.0.12.1"; $env:Path="$env:JAVA_HOME\bin;$env:Path"
```

---

## Guía autónoma: pruebas A-D

```powershell
curl.exe http://localhost:8888/api/kafka/health
```

**Prueba A, sin clave:**

```powershell
curl.exe -X POST "http://localhost:8888/api/kafka/send?message=Mensaje1"
curl.exe -X POST "http://localhost:8888/api/kafka/send?message=Mensaje2"
curl.exe -X POST "http://localhost:8888/api/kafka/send?message=Mensaje3"
```

**Prueba B, misma clave:**

```powershell
curl.exe -X POST "http://localhost:8888/api/kafka/send?message=Paso1-Creada&key=orden-555"
curl.exe -X POST "http://localhost:8888/api/kafka/send?message=Paso2-Pagada&key=orden-555"
curl.exe -X POST "http://localhost:8888/api/kafka/send?message=Paso3-Enviada&key=orden-555"
```

Los mensajes del consumidor aparecen como `RECIBIDO | particion=... | offset=... | key=... | valor=...` en
`docker compose logs -f ms-kafka-lab` (o en la terminal de Maven).

**Prueba C, offsets y reinicio:**

```powershell
docker compose exec kafka /opt/kafka/bin/kafka-consumer-groups.sh --bootstrap-server localhost:9092 --describe --group mi-grupo-educativo
docker compose restart ms-kafka-lab
```

**Prueba D, grupo nuevo y relectura.** El group-id se lee de una sola fuente (`KAFKA_GROUP_ID`), así que no hay que
tocar código:

```powershell
$env:KAFKA_GROUP_ID="mi-grupo-educativo-2"
docker compose up -d ms-kafka-lab
```

Para volver al grupo original:

```powershell
Remove-Item Env:KAFKA_GROUP_ID
docker compose up -d ms-kafka-lab
```

---

## Lab 3.3.2: tuning y pruebas de carga

### Prueba de control

```powershell
curl.exe -X POST "http://localhost:8888/api/test/start?count=100"
```

Respuesta esperada:

```
Solicitados=100 Confirmados=100 Fallidos=0 Segundos=0.123 msg/s=812.3 | batch.size=16384 linger.ms=0 compression.type=none acks=all
```

`GET /api/test/config` muestra el escenario activo y cuántos mensajes lleva procesados el SlowConsumer.

### Fase 5: escenarios A y B

La compose lee las variables de la terminal. Para cambiar de escenario se recrea el contenedor:

**Escenario A:**

```powershell
$env:KAFKA_BATCH_SIZE="16384"; $env:KAFKA_LINGER_MS="0"; $env:KAFKA_COMPRESSION="none"
docker compose up -d ms-kafka-lab
```

**Escenario B:**

```powershell
$env:KAFKA_BATCH_SIZE="32768"; $env:KAFKA_LINGER_MS="20"; $env:KAFKA_COMPRESSION="snappy"
docker compose up -d ms-kafka-lab
```

Con la Opción 2 (Maven) es igual que en la guía: defines las variables y ejecutas
`mvn -pl backend/ms-kafka-lab spring-boot:run` en la misma terminal.

En cada escenario, repite 3 veces cada carga:

```powershell
curl.exe -X POST "http://localhost:8888/api/test/start?count=1000"
curl.exe -X POST "http://localhost:8888/api/test/start?count=10000"
```

Comprueba con `curl.exe http://localhost:8888/api/test/config` que el escenario activo es el correcto antes de medir.

### Fase 6: Consumer Lag

```powershell
curl.exe -X POST "http://localhost:8888/api/test/start?count=10000"
docker compose exec kafka /opt/kafka/bin/kafka-consumer-groups.sh --bootstrap-server localhost:9092 --describe --group analytics-group
```

El SlowConsumer duerme 10 ms por mensaje (unos 100 msg/s), así que 10.000 mensajes tardan unos 100 s en drenarse.
Captura el lag de inmediato y otra vez a los 30-60 s.

### Fase 7: retención

```powershell
docker compose exec kafka /opt/kafka/bin/kafka-configs.sh --bootstrap-server localhost:9092 --entity-type topics --entity-name sensor-topic --describe
docker compose exec kafka /opt/kafka/bin/kafka-configs.sh --bootstrap-server localhost:9092 --entity-type topics --entity-name sensor-topic --alter --add-config retention.bytes=-1
```

Envía una carga, espera más de 5 minutos, envía otra y compara el offset inicial de cada partición en Kafka UI. Para
restaurar el límite:

```powershell
docker compose exec kafka /opt/kafka/bin/kafka-configs.sh --bootstrap-server localhost:9092 --entity-type topics --entity-name sensor-topic --alter --add-config retention.bytes=52428800
```

El broker revisa la retención cada 60 s (`KAFKA_LOG_RETENTION_CHECK_INTERVAL_MS`). La app no modifica la configuración
de topics que ya existen, así que los cambios hechos con `kafka-configs.sh` se mantienen aunque reinicies ms-kafka-lab.

### Fase 8: particiones y escalamiento

Las claves `sensor-0` a `sensor-4` se reparten en las 3 particiones. Para ver varios consumidores del mismo grupo
sin levantar otra instancia:

```powershell
$env:CONSUMER_CONCURRENCY="3"
docker compose up -d ms-kafka-lab
```

Con 3 hilos, cada uno recibe una partición. Con 4 o más, los sobrantes quedan sin partición.

### Fase 10: limpieza

```powershell
docker compose stop ms-kafka-lab kafka-ui kafka
docker compose rm -f ms-kafka-lab kafka-ui kafka
```

`docker compose down -v` también borra los volúmenes de MySQL y RabbitMQ del resto del proyecto: úsalo solo si
quieres reiniciar todo.

---

## Archivos del laboratorio

```
backend/ms-kafka-lab/
  Dockerfile
  pom.xml
  src/main/resources/application.yml
  src/main/java/cl/donaciones/kafkalab/
    MsKafkaLabApplication.java
    config/KafkaLabConfig.java           topics + listener con commit manual
    controller/KafkaController.java      guía autónoma (/api/kafka)
    controller/LoadTestController.java   lab 3.3.2 (/api/test)
    consumer/KafkaConsumer.java          RECIBIDO | particion | offset | key
    consumer/SlowConsumer.java           10 ms por mensaje, ack por registro
    service/LoadTestService.java         ventanas de 500, espera confirmaciones
    service/LoadTestResult.java
    exception/GlobalExceptionHandler.java
  src/test/java/...                      8 pruebas unitarias
```

## Errores frecuentes específicos de este backend

| Síntoma | Causa | Solución |
|---------|-------|----------|
| Kafka UI no abre en 8080 | Aquí el 8080 es el api-gateway | Usa http://localhost:8090 |
| `Port is already allocated` 8888 | El contenedor ms-kafka-lab y `mvn spring-boot:run` a la vez | `docker compose stop ms-kafka-lab` |
| El escenario no cambia | Variables definidas en otra terminal | Defínelas y ejecuta `docker compose up -d ms-kafka-lab` en la misma terminal; revisa `/api/test/config` |
| `TopicExistsException` | ms-kafka-lab ya creó el topic | Usa `--describe` o `--if-not-exists` |
| 503 "Kafka no confirmo el envio" | Broker caído o no listo | `docker compose ps` y `docker compose logs kafka` |
