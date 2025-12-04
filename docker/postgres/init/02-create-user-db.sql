SELECT format('CREATE DATABASE %I OWNER %I', 'user_service', current_user)
WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = 'user_service');
\gexec

SELECT format('ALTER DATABASE %I SET timezone = ''UTC''', 'user_service')
WHERE EXISTS (SELECT FROM pg_database WHERE datname = 'user_service');
\gexec

SELECT format('ALTER DATABASE %I SET client_encoding = ''UTF8''', 'user_service')
WHERE EXISTS (SELECT FROM pg_database WHERE datname = 'user_service');
\gexec
