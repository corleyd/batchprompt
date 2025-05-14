alter table job_task add column cost_estimate numeric;
alter table job add column cost_estimate numeric;
alter table job_task add column prompt_text varchar;
alter table job_task add column estimated_completion_tokens int;
alter table job_task add column estimated_thinking_tokens int;

update job_task set prompt_text = '';
alter table job_task alter column prompt_text set not null;

alter table job_validation_result_message drop column job_task_uuid;

alter table jobs.job alter column task_count drop not null;