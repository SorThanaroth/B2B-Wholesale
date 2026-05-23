
ALTER TABLE users ADD COLUMN IF NOT EXISTS phone          VARCHAR(30);
ALTER TABLE users ADD COLUMN IF NOT EXISTS email_verified BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE users ALTER COLUMN created_at TYPE TIMESTAMPTZ;

ALTER TABLE companies ADD COLUMN IF NOT EXISTS logo_url      VARCHAR(512);
ALTER TABLE companies ADD COLUMN IF NOT EXISTS contact_email VARCHAR(255);
ALTER TABLE companies ADD COLUMN IF NOT EXISTS created_at    TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP;

CREATE TABLE categories (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name        VARCHAR(255) NOT NULL,
    description TEXT,
    parent_id   UUID REFERENCES categories(id) ON DELETE SET NULL,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_categories_parent ON categories(parent_id);

CREATE TABLE products (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    company_id    UUID NOT NULL REFERENCES companies(id) ON DELETE CASCADE,
    category_id   UUID REFERENCES categories(id) ON DELETE SET NULL,
    name          VARCHAR(255) NOT NULL,
    description   TEXT,
    price         NUMERIC(12,2) NOT NULL CHECK (price >= 0),
    min_order_qty INTEGER NOT NULL DEFAULT 1 CHECK (min_order_qty >= 1),
    unit          VARCHAR(50) NOT NULL DEFAULT 'unit',
    stock         INTEGER NOT NULL DEFAULT 0 CHECK (stock >= 0),
    image_url     VARCHAR(512),
    status        VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at    TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_products_company  ON products(company_id);
CREATE INDEX idx_products_category ON products(category_id);
CREATE INDEX idx_products_status   ON products(status);
CREATE INDEX idx_products_name_trgm ON products USING gin (to_tsvector('simple', name || ' ' || coalesce(description,'')));

CREATE TABLE carts (
    id         UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id    UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    status     VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',   -- ACTIVE | CHECKED_OUT
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_carts_user ON carts(user_id);
CREATE UNIQUE INDEX uq_carts_one_active_per_user ON carts(user_id) WHERE status = 'ACTIVE';

CREATE TABLE cart_items (
    id         UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    cart_id    UUID NOT NULL REFERENCES carts(id) ON DELETE CASCADE,
    product_id UUID NOT NULL REFERENCES products(id) ON DELETE CASCADE,
    quantity   INTEGER NOT NULL CHECK (quantity >= 1),
    unit_price NUMERIC(12,2) NOT NULL,
    subtotal   NUMERIC(12,2) NOT NULL,
    CONSTRAINT uq_cart_product UNIQUE (cart_id, product_id)
);
CREATE INDEX idx_cart_items_cart ON cart_items(cart_id);

CREATE TABLE orders (
    id             UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id        UUID NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
    cart_id        UUID REFERENCES carts(id) ON DELETE SET NULL,
    total_amount   NUMERIC(12,2) NOT NULL,
    status         VARCHAR(20) NOT NULL DEFAULT 'PENDING', 
    payment_status VARCHAR(20) NOT NULL DEFAULT 'PENDING', 
    qr_token       VARCHAR(128) UNIQUE,
    created_at     TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_orders_user   ON orders(user_id);
CREATE INDEX idx_orders_status ON orders(status);

CREATE TABLE order_items (
    id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    order_id     UUID NOT NULL REFERENCES orders(id) ON DELETE CASCADE,
    product_id   UUID NOT NULL REFERENCES products(id) ON DELETE RESTRICT,
    company_id   UUID NOT NULL REFERENCES companies(id) ON DELETE RESTRICT,
    product_name VARCHAR(255) NOT NULL,
    quantity     INTEGER NOT NULL,
    unit_price   NUMERIC(12,2) NOT NULL,
    subtotal     NUMERIC(12,2) NOT NULL
);
CREATE INDEX idx_order_items_order   ON order_items(order_id);
CREATE INDEX idx_order_items_company ON order_items(company_id);

CREATE TABLE order_company_splits (
    id             UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    order_id       UUID NOT NULL REFERENCES orders(id) ON DELETE CASCADE,
    company_id     UUID NOT NULL REFERENCES companies(id) ON DELETE RESTRICT,
    subtotal       NUMERIC(12,2) NOT NULL,
    payment_status VARCHAR(25) NOT NULL DEFAULT 'PENDING', -- PENDING|PENDING_SETTLEMENT|SETTLED
    paid_at        TIMESTAMPTZ,
    settled_at     TIMESTAMPTZ,
    CONSTRAINT uq_order_company UNIQUE (order_id, company_id)
);
CREATE INDEX idx_splits_order   ON order_company_splits(order_id);
CREATE INDEX idx_splits_company ON order_company_splits(company_id);
CREATE INDEX idx_splits_status  ON order_company_splits(payment_status);

CREATE TABLE payments (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    order_id    UUID NOT NULL REFERENCES orders(id) ON DELETE CASCADE,
    qr_code_url VARCHAR(512),
    amount      NUMERIC(12,2) NOT NULL,
    status      VARCHAR(20) NOT NULL DEFAULT 'PENDING',  -- PENDING|PAID|FAILED|EXPIRED
    gateway_ref VARCHAR(128),
    paid_at     TIMESTAMPTZ,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_payments_order ON payments(order_id);

CREATE TABLE addresses (
    id         UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id    UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    label      VARCHAR(100),
    street     VARCHAR(255) NOT NULL,
    city       VARCHAR(100) NOT NULL,
    province   VARCHAR(100),
    is_default BOOLEAN NOT NULL DEFAULT FALSE
);
CREATE INDEX idx_addresses_user ON addresses(user_id);

CREATE TABLE refresh_tokens (
    id         UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id    UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    token      VARCHAR(255) UNIQUE NOT NULL,
    expires_at TIMESTAMPTZ NOT NULL,
    revoked    BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_refresh_tokens_user ON refresh_tokens(user_id);

CREATE TABLE verification_tokens (
    id         UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id    UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    token      VARCHAR(255) UNIQUE NOT NULL,
    type       VARCHAR(30) NOT NULL,                    -- EMAIL_VERIFY | PASSWORD_RESET
    expires_at TIMESTAMPTZ NOT NULL,
    used       BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_verification_tokens_user ON verification_tokens(user_id);
