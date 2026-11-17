# Deploying Room Reserve

Target architecture:

    Cloudflare Pages (React frontend)
        │  HTTPS
        ▼
    Render (Spring Boot backend, Docker)
        │  TLS (sslMode=VERIFY_IDENTITY)
        ▼
    TiDB Cloud Starter (MySQL-compatible database)

Local development is unaffected: run `start-all.cmd` at the repo root.

## 1. Database (TiDB Cloud Starter)

1. Create a TiDB Cloud Starter cluster.
2. Open **Connect** on the cluster's overview page → **Public** → copy the
   connection details. The user name has a required, cluster-specific **prefix**
   (e.g. `<prefix>.root`) and the password is generated in that dialog.
3. TiDB Cloud Starter only accepts TLS 1.2/1.3 connections. Certificates are issued
   by **Let's Encrypt (ISRG Root X1)**, which is present in the JDK truststore,
   so no custom truststore is needed. Use `sslMode=VERIFY_IDENTITY`.
4. Create the `room_reservation` database, e.g. by connecting with any MySQL client
   and running `CREATE DATABASE room_reservation;`. Tables come from Flyway (below).
5. JDBC URL shape:
   `jdbc:mysql://<host>:4000/room_reservation?sslMode=VERIFY_IDENTITY&enabledTLSProtocols=TLSv1.2,TLSv1.3&serverTimezone=UTC`

### Flyway

- Migrations `V1__Base_Schema.sql` … `V5__Add_Room_Location_And_Active.sql` run automatically on startup.
- They contain no triggers, stored procedures, or functions — only tables, indexes,
  foreign keys and seed inserts (`UUID()`, `NOW(6)`), all TiDB-compatible (`utf8mb4`, `DATETIME(6)`).
- `flyway-database-tidb` is not published for the project's Flyway version (latest is 10.26.2),
  so Flyway talks to TiDB Cloud over its MySQL-compatible wire protocol via `flyway-mysql`.
- Baseline-on-migrate is enabled; migrations are applied with `spring.flyway.baseline-on-migrate=true`.

### Seed users

`V1__Base_Schema.sql` seeds the four rooms and the `hod001` / `staff001` portal IDs
with **example bcrypt hashes** (intentionally not working passwords). Before any
shared deployment, replace the example hashes in `V1__Base_Schema.sql` or insert
real accounts with bcrypt hashes generated out-of-band.

## 2. Backend (Render)

Use Render's **Docker** runtime with the repository root or `backend/Dockerfile`.

- **Dockerfile path:** `backend/Dockerfile`
- **Health check path:** `/actuator/health` (unauthenticated)
- Render sets `PORT` automatically and the container listens on `0.0.0.0:$PORT`.
- Suggested env: `JAVA_TOOL_OPTIONS=-Xms64m -Xmx256m`

Environment variables (names only, set in the Render service):

| Name | Purpose |
| --- | --- |
| `DB_URL` | JDBC URL from step 1 (TiDB Cloud Starter, `sslMode=VERIFY_IDENTITY`) |
| `DB_USERNAME` | TiDB user (with cluster prefix) |
| `DB_PASSWORD` | TiDB password |
| `JWT_SECRET` | Base64-encoded secret (32+ bytes) for signing access/refresh tokens; generate with `openssl rand -base64 32` |
| `CORS_ALLOWED_ORIGINS` | Comma-separated list of allowed frontend origins (include the Cloudflare Pages URL) |
| `COOKIE_SECURE` | `true` in production (HTTPS); `false` for local HTTP |
| `PORTAL_COOKIE_SECRET` | *(optional)* Base64 secret used to verify the college portal's `PORTAL_SESSION` cookie. If unset, SSO is disabled and `POST /api/auth/portal-sso` returns 503; the app still boots. |
| `PORT` | set by Render, no default (defaults to `8080` locally) |
| `JAVA_TOOL_OPTIONS` | *(suggested)* `-Xms64m -Xmx256m` |

### Alternative: WAR on college Tomcat

`mvn clean package -DskipTests` produces `backend/target/room-reservation-0.0.1-SNAPSHOT.war`,
embeddable as a classic WAR for the college Tomcat. All configuration remains env-var driven,
so Tomcat must see the same environment variables.

## 3. Frontend (Cloudflare Pages)

- Build command: `pnpm build`
- Output directory: `dist`
- Environment variable: `VITE_API_BASE_URL` — the Render backend origin (no trailing slash)
- SPA fallback: `frontend/public/_redirects` ships `/*    /index.html   200`, so deep routes
  such as `/rooms/manage` and `/reservations` survive a page refresh on Pages.

The frontend performs no other configuration. `CORS_ALLOWED_ORIGINS` on the backend must include
the Pages origin (scheme + host, e.g. `https://room-reserve.pages.dev`).

## 4. Env var reference (backend, names only)

`DB_URL` · `DB_USERNAME` · `DB_PASSWORD` · `JWT_SECRET` · `JWT_ACCESS_EXPIRATION_MINUTES` ·
`JWT_REFRESH_EXPIRATION_DAYS` · `CORS_ALLOWED_ORIGINS` · `COOKIE_SECURE` · `PORTAL_COOKIE_SECRET` ·
`PORT` · `JAVA_TOOL_OPTIONS`

No secret has a default in `application.properties`. Known defaults (non-secrets):
`PORT=8080`, `JWT_ACCESS_EXPIRATION_MINUTES=30`, `JWT_REFRESH_EXPIRATION_DAYS=7`,
`CORS_ALLOWED_ORIGINS=http://localhost:5173` (local frontend only), `COOKIE_SECURE=false`.

## 5. Verification after deploy

1. `GET https://<backend>/actuator/health` → `{"status":"UP"}`.
2. `POST https://<backend>/api/auth/login` with a seeded portal staff ID → tokens returned.
3. Open the Pages URL → app loads → dashboard or login page appear.
4. On refresh inside `/rooms/manage`, the Pages SPA fallback serves `index.html` instead of 404.
