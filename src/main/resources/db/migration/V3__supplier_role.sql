-- v1.1 — Supplier role & portal.
-- Adds the company a SUPPLIER user represents. Null for MERCHANT/ADMIN accounts.
ALTER TABLE users
    ADD COLUMN IF NOT EXISTS company_id UUID REFERENCES companies(id) ON DELETE SET NULL;

CREATE INDEX IF NOT EXISTS idx_users_company ON users(company_id);
