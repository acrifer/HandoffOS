FROM maven:3.9.11-eclipse-temurin-17 AS builder

ARG SERVICE_MODULE

WORKDIR /build

COPY . .

RUN --mount=type=cache,target=/root/.m2 \
    mvn -B -ntp -pl ${SERVICE_MODULE} -am package -DskipTests \
    && cp $(find ${SERVICE_MODULE}/target -maxdepth 1 -name '*.jar' ! -name '*original*.jar' | head -n 1) /tmp/app.jar

FROM eclipse-temurin:17-jre

WORKDIR /app

COPY docker/start-service.sh /app/start-service.sh
COPY --from=builder /tmp/app.jar /app/app.jar

RUN chmod +x /app/start-service.sh

ENTRYPOINT ["/app/start-service.sh"]
