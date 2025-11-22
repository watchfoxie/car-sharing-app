SELECT format('CREATE DATABASE %I OWNER %I', 'driver', current_user)
WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = 'driver');
\gexec

SELECT format('ALTER DATABASE %I SET timezone = ''UTC''', 'driver')
WHERE EXISTS (SELECT FROM pg_database WHERE datname = 'driver');
\gexec

SELECT format('ALTER DATABASE %I SET client_encoding = ''UTF8''', 'driver')
WHERE EXISTS (SELECT FROM pg_database WHERE datname = 'driver');
\gexec
