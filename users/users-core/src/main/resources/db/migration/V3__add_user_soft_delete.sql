-- Add delete_timestamp column for soft deletion
-- Use the users schema
SET search_path TO users;

ALTER TABLE "user" ADD COLUMN delete_timestamp TIMESTAMP;

-- Create index for delete_timestamp to optimize queries that filter out deleted users
CREATE INDEX idx_user_delete_timestamp ON "user" (delete_timestamp);
