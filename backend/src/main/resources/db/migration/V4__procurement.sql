-- Suppliers
CREATE TABLE suppliers (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name          VARCHAR(255) NOT NULL,
    code          VARCHAR(50)  NOT NULL UNIQUE,
    contact_info  JSONB,
    payment_terms INTEGER      NOT NULL DEFAULT 30,
    rating        DECIMAL(3,2),
    status        VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE',
    created_at    TIMESTAMP    NOT NULL DEFAULT NOW()
);

-- Purchase Orders
CREATE TABLE purchase_orders (
    id                UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    po_number         VARCHAR(50)   NOT NULL UNIQUE,
    supplier_id       UUID          NOT NULL REFERENCES suppliers(id),
    requested_by      UUID          REFERENCES users(id),
    approved_by       UUID          REFERENCES users(id),
    status            VARCHAR(30)   NOT NULL DEFAULT 'DRAFT',
    expected_delivery DATE,
    total_amount      DECIMAL(12,2),
    created_at        TIMESTAMP     NOT NULL DEFAULT NOW()
);

-- Purchase Order Lines
CREATE TABLE purchase_order_lines (
    id                UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    po_id             UUID          NOT NULL REFERENCES purchase_orders(id),
    sku_id            UUID          NOT NULL REFERENCES skus(id),
    quantity_ordered  INTEGER       NOT NULL,
    quantity_received INTEGER       NOT NULL DEFAULT 0,
    unit_price        DECIMAL(10,2) NOT NULL,
    status            VARCHAR(30)   NOT NULL DEFAULT 'PENDING'
);

-- Goods Receipts
CREATE TABLE goods_receipts (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    po_id       UUID      NOT NULL REFERENCES purchase_orders(id),
    received_by UUID      REFERENCES users(id),
    received_at TIMESTAMP NOT NULL DEFAULT NOW(),
    notes       TEXT,
    gr_number   VARCHAR(50)  NOT NULL UNIQUE
);

-- Goods Receipt Lines
CREATE TABLE goods_receipt_lines (
    id                UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    goods_receipt_id  UUID    NOT NULL REFERENCES goods_receipts(id),
    po_line_id        UUID    NOT NULL REFERENCES purchase_order_lines(id),
    sku_id            UUID    NOT NULL REFERENCES skus(id),
    quantity_received INTEGER NOT NULL,
    batch_id          VARCHAR(100),
    expiry_date       DATE,
    shelf_id          UUID    REFERENCES shelves(id)
);

-- Supplier Invoices
CREATE TABLE supplier_invoices (
    id                     UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    po_id                  UUID          NOT NULL REFERENCES purchase_orders(id),
    supplier_id            UUID          NOT NULL REFERENCES suppliers(id),
    invoice_number         VARCHAR(100)  NOT NULL,
    invoice_amount         DECIMAL(12,2) NOT NULL,
    status                 VARCHAR(20)   NOT NULL DEFAULT 'PENDING',
    three_way_match_status VARCHAR(20)   NOT NULL DEFAULT 'PENDING',
    created_at             TIMESTAMP     NOT NULL DEFAULT NOW()
);

-- Indexes
CREATE INDEX idx_purchase_orders_supplier ON purchase_orders(supplier_id);
CREATE INDEX idx_purchase_orders_status   ON purchase_orders(status);
CREATE INDEX idx_po_lines_po_id           ON purchase_order_lines(po_id);
CREATE INDEX idx_goods_receipts_po        ON goods_receipts(po_id);
CREATE INDEX idx_invoices_po_id           ON supplier_invoices(po_id);
CREATE INDEX idx_invoices_supplier        ON supplier_invoices(supplier_id);