-- V7__scope_aisle_shelf_uniqueness_to_parent.sql
-- Aisle codes were globally unique (one "A1" for the whole warehouse);
-- Shelf codes were globally unique the same way. Real warehouse naming
-- reuses codes per parent (every zone has an "A1", every aisle has a
-- "G01"), so this scopes uniqueness to the immediate parent instead:
-- aisle code unique per zone, shelf code unique per aisle.

ALTER TABLE aisles DROP CONSTRAINT aisles_code_key;
ALTER TABLE aisles ADD CONSTRAINT uq_aisles_zone_code UNIQUE (zone_id, code);

ALTER TABLE shelves DROP CONSTRAINT shelves_code_key;
ALTER TABLE shelves ADD CONSTRAINT uq_shelves_aisle_code UNIQUE (aisle_id, code);