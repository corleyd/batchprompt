-- Add new optional fields for job processing configuration
ALTER TABLE job ADD COLUMN max_tokens INTEGER;
ALTER TABLE job ADD COLUMN temperature NUMERIC(3,2);
ALTER TABLE job ADD COLUMN max_records INTEGER;
ALTER TABLE job ADD COLUMN start_record_number INTEGER;

-- Add indices to support potential queries on these fields
CREATE INDEX IF NOT EXISTS idx_job_max_records ON job(max_records);
CREATE INDEX IF NOT EXISTS idx_job_start_record_number ON job(start_record_number);