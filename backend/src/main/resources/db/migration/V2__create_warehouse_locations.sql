CREATE TABLE zones (
    id                UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name              VARCHAR(100) NOT NULL UNIQUE,
    type              VARCHAR(50) NOT NULL,
    capacity          INTEGER NOT NULL,
    current_occupancy INTEGER NOT NULL DEFAULT 0,
    created_at        TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE TABLE aisles (
    id      UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    zone_id UUID NOT NULL REFERENCES zones(id) ON DELETE CASCADE,
    code    VARCHAR(20) NOT NULL UNIQUE
);

CREATE TABLE shelves (
    id             UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    aisle_id       UUID NOT NULL REFERENCES aisles(id) ON DELETE CASCADE,
    level          VARCHAR(20) NOT NULL,
    code           VARCHAR(30) NOT NULL UNIQUE,
    max_weight     DECIMAL(10,2),
    current_weight DECIMAL(10,2) NOT NULL DEFAULT 0
);

CREATE INDEX idx_aisles_zone_id ON aisles(zone_id);
CREATE INDEX idx_shelves_aisle_id ON shelves(aisle_id);