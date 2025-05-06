-- Add token tracking columns to job_task table
ALTER TABLE job_task ADD COLUMN estimated_prompt_tokens INTEGER;
ALTER TABLE job_task ADD COLUMN prompt_tokens INTEGER;
ALTER TABLE job_task ADD COLUMN completion_tokens INTEGER;
ALTER TABLE job_task ADD COLUMN total_tokens INTEGER;

-- Create indexes for better performance
CREATE INDEX IF NOT EXISTS idx_job_task_tokens ON jobs.job_task(prompt_tokens, completion_tokens, total_tokens);