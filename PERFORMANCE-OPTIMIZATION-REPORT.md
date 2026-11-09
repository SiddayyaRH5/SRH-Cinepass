# SRH CinePass — Performance and cold-start optimization report

## Scope

This report describes changes made to the supplied source archive. It does not claim a measured improvement on the live Render/Aiven services: production credentials and access to the deployed service were not supplied, and the application was not load-tested against production data.

## Findings from the supplied source

1. The frontend API helper attached a stored bearer token to every request, including public catalogue reads and login/signup. The JWT filter then attempted a user-table lookup whenever a bearer token was present. That created avoidable database work on public requests and on authentication endpoints.
2. HikariCP had a 20-second connection timeout but no explicit small-instance pool size. Its prior initialization-failure setting did not clearly express the intended start-without-database behavior.
3. Hibernate was configured with `ddl-auto=update`, which can perform schema work at startup and is not appropriate for a production performance patch that must preserve existing records and schema.
4. The existing `/health` endpoint was liveness-only and did not provide a separate database readiness check.
5. The frontend home page waited for both movies and shows before rendering the page content.
6. The existing security chain ended with `anyRequest().permitAll()`. This allowed anonymous requests to reach routes that assume an authenticated principal. Access rules have been tightened around existing public reads, authenticated user routes, and administrator operations.
7. The supplied archive included a nested `client.zip` containing `node_modules` and a `.env` file. That nested archive is excluded from the output ZIP.

## Implemented changes

### Backend

- Added `GET /api/health/live` (and retained `/health`) with no database dependency.
- Added `GET /api/health/ready`, which borrows a pooled connection and uses JDBC `isValid(2)`; it returns only generic status information and does not expose connection details.
- Set Hikari defaults to a small pool (`maximumPoolSize=4`, `minimumIdle=1`), bounded connection/validation timeouts, finite max lifetime, a four-minute keepalive validation on one idle connection, and start-without-failing-on-database-unavailable behavior (`initialization-fail-timeout=-1`). Pool sizes/timeouts can be overridden with Render environment variables.
- Set Hibernate schema management to `none`, selected the MySQL dialect explicitly, disabled open-session-in-view, and enabled conservative JDBC batching. This avoids automatic DDL changes at deployment.
- Added bounded Caffeine caches for public movie and location lists with three-minute expiry and cache eviction on writes.
- Added fetch graphs for show, booking, and seat reads to fetch their existing to-one associations and booking seat collection as part of the repository query, reducing common serialization-time N+1 patterns while retaining response entities.
- Marked signup/login transaction boundaries explicitly.
- Changed auth API failure responses so database outages return a generic `503`; duplicate email conflicts return `409`; login failures do not expose internal details.
- Skipped JWT database lookups for login/signup, health, and known public GET catalogue endpoints.
- Tightened Spring Security from blanket anonymous access to public catalogue reads, authenticated user routes, and administrator-only catalogue writes, booking administration, and theatre-verification decisions.
- Updated Render's health check path to `/api/health/live` and added a backend `.dockerignore`.

### Frontend

- Centralized API handling with a 90-second configurable request timeout, generic network errors, in-flight GET deduplication, and caller-provided AbortSignal cancellation.
- Avoided attaching a previously stored JWT to login/signup requests.
- Did not add automatic retries for POST requests. A signup timeout explicitly warns that the account may have been created and recommends checking by logging in before attempting signup again.
- Added a synchronous submit lock to normal login/signup forms to prevent duplicate submissions.
- Removed login/signup response/error console logging that could expose user data.
- Allowed the home page to render after movie data resolves instead of waiting for the secondary shows request.

## What this does not solve

- Render Free may spin down an idle web service. The first request can still wait for the service to start. A lightweight scheduled GET to `/api/health/live` may reduce idle periods but is not a guarantee and may not override provider free-tier policies.
- Aiven Free may independently pause or power off its database. `/api/health/live` deliberately does not wake or keep the database alive. `/api/health/ready` will report `503` while the database is unavailable; the first real database request can still incur a connection/wake delay.
- A database that is paused by the provider cannot be made continuously available by HikariCP. If predictable first-request latency is required, use always-on paid application/database tiers or a hosting plan with explicit availability guarantees.
- Signup/login POSTs are never automatically retried because the client cannot know whether the server committed the operation after a connection timeout.

## Performance validation status

| Scenario | Validation in this workspace |
|---|---|
| First website visit | Not measured against production; frontend production build could not complete in this offline workspace |
| First public API request | Not measured against deployed Render service |
| First login after inactivity | Not measured; requires a valid production test account and deployed service |
| First signup request | Not measured; requires permission to create a production test account |
| Dashboard loading | Static frontend build and source review only |
| Database reconnection after interruption | Not run against Aiven; requires database test environment |
| Subsequent API requests | Not measured against deployed service |
| Concurrent login requests | Not load-tested; no production credentials or dedicated test database supplied |
| Backend startup / connection / SQL / auth timings | No production telemetry was available; no numeric improvement is claimed |

## Database index review

No index DDL was applied because the live Aiven schema and existing index list were not available. Before adding indexes, inspect `INFORMATION_SCHEMA.STATISTICS` and use `EXPLAIN` against real query patterns. Candidate columns to review (add only when an equivalent index is absent) are `shows(movie_id, show_date)`, `shows(theatre_id, show_date)`, `shows(movie_id, theatre_id, show_date, show_time)`, `bookings(user_id)`, `bookings(show_id)`, `seats(show_id)`, and `theatres(location_id)`. The unique email constraint on users already exists in the supplied entity mapping.

## Local verification constraints

- `pom.xml` parses as valid XML. The Maven wrapper could not download Maven 3.9.16 because this environment could not reach Maven Central, so backend compilation and Maven tests were not completed here.
- Frontend ESLint completed successfully using the dependency tree bundled in the supplied nested archive (the nested archive itself is not included in the deliverable).
- Frontend production build was attempted but could not run with that bundled dependency tree because it lacks the Linux optional package `@rollup/rollup-linux-x64-gnu`. A fresh `npm ci` using the included lockfile on a network-enabled Linux/CI runner should install the platform-specific optional package before `npm run build`. Offline `npm ci` was also unavailable because the package tarballs were not cached.
- Production authentication, database reconnection, concurrent login, and latency measurements remain unverified and are not represented as passing.

## Recommended production measurement

Use Render logs and Aiven metrics, and measure these separately with timestamps: process start to `/api/health/live` 200; `/api/health/ready` 200; first `GET /api/movies`; first login; first signup in a disposable test environment; and the second identical public GET. Record p50/p95 over repeated cold and warm trials. Do not use real user passwords or tokens in test logs.
