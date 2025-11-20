SELECT format('CREATE DATABASE %I OWNER %I', 'user_service', current_user)
WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = 'user_service');
\gexec
