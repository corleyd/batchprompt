insert into model_provider values ('BATCHPROMPT', 'BatchPrompt');

alter table model add column required_role varchar;

insert into model (model_id, display_name, model_provider_id, model_provider_model_id, model_provider_properties, model_provider_display_order, task_queue_name, simulate_structured_output, required_role)
values ('batchprompt-test-echo', 'Test Echo', 'BATCHPROMPT', 'echo', '{}', 0, 'job-task-batchprompt', false, 'admin');

insert into model (model_id, display_name, model_provider_id, model_provider_model_id, model_provider_properties, model_provider_display_order, task_queue_name, simulate_structured_output, required_role)
values ('batchprompt-test-random', 'Test Random', 'BATCHPROMPT', 'random', '{}', 0, 'job-task-batchprompt-random', false, 'admin');

insert into model_cost (model_cost_uuid, model_id, input_token_1m_cost_usd, output_token_1m_cost_usd, thinking_token_1m_cost_usd, effective_begin_timestamp, effective_end_timestamp)
values (gen_random_uuid(), 'batchprompt-test-echo', 0, 0, 0, current_timestamp, null);

insert into model_cost (model_cost_uuid, model_id, input_token_1m_cost_usd, output_token_1m_cost_usd, thinking_token_1m_cost_usd, effective_begin_timestamp, effective_end_timestamp)
values (gen_random_uuid(), 'batchprompt-test-random', 0, 0, 0, current_timestamp, null);

alter table model_provider add column display_order integer;
update model_provider set display_order =
case when model_provider_id = 'OPENAI' then 0
     when model_provider_id = 'GOOGLE' then 1
     when model_provider_id = 'XAI' then 2
     when model_provider_id = 'AWS' then 3
     when model_provider_id = 'BATCHPROMPT' then 4
     end
     ;

-- Script to update foreign key references to jobs.model.model_id with CASCADE UPDATE

set search_path TO jobs;

-- Drop existing foreign key constraints
ALTER TABLE job DROP CONSTRAINT IF EXISTS fk_job_model_id;
ALTER TABLE job DROP CONSTRAINT IF EXISTS job_model_id_fkey;
ALTER TABLE model_credit_rate DROP CONSTRAINT IF EXISTS fk_model_credit_rate_model_id;
ALTER TABLE model_credit_rate DROP CONSTRAINT IF EXISTS model_credit_rate_model_id_fkey;
ALTER TABLE model_cost DROP CONSTRAINT IF EXISTS fk_model_cost_model_id;
ALTER TABLE model_cost DROP CONSTRAINT IF EXISTS model_cost_model_id_fkey;
ALTER TABLE job_task DROP CONSTRAINT IF EXISTS fk_job_task_model_id;
ALTER TABLE job_task DROP CONSTRAINT IF EXISTS job_task_model_id_fkey;

update job set model_id =
case 
when model_id = 'gpt-4o' then 'openai-gpt-4o'
when model_id = 'gpt-3.5-turbo' then 'openai-gpt-3.5-turbo'
when model_id = 'openai-gpt-3-5-turbo' then 'openai-gpt-3.5-turbo'
when model_id = 'deepseek-r1' then 'aws-deepseek-1'
else model_id
end;

update job_task set model_id =
case 
when model_id = 'gpt-4o' then 'openai-gpt-4o'
when model_id = 'gpt-3.5-turbo' then 'openai-gpt-3.5-turbo'
when model_id = 'openai-gpt-3-5-turbo' then 'openai-gpt-3.5-turbo'
when model_id = 'deepseek-r1' then 'aws-deepseek-1'
else model_id
end;


-- Add new foreign key constraints with ON UPDATE CASCADE
ALTER TABLE job 
ADD CONSTRAINT job_model_id_fkey
FOREIGN KEY (model_id) REFERENCES model(model_id) 
ON UPDATE CASCADE;

ALTER TABLE model_credit_rate 
ADD CONSTRAINT model_credit_rate_model_id_fkey 
FOREIGN KEY (model_id) REFERENCES model(model_id) 
ON UPDATE CASCADE;

ALTER TABLE model_cost 
ADD CONSTRAINT model_cost_model_id_fkey 
FOREIGN KEY (model_id) REFERENCES model(model_id) 
ON UPDATE CASCADE;

ALTER TABLE job_task 
ADD CONSTRAINT job_task_model_id_fkey
FOREIGN KEY (model_id) REFERENCES model(model_id) 
ON UPDATE CASCADE;

update model set model_id =
case
when model_id = 'aws-deepseek-1' then 'aws-deepseek-r1'
when model_id = 'google-gemini-2-0-flash' then 'google-gemini-2.0-flash'
when model_id = 'google-gemini-2-0-flash-lite' then 'google-gemini-2.0-flash-lite'
when model_id = 'google-gemini-2-5-pro' then 'google-gemini-2.5-pro'
when model_id = 'google-gemini-2-5-flash' then 'google-gemini-2.5-flash'
when model_id = 'google-gemini-1-5-flash' then 'google-gemini-1.5-flash'
when model_id = 'google-gemini-1-5-pro' then 'google-gemini-1.5-pro'
when model_id = 'openai-gpt-3-5-turbo' then 'openai-gpt-3.5-turbo'
else model_id
end;

update model set model_provider_model_id = 
case 
when model_id = 'google-gemini-2.5-pro' then 'gemini-2.5-pro-preview-05-06'
when model_id = 'google-gemini-2.5-flash' then 'gemini-2.5-flash-preview-05-20'
else model_provider_model_id
end;

update model 
set model_provider_properties = jsonb_set(
    coalesce(model_provider_properties,'{}'::jsonb),
    '{useMaxCompletionTokens}',
    'true'::jsonb
)
where model_id in ('openai-o3', 'openai-o4-mini');

update model 
set model_provider_properties = jsonb_set(
    coalesce(model_provider_properties,'{}'::jsonb),
    '{supportsTemperature}',
    'false'::jsonb
)
where model_id in ('openai-o3', 'openai-o4-mini');

update jobs.model
set display_name = regexp_replace(display_name, '^([^ ]+ )','');


update model
set display_name =
case
when model_id = 'openai-o3' then 'o3'
when model_id = 'openai-o4-mini' then 'o4 mini'
when model_id = 'openai-gpt-4o' then 'GPT 4o'
when model_id = 'aws-deepseek-r1' then 'DeepSeek R1'
when model_id = 'google-gemini-2.0-flash' then 'Gemini 2.0 Flash'
when model_id = 'google-gemini-2.0-flash-lite' then 'Gemini 2.0 Flash Lite'
when model_id = 'google-gemini-2.5-pro' then 'Gemini 2.5 Pro'
when model_id = 'google-gemini-2.5-flash' then 'Gemini 2.5 Flash'
when model_id = 'google-gemini-1.5-flash' then 'Gemini 1.5 Flash'
when model_id = 'google-gemini-1.5-pro' then 'Gemini 1.5 Pro'
when model_id = 'xai-grok-3' then 'Grok 3'
when model_id = 'xai-grok-3-fast' then 'Grok 3 Fast'
when model_id = 'xai-grok-3-mini' then 'Grok 3 Mini'
when model_id = 'xai-grok-3-mini-fast' then 'Grok 3 Mini Fast'
when model_id = 'openai-gpt-4' then 'GPT 4'
when model_id = 'openai-gpt-3.5-turbo' then 'GPT 3.5 Turbo'
when model_id = 'batchprompt-test-echo' then 'Echo'
when model_id = 'batchprompt-test-random' then 'Random'
end;

