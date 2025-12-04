SELECT format('CREATE DATABASE %I OWNER %I', 'rating', current_user)
WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = 'rating');
\gexec

SELECT format('ALTER DATABASE %I SET timezone = ''UTC''', 'rating')
WHERE EXISTS (SELECT FROM pg_database WHERE datname = 'rating');
\gexec

SELECT format('ALTER DATABASE %I SET client_encoding = ''UTF8''', 'rating')
WHERE EXISTS (SELECT FROM pg_database WHERE datname = 'rating');
\gexec
