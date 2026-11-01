# syntax=docker/dockerfile:1

# ---------- Stage 1: Build with Maven and Java 21 ----------
FROM maven:3.9.9-eclipse-temurin-21 AS build

WORKDIR /build

COPY pom.xml .
COPY common/pom.xml common/pom.xml
COPY identity-service/pom.xml identity-service/pom.xml
COPY event-service/pom.xml event-service/pom.xml
COPY booking-service/pom.xml booking-service/pom.xml
COPY common/src common/src
COPY identity-service/src identity-service/src
COPY event-service/src event-service/src
COPY booking-service/src booking-service/src

RUN --mount=type=cache,target=/root/.m2 \
    mvn -B -q clean package

# ---------- Stage 2: Runtime ----------
FROM eclipse-temurin:21-jre-jammy

ENV DEBIAN_FRONTEND=noninteractive

RUN apt-get update \
 && apt-get install -y --no-install-recommends \
      postgresql rabbitmq-server supervisor curl \
 && rm -rf /var/lib/apt/lists/* \
 && ln -s "$(ls -d /usr/lib/postgresql/*/bin)" /opt/pgbin

WORKDIR /opt/tazkara

COPY --from=build /build/identity-service/target/identity-service.jar app/identity-service.jar
COPY --from=build /build/event-service/target/event-service.jar app/event-service.jar
COPY --from=build /build/booking-service/target/booking-service.jar app/booking-service.jar
COPY docker/entrypoint.sh docker/run-service.sh bin/
COPY docker/supervisord.conf /etc/supervisor/conf.d/tazkara.conf

RUN sed -i 's/\r$//' bin/*.sh /etc/supervisor/conf.d/tazkara.conf \
 && chmod +x bin/*.sh

VOLUME ["/var/lib/tazkara"]

EXPOSE 8081 8082 8083

HEALTHCHECK --interval=15s --timeout=5s --start-period=120s --retries=5 \
  CMD curl -fs http://localhost:8081/actuator/health \
   && curl -fs http://localhost:8082/actuator/health \
   && curl -fs http://localhost:8083/actuator/health \
    || exit 1

ENTRYPOINT ["/opt/tazkara/bin/entrypoint.sh"]
