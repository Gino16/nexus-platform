CREATE TABLE stocks (
    product_id UUID PRIMARY KEY,
    available INTEGER NOT NULL CHECK (available >= 0),
    reserved INTEGER NOT NULL CHECK (reserved >= 0),
    CHECK (available::BIGINT + reserved <= 2147483647)
);

CREATE TABLE reservations (
    order_id UUID PRIMARY KEY,
    status TEXT NOT NULL CHECK (status IN ('RESERVED', 'RELEASED', 'CONFIRMED'))
);

CREATE TABLE reservation_lines (
    order_id UUID NOT NULL REFERENCES reservations (order_id),
    product_id UUID NOT NULL REFERENCES stocks (product_id),
    quantity INTEGER NOT NULL CHECK (quantity > 0),
    PRIMARY KEY (order_id, product_id)
);
