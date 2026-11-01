#!/bin/bash
# Usage: run-service.sh <identity|event|booking>
# Waits for PostgreSQL (and RabbitMQ when the service uses it), then starts the service.
set -euo pipefail

SERVICE=$1
UPPER=$(echo "$SERVICE" | tr '[:lower:]' '[:upper:]')

export "${UPPER}_DB_URL=jdbc:postgresql://localhost:5432/${SERVICE}_db"
export "${UPPER}_DB_USER=${SERVICE}_user"

until /opt/pgbin/pg_isready -q -h localhost -p 5432; do
  echo "[$SERVICE] waiting for PostgreSQL..."
  sleep 2
done

if [ "$SERVICE" != "identity" ]; then
  until (echo > /dev/tcp/localhost/5672) 2>/dev/null; do
    echo "[$SERVICE] waiting for RabbitMQ..."
    sleep 2
  done
fi

exec java ${JAVA_OPTS:--XX:MaxRAMPercentage=20} -jar "/opt/tazkara/app/${SERVICE}-service.jar"
