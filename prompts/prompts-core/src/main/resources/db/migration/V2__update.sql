alter table prompt rename column output_schema to response_json_schema;

alter table prompt add column output_method varchar;
update prompt set output_method = 
case 
when response_json_schema->>'type' is not null then 'STRUCTURED'
else 'TEXT'
end;

alter table prompt alter column output_method set not null;

alter table prompt add column response_text_column_name varchar;

update prompt set response_text_column_name = 'response_text' 
where output_method = 'TEXT';

