CREATE TABLE products (
    id UUID PRIMARY KEY,
    name TEXT NOT NULL CHECK (length(btrim(name)) > 0),
    description TEXT NOT NULL,
    price NUMERIC(19, 2) NOT NULL CHECK (price >= 0),
    active BOOLEAN NOT NULL
);

CREATE INDEX idx_products_active_name_id
    ON products (name, id)
    WHERE active = TRUE;
