-- V8__add_packing_task_number.sql
ALTER TABLE packing_tasks ADD COLUMN task_number VARCHAR(30);

-- Backfill existing rows so the NOT NULL + UNIQUE constraints below don't fail
UPDATE packing_tasks
SET task_number = 'PT-LEGACY-' || SUBSTRING(id::text, 1, 8)
WHERE task_number IS NULL;

ALTER TABLE packing_tasks ALTER COLUMN task_number SET NOT NULL;
ALTER TABLE packing_tasks ADD CONSTRAINT uq_packing_tasks_task_number UNIQUE (task_number);