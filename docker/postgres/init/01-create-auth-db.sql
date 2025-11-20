SELECT format('CREATE DATABASE %I OWNER %I', 'auth_service', current_user)
WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = 'auth_service');
\gexec
