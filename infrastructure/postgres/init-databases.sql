SELECT 'CREATE DATABASE vbank_accounts OWNER vbank'
WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = 'vbank_accounts')\gexec

SELECT 'CREATE DATABASE vbank_transactions OWNER vbank'
WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = 'vbank_transactions')\gexec

SELECT 'CREATE DATABASE vbank_logs OWNER vbank'
WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = 'vbank_logs')\gexec
