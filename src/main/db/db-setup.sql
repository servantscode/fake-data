-- Clean up existing database if necessary
SELECT pg_terminate_backend(pg_stat_activity.pid)
FROM pg_stat_activity
WHERE pg_stat_activity.datname = 'servantscode'
  AND pid <> pg_backend_pid();
DROP DATABASE servantscode;
DROP USER servant1;

-- Create new database and user
create database servantscode;
CREATE USER servant1 WITH LOGIN PASSWORD 'servant!IsH3r3';
GRANT ALL ON DATABASE servantscode TO servant1;

-- Then go run the database script in person-svc
