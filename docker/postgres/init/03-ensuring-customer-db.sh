#!/bin/bash
set -euo pipefail

printf 'Ensuring database %s exists...\n' "customer"
psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname "$POSTGRES_DB" <<'EOSQL'
SELECT format('CREATE DATABASE %I OWNER %I', 'customer', current_user)
WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = 'customer')\gexec
EOSQL