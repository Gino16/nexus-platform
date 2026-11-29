ALTER TABLE purchase_orders
    ADD COLUMN payment_requested BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN payment_result TEXT CHECK (payment_result IN ('APPROVED', 'DECLINED'));

ALTER TABLE order_outbox
    DROP CONSTRAINT order_outbox_pkey,
    ADD PRIMARY KEY (order_id, event_type);
