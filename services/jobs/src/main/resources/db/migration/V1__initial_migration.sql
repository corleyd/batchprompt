create table if not exists job (
    job_uuid uuid not null primary key,
    user_id varchar not null,
    file_uuid uuid not null,
    prompt_uuid uuid not null,
    model_name varchar not null,
    status varchar not null,
    task_count integer not null,
    completed_task_count integer not null,
    created_at timestamp not null,
    updated_at timestamp not null
);

create table if not exists job_task (
    job_task_uuid uuid not null primary key,
    job_uuid uuid not null references job (job_uuid),
    file_record_uuid uuid not null,
    model_name varchar not null,
    response_text varchar,
    status varchar not null,
    error_message varchar,
    begin_timestamp timestamp,
    end_timestamp timestamp
);

CREATE INDEX IF NOT EXISTS idx_job_user_id ON jobs.job(user_id);
CREATE INDEX IF NOT EXISTS idx_job_file_uuid ON jobs.job(file_uuid);
CREATE INDEX IF NOT EXISTS idx_job_task_job_uuid ON jobs.job_task(job_uuid);
