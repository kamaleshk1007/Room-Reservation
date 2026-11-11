# Room Reservation

A room-reservation platform for St. Joseph's Institute of Technology.

## Tech stack

- Frontend: React 19, TypeScript, Vite, Tailwind CSS 4
- Backend: Spring Boot 4, Java 21, Maven, WAR packaging
- Database: MySQL 9
- Auth: JWT with refresh tokens, bcrypt password hashing, portal-cookie SSO (planned)

## Repository layout

- `backend/` - Spring Boot application
- `frontend/` - React application (planned)

## Backend setup

Requirements: Java 21, Maven 3.9+, MySQL 9.

```sh
cd backend
copy .env.example .env
# edit .env with your local database credentials
./mvnw spring-boot:run
```

The application listens on `http://localhost:8080` by default. The health
endpoint is `/actuator/health`.

### Database schema

The database schema is managed with Flyway. Migrations live in
`backend/src/main/resources/db/migration`. The schema is created from the
team's schema definition (`training_infrastructure_db`); migrations must match
that definition.

### Configuration

All configuration is environment-driven. See `backend/.env.example` for the
available keys. Never commit `.env` or real credentials.

## Git workflow

Work happens on feature branches. Open a pull request for mentor review.
`main` is protected.
