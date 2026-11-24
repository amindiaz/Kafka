# Sistema de Donaciones · Kafka + RabbitMQ

DSY1107 · Desarrollo Cloud Native I · Sección 002D

Integrantes: Cristobal Diaz · Maximiliano Rubilar

## Contenido

| Carpeta / archivo | Qué es |
|---|---|
| `donaciones-backend-kafka/` | Backend: microservicios Spring Boot, API Gateway, Keycloak, clúster RabbitMQ, Apache Kafka con Kafka UI y el microservicio `ms-kafka-lab` |
| `donaciones-frontend/` | Frontend Angular con login OIDC (Keycloak) |
| `Informe_Kafka_Donaciones.docx` | Informe técnico de la integración de Kafka y de los laboratorios |
| `Presentacion_Kafka_Usos_Reales.pptx` | Presentación: usos reales de Kafka con capturas del laboratorio |

## Cómo levantarlo

Requisitos: Docker Desktop, JDK 21 y Maven 3.9 (solo para compilar fuera de Docker).

```powershell
cd donaciones-backend-kafka
docker compose up -d --build
```

| Servicio | URL |
|---|---|
| Frontend | http://localhost:4200 |
| API Gateway | http://localhost:8080 |
| Keycloak | http://localhost:9090 |
| Consola RabbitMQ | http://localhost:15672 |
| Kafka UI | http://localhost:8090 |
| ms-kafka-lab | http://localhost:8888 |

Para los laboratorios de Kafka basta con levantar `kafka`, `kafka-ui` y `ms-kafka-lab`. La guía paso a paso está en [`donaciones-backend-kafka/KAFKA-LAB.md`](donaciones-backend-kafka/KAFKA-LAB.md).
