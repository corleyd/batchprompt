-- Add thinking_text_column_name column to prompt table
ALTER TABLE prompt ADD COLUMN thinking_text_column_name VARCHAR(255);

-- Add comment to describe the column
COMMENT ON COLUMN prompt.thinking_text_column_name IS 'Optional column name where the thinking text from the LLM will be stored in the output';
