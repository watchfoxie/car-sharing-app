#!/bin/sh
set -e
set -u

create_database() {
    database=$1
    echo "Creating database '$database'"
    psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname "$POSTGRES_DB" <<-EOSQL
        CREATE DATABASE "$database";
        GRANT ALL PRIVILEGES ON DATABASE "$database" TO "$POSTGRES_USER";
EOSQL
}

ensure_keycloak_schema() {
    schema="${KEYCLOAK_SCHEMA:-keycloak}"
    echo "Ensuring schema '$schema' exists in database '$POSTGRES_DB'"
    psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname "$POSTGRES_DB" <<-EOSQL
        CREATE SCHEMA IF NOT EXISTS "$schema" AUTHORIZATION "$POSTGRES_USER";
        GRANT USAGE ON SCHEMA "$schema" TO "$POSTGRES_USER";
        GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA "$schema" TO "$POSTGRES_USER";
        ALTER DEFAULT PRIVILEGES IN SCHEMA "$schema" GRANT ALL ON TABLES TO "$POSTGRES_USER";
EOSQL
}

# Wait for PostgreSQL to be ready
until pg_isready -U "$POSTGRES_USER" -d "$POSTGRES_DB"; do
  echo "Waiting for PostgreSQL to be ready..."
  sleep 2
done

if [ -n "$POSTGRES_MULTIPLE_DATABASES" ]; then
    echo "Multiple database creation requested: $POSTGRES_MULTIPLE_DATABASES"
    for db in $(echo $POSTGRES_MULTIPLE_DATABASES | tr ',' ' '); do
        create_database "$db"
    done
    echo "Multiple databases created successfully"
fi

ensure_keycloak_schema