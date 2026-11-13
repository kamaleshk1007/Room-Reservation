# Room Reserve Frontend

React + TypeScript + Vite single-page app for the Room Reserve reservation system.

## Prerequisites

- Node.js 20+
- pnpm 9+
- The Spring Boot backend running on the same machine (default `http://localhost:8080`)

## Setup

```bash
pnpm install
cp .env.example .env
```

`.env` contains a single value:

```
VITE_API_BASE_URL=http://localhost:8080
```

The backend must be reachable at `VITE_API_BASE_URL`. For local development the Vite dev
server runs on `http://localhost:5173`, which must be listed in the backend's
`CORS_ALLOWED_ORIGINS`.

## Scripts

| Command           | Description                          |
| ----------------- | ------------------------------------ |
| `pnpm dev`        | Start the Vite dev server (HMR)      |
| `pnpm build`      | Type-check (`tsc -b`) and production build |
| `pnpm preview`   | Serve the production build           |
| `pnpm lint`       | Run oxlint                           |

## Folder structure

```
src/
  api/         # Typed API client (JWT attach, refresh + retry, typed endpoints)
  auth/        # AuthContext, useAuth, ProtectedRoute
  components/  # ErrorBoundary, Layout
  hooks/       # useDialog (focus trap, Escape to close)
  pages/       # Login, Dashboard, Rooms, NewReservation, MyReservations, Approvals, NotFound
  App.tsx      # Route definitions
  main.tsx
```

## Notes

- Auth tokens (access + refresh) are kept in `localStorage`; the access token is sent
  as a Bearer token and the refresh token is sent as a Bearer token to `/api/auth/refresh`.
- On an expired access token the client refreshes once and retries the original request.
- Reservations use `datetime-local` inputs; the local wall time is sent without timezone
  conversion so the value matches what the user picked.

No secrets belong in this repository. Only `VITE_API_BASE_URL` is required in `.env`.
