-- Query to help determine the default credit_per_usd

with costs as (
select m.model_id, t.input_tokens, t.output_tokens, 
case when coalesce(mc.input_token_1m_cost_usd,0) != 0
then
(t.input_tokens * (mc.input_token_1m_cost_usd / 1000000.0)) + 
(t.output_tokens * (mc.output_token_1m_cost_usd / 1000000.0)) 
end cost_usd
from jobs.model m join jobs.model_cost mc
on (mc.model_id = m.model_id and current_timestamp between coalesce(effective_begin_timestamp,'1/1/1900') and coalesce(effective_end_timestamp,'12/31/9999'))
cross join (
select 500 input_tokens, 750 output_tokens
) t
)
select c.*, 
c.cost_usd / b.cost_usd credits,
(c.cost_usd / b.cost_usd) / c.cost_usd model_credits_per_usd
from costs c cross join costs b
where
b.model_id = 'openai-gpt-3.5-turbo'