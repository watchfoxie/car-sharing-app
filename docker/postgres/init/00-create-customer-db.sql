SELECT format('CREATE DATABASE %I OWNER %I', 'customer', current_user)
WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = 'customer');
\gexec
