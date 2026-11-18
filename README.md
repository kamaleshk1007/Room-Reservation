# Room Reserve

Room Reserve manages room reservations for St. Joseph's Institute of Technology.
The college has 30+ bookable spaces (AV halls, auditoriums, studio labs, innovation
labs). Staff request a room and time, the HOD approves or rejects, and a lab
in-charge will record actual usage.

**Live demo:** <https://room-reservation-dsp.pages.dev>

The free Render instance sleeps when there is no traffic, so the first load after
an idle period can take about 50 seconds.

## Features by role

### Staff
- Browse rooms and their capacity/type
- New Reservation page with a college-period slot picker (multi-select consecutive
  periods, or a custom time range) or a full custom time range, validated against
  confirmed bookings
- View own reservations (paginated), see status, cancel their own pending/confirmed
  future reservations
- Dashboard with own pending/confirmed counts and the next upcoming reservations

### HOD
- Approvals queue with status/room filtering and real pagination
- Approve or reject reservations; `approved_by` and `approved_at` are recorded and
  an audit log entry is written per action
- Double-booking protection is enforced at both creation and approval time
- Manage rooms (add, edit, soft-delete). Delete is blocked with a 409 and the
  blocking reservation count when the room has upcoming confirmed or pending
  reservations
- Dashboard with pending approvals count, today's confirmed reservations, and
  per-room utilisation for the day

### Lab in-charge
- Not implemented yet. The schema (`usage_sessions`, Flyway migration V2) and entity
  exist, and the role is part of the user model, but the API and UI for recording
  usage start/end are not built.

## Tech stack

| Layer | Tech |
| --- | --- |
| Frontend | React 19 + Vite + Tailwind CSS |
| Backend | Spring Boot, Java 21 |
| Database | MySQL (TiDB Cloud in production) |
| Migrations | Flyway |
| Auth | JWT access tokens; refresh token in an HttpOnly cookie; bcrypt hashing |

## Architecture

The interesting part is double-booking protection. When a reservation is created or
approved, the backend takes a pessimistic write lock (`SELECT ... FOR UPDATE`) on
the room row inside a transaction, then checks for overlap against confirmed
reservations. Pending reservations are allowed to overlap (they aren't confirmed
yet); approval rejects with `409` if a confirmed reservation already overlaps.
Every create/approve/reject/cancel writes an audit entry.

## Running locally

Prerequisites: Java 21, Maven 3.9+, Node 20+, pnpm 9+, a running MySQL server.

```powershell
start-all.cmd
```

This checks the MySQL97 service, then starts the dummy portal (8090), the backend
(8080) and the frontend (5173) in separate windows, waits for each health check,
and prints the three URLs plus a PASS/FAIL line. `stop-all.cmd` stops the three
services.

Frontend is at <http://localhost:5173>, backend at <http://localhost:8080>, dummy
portal at <http://localhost:8090>.

Configuration (names only):
- `backend/.env`: `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, `JWT_SECRET`,
  `JWT_ACCESS_EXPIRATION_MINUTES`, `JWT_REFRESH_EXPIRATION_DAYS`,
  `CORS_ALLOWED_ORIGINS`, `COOKIE_SECURE`, `PORTAL_COOKIE_SECRET`
- `dummy-portal/.env`: `PORTAL_DB_URL`, `PORTAL_DB_USERNAME`, `PORTAL_DB_PASSWORD`,
  `PORTAL_COOKIE_SECRET`, seed passwords `PORTAL_SEED_HOD_PASSWORD`,
  `PORTAL_SEED_STAFF_PASSWORD`, `PORTAL_SEED_STAFF002_PASSWORD`
- `frontend/.env`: `VITE_API_BASE_URL`

See `DEPLOY.md` for the TiDB/Render/Cloudflare deployment guide.

## Deployment

- Database: TiDB Cloud Starter (MySQL-compatible), TLS, `sslMode=VERIFY_IDENTITY`.
  Migrations V1-V5 run automatically on startup and include no triggers or stored
  procedures.
- Backend: Render (Docker, `backend/Dockerfile`, multi-stage Maven build),
  health check at `/actuator/health`. Note that the free Render instance sleeps when
  idle; wake time is what causes the ~50 s first load on the live demo.
- Frontend: Cloudflare Pages; build `pnpm build`, output `dist/`, SPA fallback via
  `public/_redirects`.

## Repository layout

- `backend/` — Spring Boot API and Flyway migrations
- `frontend/` — React + Vite + Tailwind app
- `dummy-portal/` — stand-in for the college portal used to test portal-cookie SSO
- `scripts/` — local utilities (e.g. make-user-sql.cmd for inserting a real user with a real bcrypt hash)

## Notes

- Room Reserve authenticates users from the college portal's session cookie when
  deployed on the same domain. Local development uses JWT login with refresh tokens
  and a dummy portal.
- `main` is protected. All work goes through pull requests.
- `PROJECT_DOCUMENTATION.md` holds the detailed architecture notes and
  `DEPLOY.md` the deployment runbook.
