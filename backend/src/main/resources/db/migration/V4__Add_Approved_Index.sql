-- =============================================================================
-- V4__Add_Approved_Index.sql
-- Indexes on approval fields
-- approved_by/approved_at columns already added in V1__Base_Schema.sql
-- Double-booking protection is enforced by application layer + V3 index
-- =============================================================================

-- --------------------------------------------------------
-- Index on approved_by for fast HOD query filtering
-- --------------------------------------------------------
CREATE INDEX idx_res_approved_by ON reservations (approved_by);

-- Index on approved_at for fast HOD query filtering
-- --------------------------------------------------------
CREATE INDEX idx_res_approved_at ON reservations (approved_at);

-- Index on status + approval fields for common HOD queries
-- --------------------------------------------------------
CREATE INDEX idx_res_status_approved ON reservations (status, approved_by, approved_at);