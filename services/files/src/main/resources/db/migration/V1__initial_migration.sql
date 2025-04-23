-- Create schema if it doesn't exist
CREATE SCHEMA IF NOT EXISTS files;

-- Use the files schema
SET search_path TO files;

-- Create the file table
CREATE TABLE IF NOT EXISTS file (
    file_uuid UUID PRIMARY KEY,
    file_type VARCHAR(50) NOT NULL,
    user_id VARCHAR(255) NOT NULL,
    file_name VARCHAR(255) NOT NULL,
    content_type VARCHAR(255) NOT NULL,
    file_size BIGINT NOT NULL,
    status VARCHAR(50) NOT NULL,
    validation_errors JSONB,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);

-- Create index on user_id
CREATE INDEX IF NOT EXISTS idx_file_user_id ON file(user_id);
CREATE INDEX IF NOT EXISTS idx_file_type ON file(file_type);
CREATE INDEX IF NOT EXISTS idx_file_status ON file(status);

-- Create the file_record table
CREATE TABLE IF NOT EXISTS file_record (
    file_record_uuid UUID PRIMARY KEY,
    file_uuid UUID NOT NULL REFERENCES file(file_uuid) ON DELETE CASCADE,
    record_number INTEGER NOT NULL,
    record JSONB NOT NULL
);

-- Create index on file_uuid
CREATE INDEX IF NOT EXISTS idx_file_record_file_uuid ON file_record(file_uuid);