SELECT format('CREATE DATABASE %I OWNER %I', 'payment', current_user)
WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = 'payment');
\gexec

SELECT format('ALTER DATABASE %I SET timezone = ''UTC''', 'payment')
WHERE EXISTS (SELECT FROM pg_database WHERE datname = 'payment');
\gexec

SELECT format('ALTER DATABASE %I SET client_encoding = ''UTF8''', 'payment')
WHERE EXISTS (SELECT FROM pg_database WHERE datname = 'payment');
\gexec
