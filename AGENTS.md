Room Reserve
Project context for the coding agent
====================================================

PROJECT
-------
A room-reservation platform for St. Joseph's Institute of Technology.
The college has 30+ bookable spaces (AV halls, auditoriums, studio labs,
innovation labs) with no proper reservation system. Room Reserve is that system.

USER ROLES
----------
- Staff: browse rooms, create reservation requests.
- HOD: review and approve/reject staff reservation requests.
- Lab-incharge: start and end actual usage sessions for a reservation.

TECH STACK (hard constraints)
-----------------------------
- Frontend: React + TypeScript (Vite). Current frontend branch uses
  React 19, react-router-dom 7, Tailwind CSS 4, lucide-react, oxlint.
- Backend: Spring Boot (Java), packaged as a WAR file.
  Reason: the college server runs Tomcat; Node.js cannot be hosted there.
  The React production build must be served from the same Tomcat.
- Database: MySQL.
- Local development target: local Tomcat + local MySQL (no college server
  access yet, so all integration practice happens locally).

DATABASE (existing design: 7 tables)
-----------------------------------
- 7 tables with UUID primary keys, foreign keys, and a refresh_tokens
  table for JWT-based auth.
- Required fixes/additions on top of the current schema:
  1. Double-booking protection: an overlap check so one room cannot be
     booked for overlapping time ranges, plus a supporting index.
  2. A usage_sessions table so the Lab-incharge can record start/end of
     actual room usage.
  3. approved_by and approved_at fields to record HOD approval decisions.
- Exact table list and columns live in the team's schema file
  (training_infrastructure_db) - treat them as the source of truth; do
  not invent tables. If the schema file is not present in the repo, ask
  for it before writing migrations or entities.

AUTHENTICATION - PORTAL-COOKIE SSO (key requirement)
----------------------------------------------------
- Staff must NOT need a new login for Room Reserve. This was an explicit HOD
  objection at the first demo, and the accepted solution is:
  a button on the existing college portal's staff landing page redirects
  to the Room Reserve reservation page, and Room Reserve authenticates the user by
  reading and verifying the portal's existing session cookie.
- Room Reserve must run on the same domain as the college portal for cookie
  reuse to work.
- Mapping: portal staff ID -> Room Reserve user record.
- Interim/local development state: JWT auth with refresh tokens and
  bcrypt-hashed passwords stored server-side. Never store plaintext
  passwords. A plain localStorage-only login is a demo skeleton, not the
  final design.
- UNKNOWN (do not guess): the real portal cookie's name, domain, and
  expiry are not known yet because there is no college portal/server
  access. Design the cookie-verification step behind a clean interface
  so the real values can be plugged in later.

CURRENT REPO STATE (verified 2026-11-11)
----------------------------------------
- Main repo: github.com/francko93/training-infrastructure-management-system
  (private). Branches: main, frontend, feature/frontend, feature/backend.
  - Only the `frontend` branch has real work: a React + TypeScript HOD
    dashboard running on mock data.
  - main, feature/frontend and feature/backend are empty (.gitkeep only).
  - Spring Boot backend is NOT initialized. DB schema is NOT in the repo.
  - No PRs or issues yet.
- Second repo: github.com/vinayagam2007/Room-Reservation (public, Java):
  a "dummy portal" - Spring Boot + MySQL with branch, signup and login
  APIs, bcrypt password storage. This is the skeleton for the portal /
  SSO side of the work.
- Note: which repo is the single canonical repo going forward is an open
  team decision - do not assume; flag it if it affects the task.

GIT WORKFLOW
------------
- One shared repo, all members as collaborators.
- Work happens on feature branches -> pull request -> mentor reviews and
  merges. main is protected.

CODE QUALITY RULES (apply to everything you generate)
-----------------------------------------------------
- Production-quality, maintainable code. Not vibe coded.
- Small, typed, single-responsibility modules; meaningful names; no dead
  code, no placeholder comments left behind.
- Handle errors explicitly; validate all inputs at the boundary.
- Security is non-negotiable: bcrypt for passwords server-side, no
  secrets or credentials committed, parameterized queries only.
- UI: no purple gradients, no pill-shaped buttons, no fake reviews or
  metrics, no vague hero text, no emoji icons, no em dashes, no
  over-the-top scroll animations, no AI-slop stock photos or copy, no
  cursor animations, no fake counters.
- Do not invent scope. If a requirement is missing or ambiguous, state
  the assumption or ask, instead of hallucinating features.
- MAKE NO MISTAKES.
