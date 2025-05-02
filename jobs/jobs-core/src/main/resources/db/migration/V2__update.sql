create table job_output_field (
    job_output_field_uuid uuid not null primary key,
    job_uuid uuid not null references job (job_uuid),
    field_order integer not null,
    field_uuid uuid not null
);

create index if not exists idx_job_output_field_job_uuid ON jobs.job_output_field(job_uuid);