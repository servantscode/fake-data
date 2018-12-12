create database servantscode;
CREATE USER servant1 WITH LOGIN PASSWORD 'servant!IsH3r3';
GRANT ALL ON DATABASE servantscode TO servant1;

-- Then go run the database script in person-svc