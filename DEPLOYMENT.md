# SRH-CinePass deployment repair notes

## What was confirmed from the supplied ZIP

- The backend is a Java 21 Spring Boot application built with Maven and MySQL.
- The frontend is a React 19 + Vite application.
- The backend reads `DB_URL`, `DB_USERNAME`, and `DB_PASSWORD`; the Render process cannot connect to the database unless all three are configured correctly.
- The JWT signing key was hard-coded in source. It has been moved to `JWT_SECRET`; configure a new random secret of at least 32 UTF-8 bytes. Existing JWTs signed with the old source-code key will no longer work; users must sign in again.
- CORS was hard-coded to local development and a single production origin. It now reads `FRONTEND_URL` (comma-separated origins are supported) and retains localhost origins for development.
- The existing JWT filter was a Spring component but was not attached to the Spring Security filter chain. It is now registered so controllers can receive the authenticated principal.
- Added `/health` for Render's process health check. It is a liveness check and does not prove that MySQL is reachable.
- Added root and frontend ignore rules for `.env` files and generated directories. The supplied frontend `.env` file is intentionally not included in this repaired archive; use Vercel environment settings and `client/.env.example` for local setup.

## Render configuration

The included `render.yaml` describes a Docker web service with `rootDir: backend`, `Dockerfile: ./Dockerfile`, and health check `/health`.

Set these environment variables in Render:

| Key | Required value |
|---|---|
| `DB_URL` | Full JDBC URL, e.g. `jdbc:mysql://HOST:PORT/DATABASE?sslMode=REQUIRED` (replace placeholders with the Aiven values) |
| `DB_USERNAME` | Aiven database username |
| `DB_PASSWORD` | Aiven database password |
| `JWT_SECRET` | A newly generated random secret, minimum 32 UTF-8 bytes |
| `FRONTEND_URL` | `https://srh-cinepass.vercel.app` (or your exact Vercel custom domain) |

Do not add quotes around environment values in the Render dashboard. Do not put credentials in source files or commit `.env` files.

If configuring the existing Render service manually instead of using the Blueprint:

- Runtime: Docker
- Root Directory: `backend`
- Dockerfile Path: `./Dockerfile`
- Health Check Path: `/health`
- The Dockerfile builds with `./mvnw -B clean package -DskipTests` and runs the packaged Spring Boot JAR.
- Render supplies `PORT`; the app listens on `${PORT:8080}`.

## Vercel frontend

Set the Vercel project environment variable:

`VITE_API_BASE_URL=https://srh-cinepass.onrender.com`

Use the actual Render service hostname if it differs. Redeploy the frontend after changing this value. `VITE_API_BASE_URL` is public configuration, not a secret.

## Tests and limitations in this repair run

- ZIP extraction and source/configuration inspection: completed.
- Frontend production build: not verified. The included `node_modules` snapshot was incomplete (the Vite executable was missing in the extracted copy; the nested dependency snapshot also lacked Rollup's Linux optional native package). Installing dependencies timed out in this environment, so no successful build can be claimed.
- Backend Maven build: not verified. The Maven wrapper could not download its Maven distribution in this environment, and no system Maven executable was available.
- Database connectivity: not tested; Aiven credentials are not available in the source archive.
- Render runtime logs: not included in the email or ZIP. Therefore the exact cause of the reported `Exited with status 1` cannot be proven from the supplied evidence. The Render logs around the crash are still required to distinguish a missing/incorrect DB setting from a code or runtime exception.

The changes above address confirmed configuration and authentication integration problems found in the source. They are not a claim that a successful live Render deployment has been verified.
