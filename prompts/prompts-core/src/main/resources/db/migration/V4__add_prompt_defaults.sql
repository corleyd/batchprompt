-- Add temperature and max_tokens columns to prompt table
ALTER TABLE prompt ADD COLUMN temperature DOUBLE PRECISION;
ALTER TABLE prompt ADD COLUMN max_tokens INTEGER;

-- Add comments to describe the columns
COMMENT ON COLUMN prompt.temperature IS 'Default temperature setting for this prompt (0.0-1.0)';
COMMENT ON COLUMN prompt.max_tokens IS 'Default maximum number of tokens to generate for this prompt';
