create table model_provider (
    model_provider_id varchar not null primary key,
    display_name varchar not null
);

create table if not exists model (
    model_id varchar not null primary key,
    display_name varchar,
    model_provider_id varchar not null,
    model_provider_model_id varchar not null,
    model_provider_properties jsonb,
    model_provider_display_order int,
    task_queue_name varchar not null
);

create table if not exists model_cost (
    model_cost_uuid uuid not null primary key default gen_random_uuid(),
    model_id varchar not null references model(model_id),
    min_input_tokens int,
    max_input_tokens int,
    input_token_1m_cost_usd numeric not null,
    output_token_1m_cost_usd numeric not null,
    thinking_token_1m_cost_usd numeric not null,
    effective_begin_timestamp timestamp not null,
    effective_end_timestamp timestamp
);

alter table job rename column model_name to model_id;

alter table job_task rename column model_name to model_id;
alter table job_task add column calculated_cost_usd numeric;
alter table job_task add column thinking_tokens int;

-- Insert model providers
INSERT INTO model_provider (model_provider_id, display_name) VALUES 
('OPENAI', 'OpenAI'),
('AWS', 'AWS'),
('GOOGLE', 'Google'),
('XAI', 'xAI');

-- Insert models
INSERT INTO model (model_id, display_name, model_provider_id, model_provider_model_id, model_provider_display_order, task_queue_name) VALUES 
('openai-o3', 'OpenAI o3', 'OPENAI', 'o3', 1, 'job-task-openai-o3'),
('openai-o4-mini', 'OpenAI o4-mini', 'OPENAI', 'o4-mini', 2, 'job-task-openai-o4-mini'),
('openai-gpt-4o', 'OpenAI GPT-4o', 'OPENAI', 'gpt-4o', 3, 'job-task-openai-gpt-4o'),
('openai-gpt-4', 'OpenAI GPT-4', 'OPENAI', 'gpt-4', 4, 'job-task-openai-gpt-4'),
('openai-gpt-3-5-turbo', 'OpenAI GPT-3.5 Turbo', 'OPENAI', 'gpt-3.5-turbo', 5, 'job-task-openai-gpt-3-5-turbo'),
('aws-deepseek-1', 'AWS DeepSeek-R1', 'AWS', 'arn:aws:bedrock:us-east-1:187419035811:inference-profile/us.deepseek.r1-v1:0', 1, 'job-task-aws-deepseek-r1'),
('google-gemini-2-0-flash', 'Google Gemini 2.0 Flash', 'GOOGLE', 'gemini-2.0-flash', 1, 'job-task-google-gemini-2-0-flash'),
('google-gemini-2-0-flash-lite', 'Google Gemini 2.0 Flash Lite', 'GOOGLE', 'gemini-2.0-flash-lite', 2, 'job-task-google-gemini-2-0-flash-lite'),
('google-gemini-2-5-pro', 'Google Gemini 2.5 Pro', 'GOOGLE', 'gemini-2.5-pro', 3, 'job-task-google-gemini-2-5-pro'),
('google-gemini-2-5-flash', 'Google Gemini 2.5 Flash', 'GOOGLE', 'gemini-2.5-flash', 4, 'job-task-google-gemini-2-5-flash'),
('google-gemini-1-5-flash', 'Google Gemini 1.5 Flash', 'GOOGLE', 'gemini-1.5-flash', 5, 'job-task-google-gemini-1-5-flash'),
('google-gemini-1-5-pro', 'Google Gemini 1.5 Pro', 'GOOGLE', 'gemini-1.5-pro', 6, 'job-task-google-gemini-1-5-pro'),
('xai-grok-3', 'xAI Grok-3', 'XAI', 'grok-3', 1, 'job-task-xai-grok-3'),
('xai-grok-3-fast', 'xAI Grok-3 Fast', 'XAI', 'grok-3-fast', 2, 'job-task-xai-grok-3-fast'),
('xai-grok-3-mini', 'xAI Grok-3 Mini', 'XAI', 'grok-3-mini', 3, 'job-task-xai-grok-3-mini'),
('xai-grok-3-mini-fast', 'xAI Grok-3 Mini Fast', 'XAI', 'grok-3-mini-fast', 4, 'job-task-xai-grok-3-mini-fast');





-- Insert model costs (using current timestamp for effective begin date)
INSERT INTO model_cost (model_id, min_input_tokens, max_input_tokens, input_token_1m_cost_usd, output_token_1m_cost_usd, thinking_token_1m_cost_usd, effective_begin_timestamp) VALUES
('openai-o3', NULL, NULL, 10.0, 40.0, 0.0, CURRENT_TIMESTAMP),
('openai-o4-mini', NULL, NULL, 1.1, 4.4, 0.0, CURRENT_TIMESTAMP),
('openai-gpt-4o', NULL, NULL, 2.5, 10.0, 0.0, CURRENT_TIMESTAMP),
('openai-gpt-4', NULL, NULL, 30.0, 60.0, 0.0, CURRENT_TIMESTAMP),
('openai-gpt-3-5-turbo', NULL, NULL, 0.5, 1.5, 0.0, CURRENT_TIMESTAMP),
('aws-deepseek-1', NULL, NULL, 1.35, 5.4, 0.0, CURRENT_TIMESTAMP),
('google-gemini-2-0-flash', NULL, NULL, 0.1, 0.4, 0.0, CURRENT_TIMESTAMP),
('google-gemini-2-0-flash-lite', NULL, NULL, 0.075, 0.3, 0.0, CURRENT_TIMESTAMP),
('google-gemini-2-5-pro', NULL, NULL, 2.5, 15.0, 0.0, CURRENT_TIMESTAMP),
('google-gemini-2-5-flash', NULL, NULL, 0.15, 0.6, 3.5, CURRENT_TIMESTAMP),
('google-gemini-1-5-flash', NULL, NULL, 0.15, 0.6, 0.0, CURRENT_TIMESTAMP),
('google-gemini-1-5-pro', NULL, NULL, 2.5, 10.0, 0.0, CURRENT_TIMESTAMP),
('xai-grok-3', NULL, NULL, 3.0, 15.0, 0.0, CURRENT_TIMESTAMP),
('xai-grok-3-fast', NULL, NULL, 5.0, 25.0, 0.0, CURRENT_TIMESTAMP),
('xai-grok-3-mini', NULL, NULL, 0.3, 0.5, 0.0, CURRENT_TIMESTAMP),
('xai-grok-3-mini-fast', NULL, NULL, 0.6, 4.0, 0.0, CURRENT_TIMESTAMP);


