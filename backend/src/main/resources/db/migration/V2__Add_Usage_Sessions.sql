-- =============================================================================
-- V2__Add_Usage_Sessions.sql
-- Usage sessions table for Lab-incharge to record start/end of actual room usage
-- =============================================================================

-- --------------------------------------------------------
-- 9. usage_sessions
-- --------------------------------------------------------
CREATE TABLE usage_sessions (
    id CHAR(36) NOT NULL,
    reservation_id CHAR(36) NOT NULL,
    start_time DATETIME(6) NOT NULL,
    end_time DATETIME(6),
    actual_duration_minutes INT,
    checked_by CHAR(36) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    KEY idx_usage_reservation (reservation_id),
    CONSTRAINT fk_usage_reservation FOREIGN KEY (reservation_id) REFERENCES reservations (id) ON DELETE RESTRICT,
    CONSTRAINT fk_usage_checker FOREIGN KEY (checked_by) REFERENCES users (id) ON DELETE RESTRICT
) ENGINE=InnoDB;

-- --------------------------------------------------------
-- Indexes for usage_sessions queries
-- --------------------------------------------------------
-- Look up active sessions by checker
CREATE INDEX idx_usage_checked_by ON usage_sessions (checked_by);
-- Find sessions overlapping a time range (if needed)
CREATE INDEX idx_usage_time_range ON usage_sessions (start_time, end_time);