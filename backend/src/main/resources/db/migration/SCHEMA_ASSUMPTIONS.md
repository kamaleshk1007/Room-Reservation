# Schema Assumptions

This document documents all assumptions made for the database schema, Flyway migrations, JPA entities, and repositories. The real schema definition lives in the team's `training_infrastructure_db` file - this document is intended for code development only and must be reconciled with that file before production deployment.

## Overall Design Philosophy

- All tables use `UUID` as the primary key (`BINARY(16)` in MySQL, represented as `CHAR(36)` with `uuid()` generation in Hibernate).
- All tables have `created_at` and `updated_at` timestamp columns.
- Soft deletes are NOT used; deletions are physical.
- The `refresh_tokens` table is a dedicated table for JWT-based auth (one token per user session).
- MySQL 9.x features (lower_case_table_names=1) are in effect - table names in lowercase.

## Assumed Base Schema (7 tables)

The following 7 tables are assumed to exist from the original design. Column definitions follow MySQL 9 conventions.

### 1. `rooms`
- `id` CHAR(36) PRIMARY KEY [UUID]
- `name` VARCHAR(255) NOT NULL
- `code` VARCHAR(50) NOT NULL UNIQUE
- `capacity` INT NOT NULL
- `room_type` VARCHAR(100) NOT NULL (e.g., "AV_HALL", "AUDITORIUM", "STUDIO_LAB", "INNOVATION_LAB")
- `status` VARCHAR(50) NOT NULL DEFAULT 'ACTIVE'
- `created_at` DATETIME(6) NOT NULL
- `updated_at` DATETIME(6) NOT NULL

### 2. `reservations`
- `id` CHAR(36) PRIMARY KEY [UUID]
- `room_id` CHAR(36) NOT NULL [FK -> rooms.id]
- `requested_by` CHAR(36) NOT NULL [FK -> users.id or staff table]
- `start_time` DATETIME(6) NOT NULL
- `end_time` DATETIME(6) NOT NULL
- `purpose` VARCHAR(255)
- `status` VARCHAR(50) NOT NULL DEFAULT 'PENDING' (PENDING, CONFIRMED, REJECTED, CANCELLED)
- `approved_by` CHAR(36) NULL [FK -> users.id - populated at HOD approval]
- `approved_at` DATETIME(6) NULL
- `created_at` DATETIME(6) NOT NULL
- `updated_at` DATETIME(6) NOT NULL

### 3. `users` (staff users)
- `id` CHAR(36) PRIMARY KEY [UUID]
- `portal_staff_id` VARCHAR(50) NOT NULL UNIQUE (maps to college portal staff ID)
- `name` VARCHAR(255) NOT NULL
- `email` VARCHAR(255) NOT NULL UNIQUE
- `password_hash` VARCHAR(255) NOT NULL (bcrypt hash)
- `role` VARCHAR(50) NOT NULL (STAFF, HOD, LAB_INCHARGE)
- `is_active` TINYINT(1) NOT NULL DEFAULT 1
- `created_at` DATETIME(6) NOT NULL
- `updated_at` DATETIME(6) NOT NULL

### 4. `refresh_tokens`
- `id` CHAR(36) PRIMARY KEY [UUID]
- `token` VARCHAR(512) NOT NULL UNIQUE
- `user_id` CHAR(36) NOT NULL [FK -> users.id]
- `expires_at` DATETIME(6) NOT NULL
- `is_revoked` TINYINT(1) NOT NULL DEFAULT 0
- `created_at` DATETIME(6) NOT NULL

### 5. `audit_log` (or similar activity tracking)
- `id` CHAR(36) PRIMARY KEY [UUID]
- `entity_type` VARCHAR(100) NOT NULL (e.g., "RESERVATION", "USAGE_SESSION")
- `entity_id` CHAR(36) NOT NULL
- `action` VARCHAR(100) NOT NULL (e.g., "CREATE", "UPDATE", "APPROVE", "START_USAGE", "END_USAGE")
- `performed_by` CHAR(36) NOT NULL [FK -> users.id]
- `details` JSON NULL
- `created_at` DATETIME(6) NOT NULL

### 6. `room_availability` / schedule snapshot (or could be derived)
- `id` CHAR(36) PRIMARY KEY [UUID]
- `room_id` CHAR(36) NOT NULL [FK -> rooms.id]
- `date` DATE NOT NULL
- `is_available` TINYINT(1) NOT NULL
- `updated_at` DATETIME(6) NOT NULL
- UNIQUE KEY `uk_room_date` (`room_id`, `date`)

### 7. `settings` or `configuration`
- `id` CHAR(36) PRIMARY KEY [UUID] (single-row table, ID = 'sys')
- `key` VARCHAR(100) NOT NULL UNIQUE
- `value` TEXT NOT NULL
- `updated_at` DATETIME(6) NOT NULL

## Required Additions (on top of the 7 tables)

### 8. `usage_sessions` [NEW - required by AGENTS.md]
- `id` CHAR(36) PRIMARY KEY [UUID]
- `reservation_id` CHAR(36) NOT NULL [FK -> reservations.id]
- `start_time` DATETIME(6) NOT NULL
- `end_time` DATETIME(6) NULL (NULL if session still active)
- `actual_duration_minutes` INT NULL
- `checked_by` CHAR(36) NOT NULL [FK -> users.id - Lab-incharge]
- `created_at` DATETIME(6) NOT NULL

**Index:** `idx_usage_reservation` (`reservation_id`)

### 9. Overlap protection on `reservations` [NEW - required by AGENTS.md]

A partial unique index (MySQL 8+ functional-based index or trigger) to prevent overlapping time ranges for the same room.

**Assumed implementation:** A `BEFORE INSERT OR UPDATE` trigger named `trg_prevent_room_overlap` that checks for overlapping time ranges. Alternatively, a partial unique index if MySQL version supports it.

**Trigger logic (simplified):**
```sql
CREATE TRIGGER trg_prevent_room_overlap
BEFORE INSERT ON reservations
FOR EACH ROW
BEGIN
  IF EXISTS (
    SELECT 1 FROM reservations r
    WHERE r.room_id = NEW.room_id
      AND r.status IN ('CONFIRMED', 'PENDING')
      AND r.start_time < NEW.end_time
      AND r.end_time > NEW.start_time
      AND r.id != NEW.id
  ) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Room double-booked: overlapping time range detected';
  END IF;
END;
```

**Index for quick overlap checks:**
```sql
CREATE INDEX idx_res_room_time ON reservations(room_id, start_time, end_time);
```

## Foreign Key Relationships Summary

| From          | To            | On Delete |
| ------------- | ------------- | --------- |
| reservations.room_id  | rooms.id      | RESTRICT  |
| reservations.requested_by | users.id   | RESTRICT  |
| reservations.approved_by | users.id   | SET NULL  |
| refresh_tokens.user_id | users.id    | CASCADE   |
| usage_sessions.reservation_id | reservations.id | RESTRICT |
| usage_sessions.checked_by | users.id   | RESTRICT  |
| room_availability.room_id | rooms.id   | CASCADE   |

## Assumptions & Open Questions

| # | Assumption | Why Needed | Reconcile With |
|---|-----------|------------|----------------|
| A1 | `users.portal_staff_id` maps to the college portal staff ID for cookie-based SSO | Required for portal-cookie SSO design | `training_infrastructure_db` - verify column name/type |
| A2 | `reservations.approved_by` and `approved_at` are NULLable until HOD approves | Supports the approval workflow | `training_infrastructure_db` - verify if these columns exist |
| A3 | `refresh_tokens` table stores one row per active JWT refresh token | JWT auth design per AGENTS.md | `training_infrastructure_db` - verify table exists |
| A4 | Overlap protection uses a `BEFORE INSERT OR UPDATE` trigger | MySQL doesn't support partial unique indexes pre-8.0; trigger is portable | `training_infrastructure_db` - verify if trigger already exists |
| A5 | `room_type` uses a constrained VARCHAR (not an enum table) | Simpler schema; enum table can be added later | `training_infrastructure_db` - verify room types |
| A6 | `users.role` contains STAFF/HOD/LAB_INCHARGE values | Three user roles defined in AGENTS.md | `training_infrastructure_db` - verify role values |
| A7 | `audit_log` table tracks all state changes | Non-functional requirement; can be dropped if needed | `training_infrastructure_db` - verify if audit table exists |
| A8 | `reservations.status` uses enum-like values: PENDING, CONFIRMED, REJECTED, CANCELLED | Supports workflow | `training_infrastructure_db` - verify status values |
| A9 | `created_at`/`updated_at` use `DATETIME(6)` (microsecond precision) | MySQL 8+ default; aligns with Hibernate config | `training_infrastructure_db` - verify precision |
| A10 | Single `settings` row (ID='sys') holds config key-value pairs | Flexible config without table proliferation | `training_infrastructure_db` - verify config approach |

## Flyway Migration Strategy

- Migration `V1__Base_Schema.sql` creates the 7 base tables + refresh_tokens
- Migration `V2__Add_Usage_Sessions.sql` creates `usage_sessions` table
- Migration `V3__Add_Overlap_Protection.sql` creates the overlap trigger + index
- Migration `V4__Add_Approved_Fields.sql` adds `approved_by`/`approved_at` to reservations (if not already present)
- All migrations set `baseline-on-migrate=true` so new devs can `flyway:migrate` fresh

## JWT Auth Assumptions

- `app.jwt.secret` in `backend/.env` is a 256-bit base64 string (32 bytes -> 44 chars base64)
- Access token TTL: 30 minutes (configurable via `JWT_ACCESS_EXPIRATION_MINUTES`)
- Refresh token TTL: 7 days (configurable via `JWT_REFRESH_EXPIRATION_DAYS`)
- Token format: HS256 algorithm
- Subject (`sub`) in JWT = `users.portal_staff_id` (maps to college portal identity)
- Refresh endpoint: `POST /api/auth/refresh` - accepts valid refresh token, returns new access+refresh tokens, revokes old refresh token, creates new one

## Next Steps

1. Obtain `training_infrastructure_db` schema file from the team and reconcile this document.
2. Adjust column types, constraints, and enum values to match the real schema.
3. Update Flyway migrations accordingly.
4. Run `./mvnw flyway:migrate` against a running MySQL instance.
5. Generate JPA entities from the reconciled schema (or manually adjust the entities in `com.sjit.roomreservation.entity`).