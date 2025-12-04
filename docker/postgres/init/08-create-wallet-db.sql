SELECT format('CREATE DATABASE %I OWNER %I', 'wallet', current_user)
WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = 'wallet');
\gexec

SELECT format('ALTER DATABASE %I SET timezone = ''UTC''', 'wallet')
WHERE EXISTS (SELECT FROM pg_database WHERE datname = 'wallet');
\gexec

SELECT format('ALTER DATABASE %I SET client_encoding = ''UTF8''', 'wallet')
WHERE EXISTS (SELECT FROM pg_database WHERE datname = 'wallet');
\gexec
