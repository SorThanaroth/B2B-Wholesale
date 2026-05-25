-- v1.1 — richer company profile so admins can review supplier applications before approval.
ALTER TABLE companies ADD COLUMN IF NOT EXISTS registration_no VARCHAR(100);
ALTER TABLE companies ADD COLUMN IF NOT EXISTS phone           VARCHAR(30);
ALTER TABLE companies ADD COLUMN IF NOT EXISTS address         TEXT;
ALTER TABLE companies ADD COLUMN IF NOT EXISTS description     TEXT;
