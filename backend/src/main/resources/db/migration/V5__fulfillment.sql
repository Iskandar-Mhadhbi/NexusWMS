-- ============================================================
-- V5 — Order Fulfillment
-- orders, order_lines, fulfillment_requests,
-- pick_lists, pick_list_items, packing_stations,
-- packing_tasks, packages, carriers, shipments
-- ============================================================

-- Carriers (referenced by shipments — create first)
CREATE TABLE carriers (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name        VARCHAR(255) NOT NULL,
    code        VARCHAR(50)  NOT NULL UNIQUE,
    contact_info JSONB,
    is_active   BOOLEAN NOT NULL DEFAULT TRUE,
    created_at  TIMESTAMP NOT NULL DEFAULT NOW()
);

-- Orders
CREATE TABLE orders (
    id               UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    order_number     VARCHAR(50) NOT NULL UNIQUE,
    customer_name    VARCHAR(255) NOT NULL,
    customer_address JSONB,
    status           VARCHAR(30) NOT NULL DEFAULT 'RECEIVED',
    priority         VARCHAR(20) NOT NULL DEFAULT 'STANDARD',
    notes            TEXT,
    created_by       UUID NOT NULL,
    created_at       TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at       TIMESTAMP NOT NULL DEFAULT NOW()
);

-- Order Lines
CREATE TABLE order_lines (
    id               UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    order_id         UUID NOT NULL REFERENCES orders(id),
    sku_id           UUID NOT NULL,
    quantity_ordered INTEGER NOT NULL,
    quantity_picked  INTEGER NOT NULL DEFAULT 0,
    quantity_packed  INTEGER NOT NULL DEFAULT 0,
    unit_price       NUMERIC(12, 2) NOT NULL,
    status           VARCHAR(30) NOT NULL DEFAULT 'PENDING',
    created_at       TIMESTAMP NOT NULL DEFAULT NOW()
);

-- Fulfillment Requests
CREATE TABLE fulfillment_requests (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    order_id      UUID NOT NULL REFERENCES orders(id),
    assigned_zone VARCHAR(100),
    status        VARCHAR(30) NOT NULL DEFAULT 'PENDING',
    generated_by  UUID NOT NULL,
    created_at    TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE TABLE pick_lists (
    id                     UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    fulfillment_request_id UUID NOT NULL REFERENCES fulfillment_requests(id),
    assigned_to            UUID NOT NULL,
    generated_by           UUID NOT NULL,
    status                 VARCHAR(30) NOT NULL DEFAULT 'GENERATED',
    generated_at           TIMESTAMP NOT NULL DEFAULT NOW(),
    completed_at           TIMESTAMP
);

-- Pick List Items
CREATE TABLE pick_list_items (
    id               UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    pick_list_id     UUID NOT NULL REFERENCES pick_lists(id),
    order_line_id    UUID NOT NULL REFERENCES order_lines(id),
    sku_id           UUID NOT NULL,
    shelf_id         UUID NOT NULL,
    shelf_code       VARCHAR(50),
    quantity_to_pick INTEGER NOT NULL,
    quantity_picked  INTEGER NOT NULL DEFAULT 0,
    batch_id         VARCHAR(100),
    status           VARCHAR(30) NOT NULL DEFAULT 'PENDING',
    picked_at        TIMESTAMP
);

-- Packing Stations
CREATE TABLE packing_stations (
    id        UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    zone_id   UUID NOT NULL,
    code      VARCHAR(50) NOT NULL UNIQUE,
    is_active BOOLEAN NOT NULL DEFAULT TRUE
);
 
--packing task
CREATE TABLE packing_tasks (
    id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    pick_list_id UUID NOT NULL REFERENCES pick_lists(id),
    assigned_to  UUID NOT NULL,
    created_by   UUID NOT NULL,
    station_id   UUID,
    status       VARCHAR(30) NOT NULL DEFAULT 'PENDING',
    started_at   TIMESTAMP,
    completed_at TIMESTAMP
);

-- Packages
CREATE TABLE packages (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    order_id        UUID NOT NULL REFERENCES orders(id),
    packing_task_id UUID REFERENCES packing_tasks(id),
    tracking_number VARCHAR(100) NOT NULL UNIQUE,
    barcode         VARCHAR(100) NOT NULL UNIQUE,
    weight_kg       NUMERIC(8, 3),
    dimensions      JSONB,
    status          VARCHAR(30) NOT NULL DEFAULT 'PACKED',
    packed_at       TIMESTAMP NOT NULL DEFAULT NOW()
);

-- Shipments
CREATE TABLE shipments (
    id                      UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    package_id              UUID NOT NULL REFERENCES packages(id),
    carrier_id              UUID NOT NULL REFERENCES carriers(id),
    carrier_tracking_number VARCHAR(100),
    dispatched_by           UUID NOT NULL,
    dispatched_at           TIMESTAMP NOT NULL DEFAULT NOW(),
    estimated_delivery      DATE,
    status                  VARCHAR(30) NOT NULL DEFAULT 'PENDING'
);

-- Indexes
CREATE INDEX idx_orders_status         ON orders(status);
CREATE INDEX idx_order_lines_order_id  ON order_lines(order_id);
CREATE INDEX idx_order_lines_sku_id    ON order_lines(sku_id);
CREATE INDEX idx_pick_lists_assigned   ON pick_lists(assigned_to);
CREATE INDEX idx_pick_lists_status     ON pick_lists(status);
CREATE INDEX idx_pick_items_list_id    ON pick_list_items(pick_list_id);
CREATE INDEX idx_packages_order_id     ON packages(order_id);
CREATE INDEX idx_shipments_package_id  ON shipments(package_id);