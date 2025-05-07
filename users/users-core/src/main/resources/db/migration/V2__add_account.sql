-- Use the users schema
SET search_path TO users;

-- Create the accounts table
CREATE TABLE account (
    account_uuid UUID PRIMARY KEY,
    name VARCHAR(255) NOT NULL UNIQUE,
    create_timestamp TIMESTAMP NOT NULL,
    update_timestamp TIMESTAMP NOT NULL
);

-- Create the mapping between users and accounts
CREATE TABLE account_user (
    account_uuid UUID NOT NULL REFERENCES account(account_uuid) ON DELETE CASCADE,
    user_uuid UUID NOT NULL REFERENCES "user"(user_uuid) ON DELETE CASCADE,
    is_owner BOOLEAN NOT NULL DEFAULT FALSE,
    create_timestamp TIMESTAMP NOT NULL,
    update_timestamp TIMESTAMP NOT NULL,
    PRIMARY KEY (account_uuid, user_uuid)
);

-- Track credit transactions for an account
CREATE TABLE account_credit_transaction (
    transaction_uuid UUID PRIMARY KEY,
    account_uuid UUID NOT NULL REFERENCES account(account_uuid) ON DELETE CASCADE,
    change_amount NUMERIC NOT NULL,
    reason VARCHAR(255) NOT NULL,
    reference_id VARCHAR(255),
    create_timestamp TIMESTAMP NOT NULL
);

-- View to compute the current balance of credits for an account
CREATE VIEW account_credit_balance AS
SELECT
    account_uuid,
    SUM(change_amount) AS current_balance
FROM
    account_credit_transaction
GROUP BY
    account_uuid;

-- Populate default accounts for existing users
DO $$
DECLARE
    r RECORD;
    now_ts TIMESTAMP := CURRENT_TIMESTAMP;
BEGIN
    FOR r IN SELECT user_uuid, user_id FROM "user" LOOP
        -- Create an account for each user
        INSERT INTO account (
            account_uuid,
            name,
            create_timestamp,
            update_timestamp
        ) VALUES (
            gen_random_uuid(),
            r.user_id || '_account',
            now_ts,
            now_ts
        );

        -- Associate the user with the newly created account and set as owner
        INSERT INTO account_user (
            account_uuid,
            user_uuid,
            is_owner,
            create_timestamp,
            update_timestamp
        )
        SELECT
            a.account_uuid,
            r.user_uuid,
            TRUE,
            now_ts,
            now_ts
        FROM account a
        WHERE a.name = r.user_id || '_account';
    END LOOP;
END $$;
