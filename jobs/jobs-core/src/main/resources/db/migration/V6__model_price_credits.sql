-- Define the conversion rate from model cost in USD to credits

create table if not exists model_credit_rate (
    model_credit_rate_uuid uuid not null primary key default gen_random_uuid(),
    model_id varchar not null references model(model_id),
    effective_begin_timestamp timestamp not null,
    credits_per_usd numeric not null,
    effective_end_timestamp timestamp,
    create_timestamp timestamp not null,
    delete_timestamp timestamp
);


alter table job_task add column credit_usage numeric;
alter table job add column credit_usage numeric;
