-- V5: Room management additions (location + soft-delete flag)
-- Adds an optional location and an is_active flag used for soft deletion.
-- Existing rooms default to active so nothing disappears from the catalogue.
-- Split into separate single-change statements (TiDB-compatible).

ALTER TABLE rooms
    ADD COLUMN location VARCHAR(255) NULL AFTER status;

ALTER TABLE rooms
    ADD COLUMN is_active TINYINT(1) NOT NULL DEFAULT 1 AFTER location;
