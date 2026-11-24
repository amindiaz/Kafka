FROM maven:3.9-eclipse-temurin-21 AS builder
WORKDIR /workspace
COPY pom.xml .
# El POM raíz declara todos los módulos: Maven necesita sus pom.xml aunque solo se compile uno
COPY backend/api-gateway/pom.xml backend/api-gateway/pom.xml
COPY backend/ms-donaciones/pom.xml backend/ms-donaciones/pom.xml
COPY backend/ms-usuarios/pom.xml backend/ms-usuarios/pom.xml
COPY backend/ms-necesidades/pom.xml backend/ms-necesidades/pom.xml
COPY backend/ms-logistica/pom.xml backend/ms-logistica/pom.xml
COPY backend/ms-rabbit-admin/pom.xml backend/ms-rabbit-admin/pom.xml
COPY backend/ms-kafka-lab/pom.xml backend/ms-kafka-lab/pom.xml
COPY backend/ms-rabbit-admin/src     backend/ms-rabbit-admin/src
RUN mvn -pl backend/ms-rabbit-admin -am package -DskipTests -q
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=builder /workspace/backend/ms-rabbit-admin/target/*.jar app.jar
EXPOSE 8085
ENTRYPOINT ["java","-jar","app.jar"]
