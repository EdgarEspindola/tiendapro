CREATE TABLE sale (
    id SERIAL PRIMARY KEY,
    sale_date TIMESTAMP NOT NULL
);

CREATE TABLE sale_item (
    id SERIAL PRIMARY KEY,
    product_id BIGINT NOT NULL,
    quantity INT NOT NULL,
    unit_price DECIMAL(10,2),
    sale_id BIGINT REFERENCES sale(id) ON DELETE CASCADE
);