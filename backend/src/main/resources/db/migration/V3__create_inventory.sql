CREATE TABLE categories (
    id        UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name      VARCHAR(100) NOT NULL UNIQUE,
    parent_id UUID REFERENCES categories(id)
);

CREATE TABLE skus (
    id               UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    sku_code         VARCHAR(50) NOT NULL UNIQUE,
    name             VARCHAR(255) NOT NULL,
    category_id      UUID REFERENCES categories(id),
    weight_kg        DECIMAL(8,3),
    dimensions       JSONB,
    unit             VARCHAR(20) NOT NULL DEFAULT 'piece',
    reorder_point    INTEGER NOT NULL DEFAULT 0,
    reorder_quantity INTEGER NOT NULL DEFAULT 0,
    created_at       TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE TABLE sku_locations (
    id                UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    sku_id            UUID NOT NULL REFERENCES skus(id),
    shelf_id          UUID NOT NULL REFERENCES shelves(id),
    quantity          INTEGER NOT NULL DEFAULT 0,
    reserved_quantity INTEGER NOT NULL DEFAULT 0,
    batch_id          VARCHAR(100),
    expiry_date       DATE,
    created_at        TIMESTAMP NOT NULL DEFAULT NOW(),
    UNIQUE(sku_id, shelf_id, batch_id)
);

CREATE TABLE stock_movements (
    id             UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    sku_id         UUID NOT NULL REFERENCES skus(id),
    shelf_id       UUID REFERENCES shelves(id),
    quantity       INTEGER NOT NULL,
    movement_type  VARCHAR(30) NOT NULL,
    reference_id   UUID,
    reference_type VARCHAR(30),
    worker_id      UUID REFERENCES users(id),
    batch_id       VARCHAR(100),
    moved_at       TIMESTAMP NOT NULL DEFAULT NOW(),
    notes          TEXT
);

CREATE TABLE reorder_alerts (
    id               UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    sku_id           UUID NOT NULL REFERENCES skus(id),
    current_quantity INTEGER NOT NULL,
    reorder_point    INTEGER NOT NULL,
    status           VARCHAR(20) NOT NULL DEFAULT 'OPEN',
    created_at       TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_skus_sku_code ON skus(sku_code);
CREATE INDEX idx_skus_category ON skus(category_id);
CREATE INDEX idx_sku_locations_sku ON sku_locations(sku_id);
CREATE INDEX idx_sku_locations_shelf ON sku_locations(shelf_id);
CREATE INDEX idx_stock_movements_sku ON stock_movements(sku_id, moved_at DESC);
CREATE INDEX idx_reorder_alerts_status ON reorder_alerts(status);