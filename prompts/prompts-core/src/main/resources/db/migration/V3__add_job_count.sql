alter table prompt add column job_run_count integer default 0 not null;
alter table prompt add column last_job_run_timestamp timestamp;
alter table prompt add column delete_timestamp timestamp;
