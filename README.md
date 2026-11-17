# SRH CinePass

Existing React/Vite frontend and Java 21/Spring Boot backend for movie discovery and ticket booking. This archive includes cold-start, API request, connection-pool, caching, and health-check improvements while retaining the existing project structure.

## Project structure

- `client/` — React 19, Vite, Tailwind CSS frontend
- `backend/` — Java 21, Spring Boot, Spring Security, Spring Data JPA backend
- `render.yaml` — Render deployment blueprint
- `MOVIE-AUTOMATION.md` — TMDB catalogue sync, seven-day show window, movie lifecycle and the administrator API
- `PERFORMANCE-OPTIMIZATION-REPORT.md` — findings, changes, limitations, and validation status

## Local setup

### Backend

1. Install JDK 21.
2. Copy `backend/.env.example` to `backend/.env` for your local environment, or export the variables in your shell.
3. Set `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, and a `JWT_SECRET` containing at least 32 UTF-8 bytes. Set TMDB variables if using TMDB integration.
4. For payment testing, set `RAZORPAY_KEY_ID`, `RAZORPAY_KEY_SECRET`, and `RAZORPAY_WEBHOOK_SECRET` to **test-mode** values. Leave `APP_ENV` unset (or `development`) locally; production-only startup validation then stays off.
5. From `backend/`, run `./mvnw spring-boot:run` (Windows: `mvnw.cmd spring-boot:run`).

The current backend configuration uses `spring.jpa.hibernate.ddl-auto=update`, so Hibernate applies additive schema changes (it does not drop existing tables). Reviewable, idempotent migrations are provided at `backend/database/migrations/001_create_tickets.sql`, `002_payment_recovery_and_webhook_events.sql`, `003_show_generation_and_lifecycle.sql` and `004_movie_lifecycle_and_automation.sql` for managed or manually applied schemas. Existing customer data is never deleted by these changes.

### Business time (IST)

Show times, reservation expiry, and ticket admission windows are stored as Indian Standard Time wall-clock values. The backend injects a `Clock` fixed to `Asia/Kolkata` and uses `LocalDateTime.now(clock)` for every comparison. This is intentional: Render runs the JVM in UTC, and using the JVM default would shift a 6:00 PM IST show by 5.5 hours. Existing showtime records are **not** rewritten or reinterpreted.

### Frontend

1. Install Node.js 20 or newer.
2. Copy `client/.env.example` to `client/.env` and set `VITE_API_BASE_URL` to the backend base URL.
3. From `client/`, run `npm ci`, then `npm run dev`.
4. For production validation, run `npm run build`.

`VITE_API_BASE_URL` is public frontend configuration, not a secret. Never put database credentials, JWT secrets, TMDB access tokens, or sync keys in `VITE_*` variables.

## Movie automation (TMDB catalogue, show scheduling, lifecycle)

TMDB is the **only** external movie metadata and release provider. The full
operational reference is in [`MOVIE-AUTOMATION.md`](MOVIE-AUTOMATION.md); the
essentials are:

- **Daily at 2:00 AM IST.** One job runs every calendar day
  (`app.shows.scheduler.cron=0 0 2 * * *`, evaluated in `Asia/Kolkata`). It
  acquires the existing show-generation lease, synchronises the TMDB
  catalogue, evaluates the movie lifecycle, generates the missing shows for
  the rolling window and records the statistics — then releases the lease.
- **Seven-day rolling window.** `app.shows.scheduler.horizon-days=7` is the
  *number of calendar dates*: today plus the next six dates, exactly seven
  dates and never eight. On 16 November the job covers 16–22 November, on 17
  November it covers 17–23 November and only the missing 23 November shows
  are created. The uniqueness key is `movie_id + theatre_id + exact
  show_time`, protected by an application check, a database unique index and
  the cross-instance lease. Existing shows are never regenerated or deleted.
- **Indian theatrical release dates only.** A movie is imported only when
  TMDB confirms an Indian theatrical release date for it, taken from the
  `IN` block of `release_dates` and only from the theatrical release types
  (2 and 3). Digital, streaming, physical, TV and premiere dates are never
  used, and a worldwide date is never treated as the Indian theatrical date.
- **Idempotent catalogue sync.** The TMDB movie id is the idempotency key. An
  existing movie is updated, never duplicated; manually managed movies are
  never overwritten. A unique index on `tmdb_id` is added by migration 004,
  but only when no duplicate non-null `tmdb_id` groups exist. Duplicates and
  rows without a TMDB id are *reported* through the admin API and are never
  merged or deleted automatically.
- **30-day retirement.** A movie is retired once it is 30 calendar days past
  its Indian theatrical release date (the age must reach the configured
  number of days; 30 days exactly is eligible). Retirement never deletes
  anything: the movie row, its shows, bookings, tickets, seats and payment
  records are all preserved. A movie with future shows, open bookings, an
  unresolved payment workflow, administrator protection or an extended
  active period is never retired, and the reason is recorded on the movie.
  An archived movie disappears from the customer catalogue, receives no new
  shows and accepts no new bookings, while its history stays intact.
- **Administrator controls.** Everything lives under `/api/admin/**`, which
  the existing security configuration already restricts to the `ADMIN` role:
  list the imported movies, inspect the synchronisation status and errors,
  exclude a movie from automatic show generation, protect it from
  retirement, extend its active period and reactivate it. The public
  catalogue API never exposes the administrator fields.
- **Transactions.** No database transaction is held open while the TMDB HTTP
  requests run. The external calls happen first and the validated results are
  then persisted in short transactions.

Required environment variables: `TMDB_ACCESS_TOKEN` (for the automatic
catalogue) and `TMDB_SYNC_KEY` (for the legacy protected sync endpoint). The
optional switches `MOVIE_SYNC_ENABLED`, `MOVIE_RETIREMENT_ENABLED`,
`MOVIE_RETIREMENT_DAYS` and `SHOW_SCHEDULER_HORIZON_DAYS` are documented in
`MOVIE-AUTOMATION.md`. The TMDB access token is a backend-only secret; it is
never placed in a `VITE_*` variable and never written to a log line.

## Health endpoints

- `GET /api/health/live` — liveness only; does not access MySQL. Use this as Render's health check and for lightweight external monitoring.
- `GET /api/health/ready` — checks whether a pooled database connection is currently usable. It returns `503` if the database is unavailable. Do not use it as a frequent keep-alive ping.
- `GET /health` — retained compatibility endpoint; same database-independent liveness response.

## Payments and Razorpay webhook

Browser confirmation (`POST /api/bookings/verify-payment`) and server-to-server webhook confirmation (`POST /api/payments/razorpay/webhook`) share the same idempotent `BookingService.confirmPaidBooking` path. A ticket is issued only when the booking is `CONFIRMED` and the payment is `PAID`.

Webhook behaviour:

- The endpoint is public because Razorpay cannot present a customer JWT. It is authenticated by the `X-Razorpay-Signature` header: HMAC SHA-256 over the **exact raw request body** using `RAZORPAY_WEBHOOK_SECRET`, compared in constant time.
- `payment.captured` and `order.paid` are processed. `payment.failed` and unrelated events are acknowledged but never confirm a booking.
- The event's Razorpay order, amount in paise, and currency (`INR`) are checked against the stored booking before confirmation.
- `X-Razorpay-Event-Id` is stored in `razorpay_webhook_events` with a unique constraint. A delivery whose event is already `PROCESSED` returns `DUPLICATE` and never issues a second ticket.
- A delivery whose prior attempt is `FAILED` may retry immediately. A prior `PROCESSING` attempt is only reclaimed after a five-minute cool-off, so an in-flight or crashed delivery cannot race itself. Until a retry is accepted the endpoint returns `503` with status `RETRY`, which tells Razorpay to keep retrying instead of treating an unconfirmed payment as delivered.
- The raw body is capped at 64 KiB before signature verification to protect the public endpoint from memory exhaustion.
- Responses: `200` for processed, duplicate, ignored, or rejected events; `401` for a missing or invalid signature; `400` for a malformed or oversized payload; `503` for a retryable duplicate (`RETRY`); `500` for a transient failure so Razorpay retries.

Razorpay dashboard setup:

1. Open **Dashboard → Settings → Webhooks → Add New Webhook**.
2. URL: `https://YOUR-RENDER-SERVICE.onrender.com/api/payments/razorpay/webhook`.
3. Select at least `payment.captured` and `order.paid` (and optionally `payment.failed`).
4. Generate a webhook secret and copy it into Render as `RAZORPAY_WEBHOOK_SECRET` (and into `backend/.env` locally). Never commit it.
5. Enable the webhook only after the backend is deployed with that secret.

Local webhook testing:

1. Expose the local backend with a tunnel such as `ngrok http 8080`.
2. Use the tunnel URL in a Razorpay **test-mode** webhook, or send a signed request manually:

   ```bash
   BODY='{"entity":"event","event":"payment.captured","payload":{"payment":{"entity":{"id":"pay_test","amount":50000,"currency":"INR","status":"captured","order_id":"order_test"}}}}'
   SIGNATURE=$(printf '%s' "$BODY" | openssl dgst -sha256 -hmac "$RAZORPAY_WEBHOOK_SECRET" -hex | sed 's/^.* //')
   curl -X POST http://localhost:8080/api/payments/razorpay/webhook \
     -H "Content-Type: application/json" \
     -H "X-Razorpay-Signature: $SIGNATURE" \
     -H "X-Razorpay-Event-Id: evt_local_1" \
     --data "$BODY"
   ```

   The order and amount must match a real pending booking created through `/api/bookings/create-order`.

### Captured payment after reservation expiry

The seat reservation lasts 10 minutes. If a genuine captured payment arrives after that window:

- The booking and its seats are locked, and availability is checked first.
- If the original seats are still free, the booking is confirmed and marked `paymentRecoveryStatus=RECOVERED`.
- If another customer has bought or is actively holding those seats, the seats are **not** stolen. The booking becomes `PAYMENT_REVIEW` with `paymentStatus=PAID` and `paymentRecoveryStatus=MANUAL_REVIEW`; no ticket is issued.
- Administrators can see the queue at `GET /api/admin/payments/recovery`.
- No automatic refund is implemented. The captured payment is preserved and must be refunded or reassigned manually in the Razorpay dashboard after the administrator reviews the booking.

## Render deployment

1. Deploy the `backend/` directory using the included Dockerfile / `render.yaml` blueprint.
2. Configure these Render environment variables:
   - `DB_URL` — full Aiven JDBC URL, including the required SSL parameters from Aiven
   - `DB_USERNAME`
   - `DB_PASSWORD`
   - `JWT_SECRET` — random secret, minimum 32 UTF-8 bytes
   - `FRONTEND_URL` — exact production Vercel origin; comma-separated origins are supported
   - `APP_ENV` — set to `production`. Production startup then fails fast if a required Razorpay value is missing.
   - `RAZORPAY_KEY_ID` — live Razorpay key ID (public, but still server-side configuration)
   - `RAZORPAY_KEY_SECRET` — live Razorpay key secret; never expose it to the frontend
   - `RAZORPAY_WEBHOOK_SECRET` — the secret shown by Razorpay when the webhook is created
   - `TMDB_ACCESS_TOKEN` — TMDB read access token, required for the automatic movie catalogue
   - `TMDB_SYNC_KEY` — optional but required for the protected sync endpoint
   - `MOVIE_SYNC_ENABLED` — optional; default `true`, master switch for the TMDB catalogue sync
   - `MOVIE_RETIREMENT_ENABLED` — optional; default `true`, master switch for automatic movie retirement
   - `MOVIE_RETIREMENT_DAYS` - optional; default `30`, days after the Indian theatrical date
   - `SHOW_SCHEDULER_HORIZON_DAYS` — optional; default `7`, calendar dates in the rolling window
   - `DB_POOL_MAX_SIZE` — optional; default `4`
   - `DB_POOL_MIN_IDLE` — optional; default `1` (one warm connection; lightweight Hikari validation every four minutes)
3. `render.yaml` declares `RAZORPAY_KEY_ID`, `RAZORPAY_KEY_SECRET`, `RAZORPAY_WEBHOOK_SECRET`, `TMDB_ACCESS_TOKEN` and `TMDB_SYNC_KEY` with `sync: false`, so Render prompts for them and never stores them in Git. The same applies to the database and JWT values.
4. Confirm the Render health check is `/api/health/live`.
5. Do not override the deployed schema strategy without a reviewed migration. The committed `application.properties` currently uses `ddl-auto=update` for additive changes; `backend/database/migrations/001_create_tickets.sql`, `002_payment_recovery_and_webhook_events.sql`, `003_show_generation_and_lifecycle.sql` and `004_movie_lifecycle_and_automation.sql` are the reviewable migrations for environments where DDL is applied manually.

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
- Keep `JWT_SECRET`, database credentials, `RAZORPAY_KEY_SECRET`, `RAZORPAY_WEBHOOK_SECRET`, `TMDB_ACCESS_TOKEN`, and `TMDB_SYNC_KEY` in Render's environment settings, never in frontend variables or Git. `RAZORPAY_KEY_ID` is public, but it is still supplied as server-side configuration.
- Production startup (`APP_ENV=production`) refuses to start when `RAZORPAY_KEY_ID`, `RAZORPAY_KEY_SECRET`, or `RAZORPAY_WEBHOOK_SECRET` is missing. The error names the variables and never prints their values.
- If any real credential was committed or shared in an archive, rotate it in the provider dashboard.

## QR tickets and staff verification

The digital ticket and scanner feature is documented in `TICKETS.md`. In short:

- A ticket is issued only inside the existing paid Razorpay confirmation transaction.
- The QR code contains only an opaque 256-bit SecureRandom token.
- Customers open `/My-Bookings` → **View Ticket** to view or print their ticket.
- Administrators grant scanner access at `/admin/staff`; there is no public staff signup.
- Staff open `/staff/scanner` to scan or manually verify a ticket. The backend atomically marks a ticket `USED` and only then returns a green result.

## Build and tests

Backend: `cd backend && ./mvnw -B test` (integration tests use an in-memory H2 database; no MySQL or real Razorpay credentials are required). The suite covers security rules, IST timezone boundaries with a UTC JVM, Razorpay webhook signature/idempotency/amount checks, captured-payment recovery, resold seats, the browser/webhook confirmation race, the TMDB client against a local stub server, the idempotent catalogue synchronisation, the 60-day movie retirement and the seven-day rolling show window.

`MySqlInnoDbTransactionTests` requires Docker. Without Docker it is skipped, so an H2-only result must not be described as proof of the MySQL locking, uniqueness or migration behaviour.

Backend production build: `cd backend && ./mvnw -B package`

Frontend: `cd client && npm ci && npm test && npm run build && npm run lint`

See `PERFORMANCE-OPTIMIZATION-REPORT.md` for what was and was not measured. No production latency improvement is claimed without production measurements.
