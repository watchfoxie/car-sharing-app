#!/bin/bash
set -euo pipefail

printf 'Ensuring database %s exists...\n' "payment"
psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname "$POSTGRES_DB" <<'EOSQL'
SELECT format('CREATE DATABASE %I OWNER %I', 'payment', current_user)
WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = 'payment')\gexec
EOSQL
