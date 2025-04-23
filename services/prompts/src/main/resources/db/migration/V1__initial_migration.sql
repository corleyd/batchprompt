-- Create schema if it doesn't exist
CREATE SCHEMA IF NOT EXISTS prompts;

-- Use the prompts schema
SET search_path TO prompts;

create table prompt (
    prompt_uuid uuid not null primary key,
    user_id varchar(255) not null,
    name varchar(255) not null,
    description varchar(255) not null,
    prompt_text text not null,
    output_schema jsonb not null,
    create_timestamp timestamp not null,
    update_timestamp timestamp not null
);
