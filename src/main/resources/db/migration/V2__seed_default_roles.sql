-- Replaces the old data.sql seed (which relied on defer-datasource-initialization).
-- Kept idempotent (INSERT ... WHERE NOT EXISTS) because environments baselined at
-- V2 or later never execute this file, but any environment that does run it may
-- already carry these rows from the pre-Flyway data.sql seeding.
INSERT INTO roles (id, name) SELECT 1, 'USER' WHERE NOT EXISTS (SELECT 1 FROM roles WHERE name = 'USER');
INSERT INTO roles (id, name) SELECT 2, 'ADMIN' WHERE NOT EXISTS (SELECT 1 FROM roles WHERE name = 'ADMIN');
