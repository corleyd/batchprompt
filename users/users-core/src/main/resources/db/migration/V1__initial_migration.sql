-- Create schema if it doesn't exist
CREATE SCHEMA IF NOT EXISTS users;

-- Use the users schema
SET search_path TO users;

CREATE TABLE "user" (
    user_id VARCHAR NOT NULL PRIMARY KEY UNIQUE,
    email VARCHAR NOT NULL UNIQUE,
    name VARCHAR NOT NULL,
    picture VARCHAR,
    role VARCHAR NOT NULL,
    enabled BOOLEAN NOT NULL,
    create_timestamp TIMESTAMP NOT NULL,
    update_timestamp TIMESTAMP NOT NULL
);

-- Create indexes for frequently queried columns
CREATE INDEX idx_user_email ON "user" (email);

