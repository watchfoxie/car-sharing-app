SELECT format('CREATE DATABASE %I OWNER %I', 'customer', current_user)
WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = 'customer');
\gexec

SELECT format('ALTER DATABASE %I SET timezone = ''UTC''', 'customer')
WHERE EXISTS (SELECT FROM pg_database WHERE datname = 'customer');
\gexec

SELECT format('ALTER DATABASE %I SET client_encoding = ''UTF8''', 'customer')
WHERE EXISTS (SELECT FROM pg_database WHERE datname = 'customer');
\gexec
