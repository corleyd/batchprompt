alter table job_task add column credit_estimate numeric;
alter table job add column credit_estimate numeric;

create table if not exists job_validation_result_message (
    job_validation_result_message_uuid uuid not null primary key default gen_random_uuid(),
    job_uuid uuid not null references job(job_uuid),
    job_task_uuid uuid references job_task(job_task_uuid),
    record_number int,
    field_name varchar,
    message varchar not null
);
