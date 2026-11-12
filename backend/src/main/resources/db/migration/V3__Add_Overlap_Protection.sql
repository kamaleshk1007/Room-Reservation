-- =============================================================================
-- V3__Add_Overlap_Protection.sql
-- Index to support fast overlap detection for the same room
-- The double-booking protection is also enforced by the application layer
-- (see trg_prevent_room_overlap trigger note in SCHEMA_ASSUMPTIONS.md)
-- =============================================================================

-- --------------------------------------------------------
-- Index for fast overlap detection (covering index)
-- --------------------------------------------------------
CREATE INDEX idx_res_room_time_ovl ON reservations (room_id, start_time, end_time, status);