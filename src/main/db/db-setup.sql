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
\c servantscode servant1

-- Then go run the database script in person-svc

-- Then create initial login
INSERT INTO families(surname, addr_street1, addr_city, addr_state, addr_zip) values ('Leitheiser', '849 Dalmalley Ln', 'Coppell', 'TX', 75019);
INSERT INTO people(name, email, family_id, head_of_house, member_since) values ('Greg Leitheiser', 'greg@servantscode.org', 1, true, now());
INSERT INTO logins(person_id, hashed_password, role_id) VALUES (1, '$2a$10$ymleJy8knsspIL2c3dNnIu4c2onSsJxzOU0pBVzHs/GlSwXdFzuwO', 1);

INSERT INTO families(surname, addr_street1, addr_city, addr_state, addr_zip) values ('Jakubik', '8013 Lynores Way', 'Plano', 'TX', 75025);
INSERT INTO people(name, email, family_id, head_of_house, member_since) values ('Collin Jakubik', 'collin.jakubik@gmail.com', 1, true, now());
INSERT INTO logins(person_id, hashed_password, role_id) VALUES (1, '$2a$10$cNnyHlMCMiDQENuchb9Z1eMfm9GFUB5X6wX71mhFB7U9SRRJtbPbC', 1);

INSERT INTO families(surname, addr_street1, addr_city, addr_state, addr_zip) values ('Lukeman', '101 Somewhere', 'Coppell', 'TX', 75019);
INSERT INTO people(name, email, family_id, head_of_house, member_since) values ('Dave Lukeman', 'dave@servantscode.org', 2, true, now());
INSERT INTO logins(person_id, hashed_password, role_id) VALUES (2, '$2a$10$8r8VPc49rI30WL9.pVpPleZRZL2Qt/ksB6bhXtnAI451Nz1yNFk6C', 1);

INSERT INTO families(surname, addr_street1, addr_city, addr_state, addr_zip) values ('Rodriguez', '101 Somewhere Else', 'Coppell', 'TX', 75019);
INSERT INTO people(name, email, family_id, head_of_house, member_since) values ('Erik Rodriguez', 'erik@servantscode.org', 3, true, now());
INSERT INTO logins(person_id, hashed_password, role_id) VALUES (3, '$2a$10$8r8VPc49rI30WL9.pVpPleZRZL2Qt/ksB6bhXtnAI451Nz1yNFk6C', 1);

INSERT INTO families(surname, addr_street1, addr_city, addr_state, addr_zip) values ('Whitburn', '101 Somewhere New', 'Coppell', 'TX', 75019);
INSERT INTO people(name, email, family_id, head_of_house, member_since) values ('Brian Whitburn', 'brian@servantscode.org', 4, true, now());
INSERT INTO logins(person_id, hashed_password, role_id) VALUES (4, '$2a$10$8r8VPc49rI30WL9.pVpPleZRZL2Qt/ksB6bhXtnAI451Nz1yNFk6C', 1);
