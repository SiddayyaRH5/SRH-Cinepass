# SRH CinePass

Existing React/Vite frontend and Java 21/Spring Boot backend for movie discovery and ticket booking. This archive includes cold-start, API request, connection-pool, caching, and health-check improvements while retaining the existing project structure.

## Project structure

- `client/` — React 19, Vite, Tailwind CSS frontend
- `backend/` — Java 21, Spring Boot, Spring Security, Spring Data JPA backend
- `render.yaml` — Render deployment blueprint
- `PERFORMANCE-OPTIMIZATION-REPORT.md` — findings, changes, limitations, and validation status

## Local setup

### Backend

1. Install JDK 21.
2. Copy `backend/.env.example` to `backend/.env` for your local environment, or export the variables in your shell.
3. Set `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, and a `JWT_SECRET` containing at least 32 UTF-8 bytes. Set TMDB variables if using TMDB integration.
4. From `backend/`, run `./mvnw spring-boot:run` (Windows: `mvnw.cmd spring-boot:run`).

The backend uses `ddl-auto=none`; it will not create or update production tables. Use an existing CinePass schema. If a new database is being created, apply a reviewed schema migration separately before running the app.

### Frontend

1. Install Node.js 20 or newer.
2. Copy `client/.env.example` to `client/.env` and set `VITE_API_BASE_URL` to the backend base URL.
3. From `client/`, run `npm ci`, then `npm run dev`.
4. For production validation, run `npm run build`.

`VITE_API_BASE_URL` is public frontend configuration, not a secret. Never put database credentials, JWT secrets, TMDB access tokens, or sync keys in `VITE_*` variables.

## Health endpoints

- `GET /api/health/live` — liveness only; does not access MySQL. Use this as Render's health check and for lightweight external monitoring.
- `GET /api/health/ready` — checks whether a pooled database connection is currently usable. It returns `503` if the database is unavailable. Do not use it as a frequent keep-alive ping.
- `GET /health` — retained compatibility endpoint; same database-independent liveness response.

## Render deployment

1. Deploy the `backend/` directory using the included Dockerfile / `render.yaml` blueprint.
2. Configure these Render environment variables:
   - `DB_URL` — full Aiven JDBC URL, including the required SSL parameters from Aiven
   - `DB_USERNAME`
   - `DB_PASSWORD`
   - `JWT_SECRET` — random secret, minimum 32 UTF-8 bytes
   - `FRONTEND_URL` — exact production Vercel origin; comma-separated origins are supported
   - `TMDB_ACCESS_TOKEN` — optional if TMDB features are used
   - `TMDB_SYNC_KEY` — optional but required for the protected sync endpoint
   - `DB_POOL_MAX_SIZE` — optional; default `4`
   - `DB_POOL_MIN_IDLE` — optional; default `1` (one warm connection; lightweight Hikari validation every four minutes)
3. Confirm the Render health check is `/api/health/live`.
4. Do not set `spring.jpa.hibernate.ddl-auto=update` in production. The supplied configuration intentionally uses `none` to avoid modifying the live schema.

## Vercel deployment

1. Set the Vercel project root to `client/`.
2. Set `VITE_API_BASE_URL` to the exact Render backend URL, for example `https://YOUR-RENDER-SERVICE.onrender.com`.
3. Redeploy the frontend after changing the environment variable.
4. Ensure the backend `FRONTEND_URL` includes `https://srh-cinepass.vercel.app` or your actual custom domain.

## Optional 10-minute liveness monitor

Create a scheduled external HTTP GET every 10 minutes:

`https://YOUR-RENDER-SERVICE.onrender.com/api/health/live`

A successful response is a small JSON `200` response and does not touch MySQL. A service such as cron-job.org can send the request on a `*/10 * * * *` schedule. This may reduce idle periods, but it is not a guarantee: Render may still spin down free services, and Aiven may independently pause its free database. Provider policies and free-tier limits take precedence.

## Security and secrets

- Real `.env` files are excluded from the deliverable.
- Use the `.env.example` files as templates only.
- Keep `JWT_SECRET`, database credentials, `TMDB_ACCESS_TOKEN`, and `TMDB_SYNC_KEY` in Render's environment settings, never in frontend variables or Git.
- If any real credential was committed or shared in an archive, rotate it in the provider dashboard.

## Build and tests

Backend: `cd backend && ./mvnw -B clean verify`

Frontend: `cd client && npm ci && npm run build && npm run lint`

See `PERFORMANCE-OPTIMIZATION-REPORT.md` for what was and was not measured. No production latency improvement is claimed without production measurements.
