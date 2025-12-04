SELECT format('CREATE DATABASE %I OWNER %I', 'auth_service', current_user)
WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = 'auth_service');
\gexec

SELECT format('ALTER DATABASE %I SET timezone = ''UTC''', 'auth_service')
WHERE EXISTS (SELECT FROM pg_database WHERE datname = 'auth_service');
\gexec

SELECT format('ALTER DATABASE %I SET client_encoding = ''UTF8''', 'auth_service')
WHERE EXISTS (SELECT FROM pg_database WHERE datname = 'auth_service');
\gexec
