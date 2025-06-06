-- Create schema if it doesn't exist
CREATE SCHEMA IF NOT EXISTS waitlist;

-- Use the waitlist schema
SET search_path TO waitlist;

CREATE TABLE waitlist_entry (
    id UUID NOT NULL PRIMARY KEY DEFAULT gen_random_uuid(),
    email VARCHAR(255) NOT NULL UNIQUE,
    name VARCHAR(255),
    company VARCHAR(255),
    use_case TEXT,
    position INTEGER,
    status VARCHAR(50) NOT NULL DEFAULT 'PENDING',
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    invited_at TIMESTAMP,
    registered_at TIMESTAMP
);

-- Create indexes for frequently queried columns
CREATE INDEX idx_waitlist_email ON waitlist_entry (email);
CREATE INDEX idx_waitlist_status ON waitlist_entry (status);
CREATE INDEX idx_waitlist_position ON waitlist_entry (position);
CREATE INDEX idx_waitlist_created_at ON waitlist_entry (created_at);