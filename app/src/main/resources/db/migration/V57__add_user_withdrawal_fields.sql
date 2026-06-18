ALTER TABLE users
    DROP CONSTRAINT IF EXISTS users_status_check;

ALTER TABLE users
    ADD CONSTRAINT users_status_check CHECK (status IN ('ACTIVE', 'BLOCKED', 'WITHDRAWAL_REQUESTED', 'DELETED'));

ALTER TABLE users
    ADD COLUMN IF NOT EXISTS withdrawal_requested_at TIMESTAMP,
    ADD COLUMN IF NOT EXISTS withdrawal_due_at TIMESTAMP;

CREATE INDEX IF NOT EXISTS users_withdrawal_due_idx
    ON users (withdrawal_due_at)
    WHERE status = 'WITHDRAWAL_REQUESTED';
