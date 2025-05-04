-- Create schema if it doesn't exist
CREATE SCHEMA IF NOT EXISTS users;

-- Use the users schema
SET search_path TO users;

CREATE TABLE "user" (
    user_uuid uuid NOT NULL PRIMARY KEY,
    user_id VARCHAR(255) NOT NULL UNIQUE,
    email VARCHAR(255) NOT NULL UNIQUE,
    name VARCHAR(255) NOT NULL,
    picture VARCHAR(255),
    role VARCHAR(50) NOT NULL,
    enabled BOOLEAN NOT NULL,
    create_timestamp TIMESTAMP NOT NULL,
    update_timestamp TIMESTAMP NOT NULL
);

-- Create indexes for frequently queried columns
CREATE INDEX idx_user_user_id ON "user" (user_id);
CREATE INDEX idx_user_email ON "user" (email);