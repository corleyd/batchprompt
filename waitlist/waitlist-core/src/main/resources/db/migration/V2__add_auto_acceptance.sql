-- Add auto acceptance configuration table
CREATE TABLE waitlist_auto_acceptance (
    id UUID NOT NULL PRIMARY KEY DEFAULT gen_random_uuid(),
    remaining_auto_accept_count INTEGER NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    created_by VARCHAR(255),
    notes TEXT
);

-- Insert initial configuration row
INSERT INTO waitlist_auto_acceptance (remaining_auto_accept_count, created_by, notes) 
VALUES (0, 'SYSTEM', 'Initial auto-acceptance configuration');

-- Create index for performance
CREATE INDEX idx_waitlist_auto_acceptance_remaining_count ON waitlist_auto_acceptance (remaining_auto_accept_count);
