alter table model add column simulate_structured_output boolean not null default false;

update model set simulate_structured_output = true where model_id in ('openai-gpt-3-5-turbo', 'openai-gpt-4')