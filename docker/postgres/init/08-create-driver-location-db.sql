SELECT format('CREATE DATABASE %I OWNER %I', 'driver_location', current_user)
WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = 'driver_location');
\gexec

SELECT format('ALTER DATABASE %I SET timezone = ''UTC''', 'driver_location')
WHERE EXISTS (SELECT FROM pg_database WHERE datname = 'driver_location');
\gexec

SELECT format('ALTER DATABASE %I SET client_encoding = ''UTF8''', 'driver_location')
WHERE EXISTS (SELECT FROM pg_database WHERE datname = 'driver_location');
\gexec
