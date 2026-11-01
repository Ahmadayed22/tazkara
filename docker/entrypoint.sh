#!/bin/bash
# Prepares PostgreSQL and RabbitMQ on first start, then hands over to supervisord.
set -euo pipefail

DATA=/var/lib/tazkara
PGDATA=$DATA/postgres
RABBIT_HOME=$DATA/rabbitmq

mkdir -p "$PGDATA" "$RABBIT_HOME" /var/run/postgresql
chown -R postgres:postgres "$PGDATA" /var/run/postgresql
chown -R rabbitmq:rabbitmq "$RABBIT_HOME"
chmod 700 "$PGDATA"

# ---------- PostgreSQL ----------
if [ ! -s "$PGDATA/PG_VERSION" ]; then
  echo "[entrypoint] initialising PostgreSQL in $PGDATA"
  su postgres -c "/opt/pgbin/initdb -D $PGDATA -U postgres --auth-local=trust --auth-host=scram-sha-256 -E UTF8"
fi

su postgres -c "/opt/pgbin/pg_ctl -D $PGDATA -o '-c listen_addresses=localhost' -w start"

# One role and one database per service. Passwords are re-applied on every start so they follow the env vars.
create_db() {
  local name=$1 password=$2
  su postgres -c "psql -v ON_ERROR_STOP=1 -q" <<SQL
DO \$\$ BEGIN
  IF NOT EXISTS (SELECT FROM pg_roles WHERE rolname = '${name}_user') THEN
    CREATE ROLE ${name}_user LOGIN;
  END IF;
END \$\$;
ALTER ROLE ${name}_user PASSWORD '${password}';
SELECT 'CREATE DATABASE ${name}_db OWNER ${name}_user'
WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = '${name}_db')\gexec
SQL
}
create_db identity "$IDENTITY_DB_PASSWORD"
create_db event "$EVENT_DB_PASSWORD"
create_db booking "$BOOKING_DB_PASSWORD"

su postgres -c "/opt/pgbin/pg_ctl -D $PGDATA -m fast -w stop"

# ---------- RabbitMQ ----------
# default_user/default_pass are only used when the broker creates its database on the first start.
cat > /etc/rabbitmq/rabbitmq.conf <<CONF
listeners.tcp.default = 5672
default_user = ${RABBITMQ_USER}
default_pass = ${RABBITMQ_PASSWORD}
loopback_users = none
CONF

exec /usr/bin/supervisord -n -c /etc/supervisor/supervisord.conf
