CREATE TABLE payment_authorizations (
    order_id UUID PRIMARY KEY,
    amount NUMERIC(31,2) NOT NULL CHECK (amount >= 0),
    currency TEXT NOT NULL CHECK (currency = 'PEN'),
    outcome TEXT NOT NULL CHECK (outcome IN ('APPROVED', 'DECLINED')),
    authorized_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    published BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE INDEX payment_pending_results_idx ON payment_authorizations (authorized_at, order_id)
    WHERE published = FALSE;
