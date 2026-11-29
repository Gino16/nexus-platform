CREATE TABLE purchase_orders (
    id UUID PRIMARY KEY,
    customer_id UUID NOT NULL,
    payment_outcome TEXT NOT NULL CHECK (payment_outcome IN ('APPROVED','DECLINED')),
    status TEXT NOT NULL CHECK (status IN ('PENDING','PROCESSING','CONFIRMED','REJECTED')),
    rejection_reason TEXT,
    created_at TIMESTAMPTZ NOT NULL,
    next_attempt TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX pending_orders_idx ON purchase_orders (next_attempt, id)
    WHERE status IN ('PENDING','PROCESSING');

CREATE TABLE order_lines (
    order_id UUID NOT NULL REFERENCES purchase_orders (id),
    product_id UUID NOT NULL,
    quantity INTEGER NOT NULL CHECK (quantity > 0),
    unit_price NUMERIC(19,2) NOT NULL CHECK (unit_price >= 0),
    PRIMARY KEY (order_id, product_id)
);

CREATE TABLE order_outbox (
    order_id UUID PRIMARY KEY REFERENCES purchase_orders (id),
    event_type TEXT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    published BOOLEAN NOT NULL DEFAULT FALSE
);
