-- =============================================================================
-- V1__Base_Schema.sql
-- Initial schema for Room Reserve (7 base tables + refresh_tokens)
-- MySQL 9.x compatible
-- =============================================================================

-- --------------------------------------------------------
-- 1. rooms
-- --------------------------------------------------------
CREATE TABLE rooms (
    id CHAR(36) NOT NULL,
    name VARCHAR(255) NOT NULL,
    code VARCHAR(50) NOT NULL UNIQUE,
    capacity INT NOT NULL,
    room_type VARCHAR(100) NOT NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'ACTIVE',
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id)
) ENGINE=InnoDB;

-- --------------------------------------------------------
-- 2. users (staff users with portal mapping)
-- --------------------------------------------------------
CREATE TABLE users (
    id CHAR(36) NOT NULL,
    portal_staff_id VARCHAR(50) NOT NULL UNIQUE,
    name VARCHAR(255) NOT NULL,
    email VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    role VARCHAR(50) NOT NULL,
    is_active TINYINT(1) NOT NULL DEFAULT 1,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id)
) ENGINE=InnoDB;

-- --------------------------------------------------------
-- 3. reservations
-- --------------------------------------------------------
CREATE TABLE reservations (
    id CHAR(36) NOT NULL,
    room_id CHAR(36) NOT NULL,
    requested_by CHAR(36) NOT NULL,
    start_time DATETIME(6) NOT NULL,
    end_time DATETIME(6) NOT NULL,
    purpose VARCHAR(255),
    status VARCHAR(50) NOT NULL DEFAULT 'PENDING',
    approved_by CHAR(36) NULL,
    approved_at DATETIME(6) NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    KEY idx_res_room_time (room_id, start_time, end_time),
    CONSTRAINT fk_res_room FOREIGN KEY (room_id) REFERENCES rooms (id) ON DELETE RESTRICT,
    CONSTRAINT fk_res_requestor FOREIGN KEY (requested_by) REFERENCES users (id) ON DELETE RESTRICT,
    CONSTRAINT fk_res_approver FOREIGN KEY (approved_by) REFERENCES users (id) ON DELETE SET NULL
) ENGINE=InnoDB;

-- --------------------------------------------------------
-- 4. refresh_tokens
-- --------------------------------------------------------
CREATE TABLE refresh_tokens (
    id CHAR(36) NOT NULL,
    token VARCHAR(512) NOT NULL UNIQUE,
    user_id CHAR(36) NOT NULL,
    expires_at DATETIME(6) NOT NULL,
    is_revoked TINYINT(1) NOT NULL DEFAULT 0,
    created_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    KEY idx_ref_token (token),
    CONSTRAINT fk_ref_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
) ENGINE=InnoDB;

-- --------------------------------------------------------
-- 5. audit_log
-- --------------------------------------------------------
CREATE TABLE audit_log (
    id CHAR(36) NOT NULL,
    entity_type VARCHAR(100) NOT NULL,
    entity_id CHAR(36) NOT NULL,
    action VARCHAR(100) NOT NULL,
    performed_by CHAR(36) NOT NULL,
    details JSON NULL,
    created_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    KEY idx_audit_entity (entity_type, entity_id),
    KEY idx_audit_performed (performed_by),
    CONSTRAINT fk_audit_user FOREIGN KEY (performed_by) REFERENCES users (id) ON DELETE RESTRICT
) ENGINE=InnoDB;

-- --------------------------------------------------------
-- 6. room_availability (denormalized snapshot for quick queries)
-- --------------------------------------------------------
CREATE TABLE room_availability (
    id CHAR(36) NOT NULL,
    room_id CHAR(36) NOT NULL,
    date DATE NOT NULL,
    is_available TINYINT(1) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    UNIQUE KEY uk_room_date (room_id, date),
    CONSTRAINT fk_avail_room FOREIGN KEY (room_id) REFERENCES rooms (id) ON DELETE CASCADE
) ENGINE=InnoDB;

-- --------------------------------------------------------
-- 7. settings (single-row config table)
-- --------------------------------------------------------
CREATE TABLE settings (
    id CHAR(36) NOT NULL,
    `key` VARCHAR(100) NOT NULL UNIQUE,
    `value` TEXT NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id)
) ENGINE=InnoDB;

-- --------------------------------------------------------
-- 8. refresh_token creation helper trigger
-- --------------------------------------------------------
-- Ensure UUID id is generated via mysql `uuid()` if Hibernate doesn't override
-- (Hibernate uuid-char mapping uses uuid() by default)
-- --------------------------------------------------------

-- Set default config rows
INSERT INTO settings (id, `key`, `value`, updated_at)
VALUES (UUID(), 'app.jwt.secret', '', NOW(6));

INSERT INTO settings (id, `key`, `value`, updated_at)
VALUES (UUID(), 'app.jwt.access-token-expiration-minutes', '30', NOW(6));

INSERT INTO settings (id, `key`, `value`, updated_at)
VALUES (UUID(), 'app.jwt.refresh-token-expiration-days', '7', NOW(6));

-- Populate room_types from expected values (room_type column is VARCHAR, not enum table)
-- These are reference values; the app may add more.
INSERT INTO rooms (id, name, code, capacity, room_type, status, created_at, updated_at)
VALUES (UUID(), 'AV Hall', 'AV01', 100, 'AV_HALL', 'ACTIVE', NOW(6), NOW(6));

INSERT INTO rooms (id, name, code, capacity, room_type, status, created_at, updated_at)
VALUES (UUID(), 'Auditorium', 'AU01', 200, 'AUDITORIUM', 'ACTIVE', NOW(6), NOW(6));

INSERT INTO rooms (id, name, code, capacity, room_type, status, created_at, updated_at)
VALUES (UUID(), 'Studio Lab', 'SL01', 30, 'STUDIO_LAB', 'ACTIVE', NOW(6), NOW(6));

INSERT INTO rooms (id, name, code, capacity, room_type, status, created_at, updated_at)
VALUES (UUID(), 'Innovation Lab', 'IL01', 25, 'INNOVATION_LAB', 'ACTIVE', NOW(6), NOW(6));

-- Create a default HOD user (password_hash to be replaced with real bcrypt hash)
INSERT INTO users (id, portal_staff_id, name, email, password_hash, role, is_active, created_at, updated_at)
VALUES (UUID(), 'hod001', 'Head of Department', 'hod@sjit.ac.in',
        '$2a$10$example.hash.replace.with.real.bcrypt.hash',
        'HOD', 1, NOW(6), NOW(6));

-- Create a default staff user
INSERT INTO users (id, portal_staff_id, name, email, password_hash, role, is_active, created_at, updated_at)
VALUES (UUID(), 'staff001', 'Staff User', 'staff@sjit.ac.in',
        '$2a$10$example.hash.replace.with.real.bcrypt.hash',
        'STAFF', 1, NOW(6), NOW(6));

-- ---------------------------------------------------------------------------
-- NOTE: The above INSERT statements for users/rooms use UUID() for IDs.
-- Flyway's baseline will treat these as part of the baseline schema.
-- In production, the DBA should replace example bcrypt hashes with real ones.
-- ---------------------------------------------------------------------------