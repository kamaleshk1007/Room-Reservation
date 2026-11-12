# Room Reserve — Complete Project Documentation

> This document explains the entire Room Reserve project: architecture, database schema, file structure, functionality, setup, and roadmap. Anyone reading this should understand how every part connects and works.

---

## Table of Contents

1. [Project Overview](#1-project-overview)
2. [User Roles](#2-user-roles)
3. [Tech Stack](#3-tech-stack)
4. [Repository Structure](#4-repository-structure)
5. [Git Status & Branches](#5-git-status--branches)
6. [How to Start the App](#6-how-to-start-the-app)
7. [Database Schema (7 Tables)](#7-database-schema-7-tables)
8. [Backend File Structure & Functionality](#8-backend-file-structure--functionality)
9. [Frontend File Structure & Functionality](#9-frontend-file-structure--functionality)
10. [Authentication Flow](#10-authentication-flow)
11. [Request Flow: Create Reservation](#11-request-flow-create-reservation)
12. [Environment Variables](#12-environment-variables)
13. [What's Done](#13-whats-done)
14. [What's To Be Done](#14-whats-to-be-done)
15. [Troubleshooting](#15-troubleshooting)

---

## 1. Project Overview

Room Reserve is a room-reservation platform for **St. Joseph's Institute of Technology**. The college has 30+ bookable spaces (AV halls, auditoriums, studio labs, innovation labs) with no proper reservation system. Room Reserve is that system.

**Key Requirements:**
- Staff must NOT need a new login (portal-cookie SSO via existing college portal)
- Room Reserve runs on the same domain as the college portal for cookie reuse
- Double-booking protection: one room cannot be booked for overlapping time ranges
- Lab-incharge records actual start/end of room usage
- HOD approves/rejects staff reservation requests

---

## 2. User Roles

| Role | Permissions |
|------|------------|
| **Staff** | Browse rooms, create reservation requests |
| **HOD** | Review and approve/reject staff reservation requests |
| **Lab-incharge** | Start and end actual usage sessions for a reservation |

---

## 3. Tech Stack

| Layer | Technology |
|-------|-----------|
| Frontend | React 19 + TypeScript (Vite), react-router-dom 7, Tailwind CSS 4, lucide-react, oxlint |
| Backend | Spring Boot 4.1.2 (Java 21), packaged as WAR for Tomcat |
| Database | MySQL 9.7 with Flyway migrations |
| Auth (interim) | JWT (HS256, jjwt 0.12.2) |
| Auth (planned) | Portal-cookie SSO (same domain, read portal session cookie) |
| Build | Maven (mvnw wrapper) |
| Deployment | Tomcat (college server runs Tomcat; React build served from same Tomcat) |

---

## 4. Repository Structure

```
Reserve-Room/
├── backend/                          # Spring Boot backend
│   ├── pom.xml                       # Maven config
│   ├── .env                          # Gitignored local env vars
│   ├── .env.example                  # Template for .env
│   └── src/main/
│       ├── java/com/sjit/roomreservation/
│       │   ├── RoomReservationApplication.java
│       │   ├── ServletInitializer.java
│       │   ├── controller/
│       │   │   └── ReservationController.java
│       │   ├── entity/
│       │   │   ├── User.java
│       │   │   ├── Room.java
│       │   │   ├── Reservation.java
│       │   │   ├── UsageSession.java
│       │   │   ├── RefreshToken.java
│       │   │   └── AuditLog.java
│       │   ├── repository/
│       │   │   ├── UserRepository.java
│       │   │   ├── RoomRepository.java
│       │   │   ├── ReservationRepository.java
│       │   │   ├── UsageSessionRepository.java
│       │   │   ├── RefreshTokenRepository.java
│       │   │   └── AuditLogRepository.java
│       │   ├── security/
│       │   │   ├── SecurityConfig.java
│       │   │   ├── JwtUtil.java
│       │   │   ├── JwtAuthenticationFilter.java
│       │   │   ├── UserDetailsServiceImpl.java
│       │   │   └── AuthController.java
│       │   ├── service/
│       │   │   └── ReservationService.java
│       │   └── exception/
│       │       └── ResourceConflictException.java
│       └── resources/
│           ├── application.properties
│           └── db/migration/
│               ├── V1__Base_Schema.sql
│               ├── V2__Add_Usage_Sessions.sql
│               ├── V3__Add_Overlap_Protection.sql
│               └── V4__Add_Approved_Index.sql
├── frontend/                         # (empty - frontend work is on 'frontend' branch)
├── AGENTS.md                         # Project context for coding agent
├── PROJECT_DOCUMENTATION.md          # This file
└── README.md
```

---

## 5. Git Status & Branches

```
Branch: main
Status: clean (nothing to commit, working tree clean)
Latest commit: 2503924 fix: all 8 startup issues + ReservationController
Remote: https://github.com/kamaleshk1007/Room-Reservation.git
```

### Branches
| Branch | Status | Description |
|--------|--------|-------------|
| `main` | Active | Backend + migrations (this branch) |
| `frontend` | Has work | React + TypeScript HOD dashboard (mock data) |
| `feature/frontend` | Empty | .gitkeep only |
| `feature/backend` | Empty | .gitkeep only |

### Commit History
| Commit | Message |
|--------|---------|
| 2503924 | fix: all 8 startup issues + ReservationController |
| 24a9f12 | feat: complete backend skeleton with Spring Boot 4, Java 21, MySQL 9, Flyway JWT auth |
| 48f7b2f | feat: backend skeleton with Spring Boot 4, Java 21, MySQL 9, Flyway JWT auth |
| 83ce173 | feat: backend skeleton with Spring Boot 4, Java 21, MySQL 9, Flyway JWT auth |

---

## 6. How to Start the App

### Prerequisites
- Java 21 (JDK)
- MySQL 9.7 running on localhost:3306
- Maven (wrapper included: `mvnw.cmd`)

### Setup Steps

1. **Create MySQL database and user:**
   ```sql
   CREATE DATABASE IF NOT EXISTS room_reservation;
   CREATE USER 'room'@'localhost' IDENTIFIED BY '<your-db-password>';
   GRANT ALL PRIVILEGES ON room_reservation.* TO 'room'@'localhost';
   FLUSH PRIVILEGES;
   ```

2. **Configure `.env` file** (gitignored, at `backend/.env`):
   ```env
   SERVER_PORT=8080
   DB_URL=jdbc:mysql://127.0.0.1:3306/room_reservation?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC
   DB_USERNAME=<your-db-username>
   DB_PASSWORD=<your-db-password>
   JWT_SECRET=<your-base64-secret>
   JWT_ACCESS_EXPIRATION_MINUTES=30
   JWT_REFRESH_EXPIRATION_DAYS=7
   CORS_ALLOWED_ORIGINS=http://localhost:5173
   ```

3. **Build:**
   ```
   cd backend
   .\mvnw.cmd clean package -DskipTests
   ```

4. **Run:**
   ```
   .\mvnw.cmd spring-boot:run
   ```

5. **Verify:** App starts on `http://localhost:8080`. Health check: `http://localhost:8080/actuator/health`

### Important: Environment Variable Override
If you see "Access denied for user 'root_user'@'localhost'", there are system-level environment variables overriding your `.env`. Check with:
```powershell
Get-ChildItem Env: | Where-Object { $_.Name -like "*DB*" -or $_.Name -like "*DATASOURCE*" }
```
Clear them with (requires admin):
```powershell
[Environment]::SetEnvironmentVariable("SPRING_DATASOURCE_USERNAME", $null, "Machine")
[Environment]::SetEnvironmentVariable("SPRING_DATASOURCE_PASSWORD", $null, "Machine")
[Environment]::SetEnvironmentVariable("DB_PASSWORD", $null, "Machine")
```
Or set correct values in the current shell session before running.

---

## 7. Database Schema (7 Tables)

### Table: `users`
Maps portal staff to Room Reserve users. Supports SSO via `portal_staff_id`.

| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| `id` | CHAR(36) | PK | UUID primary key |
| `portal_staff_id` | VARCHAR(50) | NOT NULL, UNIQUE | College portal staff ID (SSO mapping) |
| `email` | VARCHAR(255) | NOT NULL, UNIQUE | Staff email |
| `password_hash` | VARCHAR(255) | NOT NULL | bcrypt-hashed password |
| `name` | VARCHAR(255) | NOT NULL | Full name |
| `role` | VARCHAR(50) | NOT NULL | STAFF, HOD, or LAB_INCHARGE |
| `is_active` | BOOLEAN | NOT NULL, default TRUE | Account active flag |
| `created_at` | DATETIME(6) | NOT NULL | Record creation timestamp |
| `updated_at` | DATETIME(6) | NOT NULL | Last update timestamp |

### Table: `rooms`
Bookable spaces (AV halls, auditoriums, labs).

| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| `id` | CHAR(36) | PK | UUID primary key |
| `name` | VARCHAR(255) | NOT NULL | Room name |
| `code` | VARCHAR(50) | NOT NULL, UNIQUE | Room code (e.g., AVH-01) |
| `capacity` | INT | NOT NULL | Seating capacity |
| `room_type` | VARCHAR(100) | NOT NULL | AV_HALL, AUDITORIUM, STUDIO_LAB, INNOVATION_LAB |
| `location` | VARCHAR(255) | | Building/floor location |
| `is_active` | BOOLEAN | NOT NULL, default TRUE | Room available flag |
| `created_at` | DATETIME(6) | NOT NULL | Record creation timestamp |
| `updated_at` | DATETIME(6) | NOT NULL | Last update timestamp |

### Table: `reservations`
Room booking requests with double-booking protection.

| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| `id` | CHAR(36) | PK | UUID primary key |
| `room_id` | CHAR(36) | FK → rooms.id, NOT NULL | Booked room |
| `requested_by` | CHAR(36) | FK → users.id, NOT NULL | Staff who requested |
| `start_time` | DATETIME(6) | NOT NULL | Reservation start |
| `end_time` | DATETIME(6) | NOT NULL | Reservation end |
| `purpose` | VARCHAR(255) | | Booking purpose |
| `status` | VARCHAR(50) | NOT NULL | PENDING, CONFIRMED, REJECTED, CANCELLED |
| `approved_by` | CHAR(36) | FK → users.id, NULL | HOD who approved/rejected |
| `approved_at` | DATETIME(6) | NULL | Approval timestamp |
| `created_at` | DATETIME(6) | NOT NULL | Record creation timestamp |
| `updated_at` | DATETIME(6) | NOT NULL | Last update timestamp |

**Indexes:**
- `idx_res_room_time_ovl` on (room_id, start_time, end_time, status) — supports overlap detection
- `idx_res_approved_by` on (approved_by) — fast HOD queries
- `idx_res_approved_at` on (approved_at) — fast HOD queries
- `idx_res_status_approved` on (status, approved_by, approved_at) — common HOD queries

**Overlap check (JPQL):**
```sql
SELECT COUNT(r) > 0 FROM Reservation r
WHERE r.room.id = :roomId
  AND r.status IN ('CONFIRMED', 'PENDING')
  AND r.id != :excludeId
  AND r.startTime < :endTime
  AND r.endTime > :startTime
```

### Table: `usage_sessions`
Actual room usage tracking by lab-incharge.

| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| `id` | CHAR(36) | PK | UUID primary key |
| `reservation_id` | CHAR(36) | FK → reservations.id, NOT NULL | Associated reservation |
| `started_by` | CHAR(36) | FK → users.id, NOT NULL | Lab-incharge who started |
| `started_at` | DATETIME(6) | NOT NULL | Session start time |
| `ended_at` | DATETIME(6) | NULL | Session end time |
| `notes` | TEXT | | Session notes |
| `created_at` | DATETIME(6) | NOT NULL | Record creation timestamp |

### Table: `refresh_tokens`
JWT refresh token storage for auth.

| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| `id` | CHAR(36) | PK | UUID primary key |
| `user_id` | CHAR(36) | FK → users.id, NOT NULL | Token owner |
| `token` | VARCHAR(512) | NOT NULL, UNIQUE | Refresh token string |
| `expires_at` | DATETIME(6) | NOT NULL | Token expiry |
| `is_revoked` | BOOLEAN | NOT NULL, default FALSE | Revocation flag |
| `created_at` | DATETIME(6) | NOT NULL | Token creation timestamp |

### Table: `audit_logs`
Action audit trail.

| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| `id` | CHAR(36) | PK | UUID primary key |
| `user_id` | CHAR(36) | FK → users.id | Actor |
| `action` | VARCHAR(100) | NOT NULL | Action performed |
| `entity_type` | VARCHAR(100) | | Target entity type |
| `entity_id` | CHAR(36) | | Target entity ID |
| `details` | TEXT | | Additional details |
| `created_at` | DATETIME(6) | NOT NULL | Action timestamp |

### Entity Relationships
```
users (1) ──── (N) reservations [requested_by]
users (1) ──── (N) reservations [approved_by]
rooms (1) ──── (N) reservations [room_id]
reservations (1) ──── (N) usage_sessions [reservation_id]
users (1) ──── (N) usage_sessions [started_by]
users (1) ──── (N) refresh_tokens [user_id]
users (1) ──── (N) audit_logs [user_id]
```

---

## 8. Backend File Structure & Functionality

### Security Package

| File | Purpose |
|------|---------|
| `SecurityConfig.java` | Spring Security config: stateless sessions, CORS, JWT filter, 401 for unauthenticated, permit /api/auth/** and /actuator/health |
| `JwtUtil.java` | JWT token generation (HS256) and validation using jjwt 0.12.x. Constructor injection of @Value params. 32-byte base64 secret. |
| `JwtAuthenticationFilter.java` | Filter that extracts Bearer token, validates it, sets SecurityContext with userDetails.getAuthorities() |
| `UserDetailsServiceImpl.java` | UserDetailsService loading by portal_staff_id, returns UserDetails with ROLE_ authorities |
| `AuthController.java` | Auth endpoints (login, refresh, logout) — @RestController |

### Entity Package

| File | Purpose |
|------|---------|
| `User.java` | User entity: portal_staff_id, email, password_hash, name, role, is_active |
| `Room.java` | Room entity: name, code, capacity, room_type, location, is_active |
| `Reservation.java` | Reservation entity: room, requestedBy, startTime, endTime, purpose, status, approvedBy, approvedAt |
| `UsageSession.java` | Usage session: reservation, startedBy, startedAt, endedAt, notes |
| `RefreshToken.java` | Refresh token: user, token, expiresAt, isRevoked |
| `AuditLog.java` | Audit log: user, action, entityType, entityId, details |

### Repository Package

| File | Purpose |
|------|---------|
| `UserRepository.java` | findByPortalStaffId, findByEmail |
| `RoomRepository.java` | JpaRepository<Room, String> |
| `ReservationRepository.java` | existsOverlapByRoom (double-booking check with r.room.id) |
| `UsageSessionRepository.java` | JpaRepository<UsageSession, String> |
| `RefreshTokenRepository.java` | findByTokenAndValid (uses CURRENT_TIMESTAMP) |
| `AuditLogRepository.java` | JpaRepository<AuditLog, String> |

### Service Package

| File | Purpose |
|------|---------|
| `ReservationService.java` | save() with overlap check before persisting. Throws ResourceConflictException on double-booking. |

### Controller Package

| File | Purpose |
|------|---------|
| `ReservationController.java` | POST /api/reservations — creates reservation with start < end validation |

### Exception Package

| File | Purpose |
|------|---------|
| `ResourceConflictException.java` | Custom RuntimeException for double-booking conflicts |

---

## 9. Frontend File Structure & Functionality

The frontend is on the `frontend` branch (not yet merged to main). It contains a React + TypeScript HOD dashboard running on mock data.

**Tech:** React 19, react-router-dom 7, Tailwind CSS 4, lucide-react, oxlint, Vite

**Status:** Running on mock data. Not yet connected to backend APIs.

---

## 10. Authentication Flow

### Current: JWT (Interim)
```
Client (POST /api/auth/login with email + password)
  │
  ▼
AuthController → AuthenticationManager → UserDetailsServiceImpl
  ├── Loads user by portal_staff_id (or email)
  └── Returns UserDetails with ROLE_ authorities
  │
  ▼
JwtUtil.generateAccessToken(username) → signed HS256 JWT
JwtUtil.generateRefreshToken(username) → signed HS256 JWT
  │
  ▼
Response: { accessToken, refreshToken }
```

### JWT Token Validation (on every request)
```
Client (Authorization: Bearer <token>)
  │
  ▼
JwtAuthenticationFilter.doFilterInternal()
  ├── Extracts raw token from "Bearer " prefix
  ├── Calls JwtUtil.extractUsername(token) → portal_staff_id
  ├── Calls UserDetailsServiceImpl.loadUserByUsername(portal_staff_id)
  │     └── Returns UserDetails with ROLE_ authorities
  ├── Calls JwtUtil.validateToken(token, userDetails)
  │     └── Checks username match + not expired
  └── Sets SecurityContext with UsernamePasswordAuthenticationToken(userDetails, null, authorities)
```

### Planned: Portal-Cookie SSO
- Staff clicks button on college portal → redirects to Room Reserve
- Room Reserve reads and verifies portal's existing session cookie
- Maps portal staff ID → Room Reserve user record
- Requires same domain for cookie reuse
- Real portal cookie name, domain, expiry: UNKNOWN (design behind clean interface)

---

## 11. Request Flow: Create Reservation

```
Client (POST /api/reservations with JSON body)
  │
  ▼
ReservationController.createReservation()
  ├── Validates start < end (returns 400 if invalid)
  └── Calls ReservationService.save()
        ├── Calls ReservationRepository.existsOverlapByRoom()
        │     ├── Query: r.room.id = :roomId AND r.status IN ('CONFIRMED','PENDING')
        │     │          AND r.id != :excludeId AND r.startTime < :endTime AND r.endTime > :startTime
        │     └── Returns boolean
        ├── If overlap → throws ResourceConflictException → 409 Conflict
        └── If no overlap → reservationRepository.save() → 201 Created
```

---

## 12. Environment Variables

| Variable | Default | Description |
|----------|---------|-------------|
| `SERVER_PORT` | 8080 | HTTP server port |
| `DB_URL` | jdbc:mysql://localhost:3306/room_reservation | MySQL JDBC URL |
| `DB_USERNAME` | room_user | MySQL username |
| `DB_PASSWORD` | room_password | MySQL password |
| `JWT_SECRET` | (required, no default) | Base64-encoded 32+ byte secret |
| `JWT_ACCESS_EXPIRATION_MINUTES` | 30 | Access token TTL in minutes |
| `JWT_REFRESH_EXPIRATION_DAYS` | 7 | Refresh token TTL in days |
| `CORS_ALLOWED_ORIGINS` | http://localhost:5173 | Comma-separated allowed origins |

---

## 13. What's Done

### Backend (Java/Spring Boot)
- [x] Spring Boot 4.1.2 project initialized (WAR packaging)
- [x] 7 JPA entities (User, Room, Reservation, UsageSession, RefreshToken, AuditLog)
- [x] 6 Spring Data JPA repositories
- [x] Flyway migrations (V1-V4) for schema + indexes
- [x] JWT authentication (HS256 signed tokens, jjwt 0.12.x)
- [x] JWT authentication filter (stateless, sets SecurityContext)
- [x] UserDetailsService loading by portal_staff_id with ROLE_ authorities
- [x] Security config (stateless sessions, CORS, formLogin/httpBasic disabled, 401 for unauth)
- [x] ReservationService with double-booking protection
- [x] ReservationController (POST /api/reservations with start<end validation)
- [x] ResourceConflictException for double-booking
- [x] CORS configuration from app.cors.allowed-origins
- [x] Java 21 pinned in pom.xml
- [x] Environment variables in .env (gitignored)

### Database
- [x] 7 tables with UUID primary keys
- [x] Foreign keys and relationships
- [x] refresh_tokens table for JWT auth
- [x] usage_sessions table for lab-incharge tracking
- [x] approved_by and approved_at fields on reservations
- [x] Double-booking protection (overlap check query + index)
- [x] Flyway migrations V1-V4

### Frontend
- [x] React + TypeScript HOD dashboard (on `frontend` branch)
- [x] Running on mock data

---

## 14. What's To Be Done

### High Priority
- [ ] AuthController full implementation (login, refresh, logout endpoints)
- [ ] Room CRUD API (browse, search, filter rooms)
- [ ] Reservation list/query API (by user, room, status, date range)
- [ ] HOD approval/rejection API (approve/reject reservations)
- [ ] Usage session start/end API (lab-incharge tracking)
- [ ] Role-based access control (@PreAuthorize on endpoints)
- [ ] Input validation (@Valid on request bodies)

### Medium Priority
- [ ] Portal-cookie SSO (replace JWT with portal session verification)
- [ ] Frontend-backend API integration
- [ ] Frontend: Staff reservation page
- [ ] Frontend: Lab-incharge usage tracking
- [ ] Error handling and global exception handler

### Low Priority
- [ ] Unit and integration tests
- [ ] API documentation (Swagger/OpenAPI)
- [ ] Logging and monitoring
- [ ] Performance optimization

---

## 15. Troubleshooting

### "Access denied for user 'nfc_app'@'localhost'"
- **Cause:** System-level environment variables `SPRING_DATASOURCE_USERNAME=nfc_app` and `SPRING_DATASOURCE_PASSWORD=...` override your `.env` file.
- **Fix:** Set correct values in current shell session:
  ```powershell
  $env:DB_PASSWORD = "<your-db-password>"
  $env:DB_USERNAME = "<your-db-username>"
  $env:SPRING_DATASOURCE_PASSWORD = "<your-db-password>"
  $env:SPRING_DATASOURCE_USERNAME = "<your-db-username>"
  ```

### "Migration checksum mismatch for migration version 3"
- **Cause:** V3 migration file edited after being applied.
- **Fix:** Update checksum in database:
  ```sql
  UPDATE flyway_schema_history SET checksum = 374229444 WHERE version = '3';
  ```

### "Could not resolve placeholder 'JWT_SECRET'"
- **Cause:** `.env` file missing or `JWT_SECRET` not set.
- **Fix:** Create `backend/.env` with valid base64 32-byte secret.

### App won't start (MySQL connection)
- Ensure MySQL is running: `Get-Service MySQL*`
- Ensure user exists: `CREATE USER 'room'@'localhost' IDENTIFIED BY '<your-db-password>';`
- Ensure database exists: `CREATE DATABASE room_reservation;`
- Grant privileges: `GRANT ALL PRIVILEGES ON room_reservation.* TO 'room'@'localhost';`

---

*Documentation for Room Reserve project. Last updated: 2026-11-12.*
*Commit: 2503924*
*Branch: main*
