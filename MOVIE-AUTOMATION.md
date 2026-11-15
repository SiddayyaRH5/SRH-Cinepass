# SRH CinePass — movie automation

This document describes the automatic movie catalogue, the rolling show
window and the movie lifecycle. It is the operational companion to the
[README](README.md).

TMDB is the **only** external movie metadata and release provider. No other
movie API is used anywhere in the project.

---

## 1. What runs automatically

One scheduled job drives everything. It runs **every day at 2:00 AM IST** and
executes these phases in order, under a single distributed lease:

| # | Phase | What it does |
|---|-------|--------------|
| 1 | Lease | Acquire the existing show-generation lease. If another instance holds it, the whole run is skipped and recorded. |
| 2 | Catalogue sync | Read the TMDB regional lists, confirm the Indian theatrical release date of every candidate, and insert or refresh movies. |
| 3 | Lifecycle | Retire movies that are 60 days past their Indian theatrical release date, unless something is still open. |
| 4 | Show generation | Create the missing shows for the rolling seven date window. |
| 5 | Statistics | Record the counters, the outcome and any error in `automation_run`. |
| 6 | Lease | Release the lease. |

The show cleanup job (marking finished shows `EXPIRED`) is a **separate**
scheduled job with its own unchanged schedule (`0 */10 * * * *`, IST) and its
unchanged 45-minute post-start grace period.

No database transaction is ever held open while the TMDB HTTP requests run.
The TMDB calls happen first; every persisted change is then written in a
short transaction of its own. A single failing TMDB record never stops the
rest of the catalogue, and it never prevents the show generation.

---

## 2. Configuration

All properties are backend-only. None of them may be placed in a `VITE_*`
variable.

### TMDB

| Property | Environment variable | Default | Meaning |
|----------|----------------------|---------|---------|
| `tmdb.access.token` | `TMDB_ACCESS_TOKEN` | *(empty)* | TMDB v4 read access token. When empty, the catalogue synchronisation is skipped. |
| `tmdb.sync-key` | `TMDB_SYNC_KEY` | *(empty)* | Shared secret for the legacy `POST /api/tmdb/sync/{movieId}` endpoint. |
| `app.tmdb.base-url` | `TMDB_BASE_URL` | `https://api.themoviedb.org/3` | API base URL. |
| `app.tmdb.region` | `TMDB_REGION` | `IN` | ISO 3166-1 region code. **Must stay `IN`.** |
| `app.tmdb.language` | `TMDB_LANGUAGE` | `en-IN` | Metadata language. |
| `app.tmdb.connect-timeout-ms` | `TMDB_CONNECT_TIMEOUT_MS` | `5000` | Connect timeout. |
| `app.tmdb.read-timeout-ms` | `TMDB_READ_TIMEOUT_MS` | `10000` | Read timeout. |
| `app.tmdb.discovery-max-pages` | `TMDB_DISCOVERY_MAX_PAGES` | `3` | Pages read per discovery list. |
| `app.tmdb.cache.discovery-ttl-minutes` | `TMDB_DISCOVERY_CACHE_MINUTES` | `30` | Discovery list cache TTL. |
| `app.tmdb.cache.details-ttl-minutes` | `TMDB_DETAILS_CACHE_MINUTES` | `360` | Movie details cache TTL. |

### Catalogue synchronisation

| Property | Environment variable | Default | Meaning |
|----------|----------------------|---------|---------|
| `app.movies.sync.enabled` | `MOVIE_SYNC_ENABLED` | `true` | Master switch for the TMDB catalogue sync. |
| `app.movies.sync.backfill-days` | `MOVIE_SYNC_BACKFILL_DAYS` | `90` | How far in the past a newly imported movie may have its Indian theatrical date. |
| `app.movies.sync.lookahead-days` | `MOVIE_SYNC_LOOKAHEAD_DAYS` | `180` | How far in the future a newly imported movie may have its Indian theatrical date. |

### Movie lifecycle

| Property | Environment variable | Default | Meaning |
|----------|----------------------|---------|---------|
| `app.movies.lifecycle.retirement-enabled` | `MOVIE_RETIREMENT_ENABLED` | `true` | Master switch for automatic retirement. |
| `app.movies.lifecycle.retirement-days` | `MOVIE_RETIREMENT_DAYS` | `60` | Days after the Indian theatrical date before a movie is retired. |

### Show scheduler

| Property | Environment variable | Default |
|----------|----------------------|---------|
| `app.shows.scheduler.enabled` | `SHOW_SCHEDULER_ENABLED` | `true` |
| `app.shows.scheduler.cron` | `SHOW_SCHEDULER_CRON` | `0 0 2 * * *` |
| `app.shows.scheduler.horizon-days` | `SHOW_SCHEDULER_HORIZON_DAYS` | `7` |
| `app.shows.scheduler.min-lead-minutes` | `SHOW_SCHEDULER_MIN_LEAD_MINUTES` | `30` |
| `app.shows.scheduler.movies-per-theatre-per-day` | `SHOW_SCHEDULER_MOVIES_PER_THEATRE` | `4` |
| `app.shows.scheduler.slots` | `SHOW_SCHEDULER_SLOTS` | `10:00,13:30,17:00,20:30` |
| `app.shows.scheduler.base-ticket-price` | `SHOW_SCHEDULER_BASE_PRICE` | `180` |
| `app.shows.scheduler.slot-price-step` | `SHOW_SCHEDULER_PRICE_STEP` | `30` |
| `app.shows.scheduler.lease-minutes` | `SHOW_SCHEDULER_LEASE_MINUTES` | `30` |
| `app.shows.booking.close-minutes-before-start` | `SHOW_BOOKING_CLOSE_MINUTES` | `15` |
| `show.cleanup.minutes-after-start` | `SHOW_CLEANUP_MINUTES_AFTER_START` | `45` |

---

## 3. TMDB endpoints and the Indian theatrical date

The endpoints were verified against the official TMDB API reference before
implementation.

| Purpose | Request |
|----------|---------|
| Now playing in India | `GET /3/movie/now_playing?region=IN&language=en-IN&page=N` |
| Upcoming in India | `GET /3/movie/upcoming?region=IN&language=en-IN&page=N` |
| Details plus release dates | `GET /3/movie/{id}?append_to_response=release_dates` |
| Admin search (existing) | `GET /3/search/movie?query=...` |

### How the Indian theatrical release date is decided

The `release_dates` block of a movie looks like this:

```json
{
  "id": 550,
  "results": [
    { "iso_3166_1": "IN",
      "release_dates": [
        { "type": 3, "release_date": "2026-01-10T00:00:00.000Z" }
      ]
    },
    { "iso_3166_1": "US",
      "release_dates": [
        { "type": 3, "release_date": "2026-03-20T00:00:00.000Z" }
      ]
    }
  ]
}
```

The rules are applied strictly:

1. Only the `IN` country block is considered.
2. Only the **theatrical** release types are considered:
   * `2` — Theatrical (limited)
   * `3` — Theatrical
3. `1` (Premiere), `4` (Digital), `5` (Physical) and `6` (TV) are **never**
   used, so a streaming, DVD or television date can never qualify a movie.
4. A type `3` date wins. If only type `2` dates exist, the earliest one is
   used.
5. The worldwide `release_date` of the details payload is **never** used as
   the Indian theatrical date.

A movie with no confirmed Indian theatrical date is **not** imported. A failed
or incomplete TMDB response never creates a placeholder row.

---

## 4. Idempotency and duplicate protection

The TMDB movie id is the stable external identifier and the idempotency key.

* A movie is looked up by `tmdb_id` before anything is written.
* An existing movie is **updated**, never duplicated.
* A manual movie (one an administrator owns) is never overwritten by the
  automatic sync; it is reported as skipped.
* `backend/database/migrations/004_movie_lifecycle_and_automation.sql` adds a
  unique index `uk_movie_tmdb_id`. The index is only added when the table has
  **no** duplicate non-null `tmdb_id` groups. If duplicates exist the
  statement is skipped instead of failing the deployment, and the duplicates
  are reported by the admin API. They are never merged or deleted
  automatically.

To inspect the duplicates yourself:

```sql
SELECT tmdb_id, COUNT(*) AS copies
FROM movie
WHERE tmdb_id IS NOT NULL
GROUP BY tmdb_id
HAVING COUNT(*) > 1;
```

`GET /api/admin/movies/sync-status` reports the duplicate TMDB ids and the
number of rows that carry no TMDB id at all.

### Show uniqueness

The unique business combination is `movie_id + theatre_id + show_time`, where
`show_time` is the **exact** date and time, not only the clock time. It is
protected by three independent layers:

1. an application-level existence check,
2. the database unique index `uk_shows_movie_theatre_time` (migration 003),
3. the cross-instance lease.

A constraint violation is counted as a skip, never as a fatal error. Existing
shows are never regenerated and never deleted.

---

## 5. The seven day rolling window

`app.shows.scheduler.horizon-days` is the **number of calendar dates** in the
window, not the number of days in the future. With the default of `7` the
window covers today and the next six dates — exactly seven dates, never
eight.

The business date is the `Asia/Kolkata` date; the cron expression is
evaluated in that zone regardless of the JVM timezone.

Example:

| Run | Window | Result |
|-----|--------|--------|
| 16 Nov, 02:00 | 16–22 Nov | All seven dates are generated. |
| 17 Nov, 02:00 | 17–23 Nov | 17–22 Nov already exist and are skipped; only 23 Nov is created. |
| 18 Nov, 02:00 | 18–24 Nov | Only the missing combinations on 24 Nov are created. |

Existing business rules are unchanged: minimum lead time, four movies per
theatre per day, the four configured slots, the base price plus the per-slot
step, the one-screen-per-theatre overlap rule, and sales closing 15 minutes
before the show starts.

---

## 6. Movie retirement after 60 days

A movie becomes eligible for retirement when the number of calendar days
between its **Indian theatrical release date** and the business date reaches
`app.movies.lifecycle.retirement-days`.

**Boundary rule:** the age must be **greater than or equal to** the
configured number of days. A movie released 60 days ago is eligible; a movie
released 59 days ago is not. The legacy `release_date` is only used as a
fallback for rows created before the Indian date column existed.

Retirement **never deletes anything**. The movie moves to `ARCHIVED`, which:

* hides it from the customer catalogue and from show discovery,
* stops new shows for it,
* rejects new bookings for it,
* keeps the movie row, its shows, its bookings, its tickets, its seats, its
  payment records and its audit history completely intact.

A movie is **not** retired while any of the following is true. The
postponement and its reason are recorded on the movie:

| Hold | Reason recorded |
|------|-----------------|
| It still has future scheduled shows. | `N future show(s) still scheduled` |
| It has bookings that still have to be fulfilled. | `N booking(s) still open` |
| It has an unresolved booking or payment workflow. | `N unresolved payment workflow(s)` |
| An administrator protected it. | `protected by an administrator` |
| An administrator extended its active period. | `active period extended until YYYY-MM-DD` |

Existing shows are never cancelled merely because a movie reached 60 days.

---

## 7. Administrator API

Every endpoint below is under `/api/admin/**`, which the security
configuration already restricts to the `ADMIN` role. No new authorisation
path was introduced and no endpoint is public.

| Method | Path | Purpose |
|--------|------|---------|
| `GET` | `/api/admin/movies` | Every movie, including the retired ones, with the lifecycle and synchronisation fields. |
| `GET` | `/api/admin/movies/imported` | Only the movies managed by the automatic TMDB sync. |
| `GET` | `/api/admin/movies/sync-status` | Catalogue counters, duplicate TMDB ids, rows without a TMDB id, movies with a sync error and the last run outcome. |
| `POST` | `/api/admin/movies/{id}/exclude` | Include or exclude a movie from automatic show generation. Body: `{"excluded":true,"reason":"..."}`. |
| `POST` | `/api/admin/movies/{id}/protect` | Protect a movie from automatic retirement. Body: `{"protectedMovie":true}`. |
| `POST` | `/api/admin/movies/{id}/extend` | Extend the active period. Body: `{"activeUntil":"2026-12-31"}`. |
| `POST` | `/api/admin/movies/{id}/reactivate` | Bring a retired movie back into the active catalogue. |
| `POST` | `/api/admin/movies/{id}/archive` | Retire a movie manually. |
| `GET` | `/api/admin/automation/runs` | The 50 most recent automation runs. |
| `POST` | `/api/admin/automation/run` | Trigger the daily automation immediately. |

The public catalogue API never exposes the administrator fields. `status` is
the only lifecycle field a customer can see, so the frontend can explain
that a deep-linked movie has been retired.

---

## 8. Database migrations

| File | Contents |
|------|----------|
| `001_create_tickets.sql` | Ticket table. |
| `002_payment_recovery_and_webhook_events.sql` | Payment recovery columns and the webhook event table. |
| `003_show_generation_and_lifecycle.sql` | `shows.status`, the show indexes, the show uniqueness index and the generation lease. |
| `004_movie_lifecycle_and_automation.sql` | The movie lifecycle and provenance columns, the TMDB indexes, the `uk_movie_tmdb_id` unique index and the `automation_run` table. |

All of them are additive and idempotent. No migration drops or truncates a
table, and none of them deletes a movie, show, booking, ticket, seat or
payment row. Every new column is nullable, so pre-existing rows stay readable
and keep their old behaviour:

* a `NULL` `movie.status` behaves as `ACTIVE`,
* a `NULL` `movie.source` behaves as `MANUAL`,
* a `NULL` `movie.exclude_from_auto_shows` and `admin_protected` behave as
  `false`.

The application keeps using Hibernate's additive schema update
(`spring.jpa.hibernate.ddl-auto=update`). The migration files are the
reviewable version of the same change, for managed or manually applied
schemas.

**Never run migration experiments against Aiven.** Use a local MySQL
instance.

---

## 9. Observability

The job logs its start and finish, the lease acquisition and release, the
number of TMDB pages read, the movies discovered, inserted, updated and
skipped, the movies archived and the movies protected from archival, the
shows created, skipped and failed, and the duration of every phase.

It never logs the TMDB access token, the JWT secret, the Razorpay secrets,
the database password or any authorisation token or customer data. The
`automation_run` table stores counters and a short message only.

---

## 10. Local test commands

```bash
# Backend: H2 in MySQL compatibility mode, no real TMDB or Razorpay calls.
cd backend && ./mvnw -B clean test          # Windows: .\mvnw.cmd -B clean test
cd backend && ./mvnw -B -DskipTests package # Windows: .\mvnw.cmd -B -DskipTests package

# Frontend
cd client && npm ci && npm test && npm run lint && npm run build
```

The TMDB tests run against a local stub HTTP server
(`backend/src/test/java/com/srh/cinepass/support/TmdbStubServer.java`). The
real TMDB service is never contacted from a test.

`MySqlInnoDbTransactionTests` needs Docker. Without Docker it is skipped, so
an H2-only result must not be read as proof of the MySQL locking, uniqueness
or migration behaviour.

---

## 11. Render setup

Add these environment variables to the Render service. They are declared in
`render.yaml` with `sync: false`, so Render prompts for them and never stores
them in Git.

| Variable | Required | Purpose |
|----------|----------|---------|
| `TMDB_ACCESS_TOKEN` | For the automatic catalogue | TMDB read access token. |
| `TMDB_SYNC_KEY` | For the legacy manual sync endpoint | Shared secret for `POST /api/tmdb/sync/{movieId}`. |
| `MOVIE_SYNC_ENABLED` | No (`true`) | Master switch for the catalogue sync. |
| `MOVIE_RETIREMENT_ENABLED` | No (`true`) | Master switch for automatic retirement. |
| `MOVIE_RETIREMENT_DAYS` | No (`60`) | Retirement age in days. |
| `SHOW_SCHEDULER_HORIZON_DAYS` | No (`7`) | Size of the rolling window. |

The scheduler runs inside the backend process, so no separate worker or cron
service is needed. The backend must not run more than one instance for the
show grid — the lease and the unique index make duplicates impossible, but a
single instance is simpler to reason about.

---

## 12. Troubleshooting

**No movies are imported.**
Check `GET /api/admin/automation/runs`. A `PARTIAL` outcome with the message
`tmdb access token is not configured` means `TMDB_ACCESS_TOKEN` is missing.
A run with `moviesDiscovered: 0` and no error usually means the Indian
theatrical dates could not be confirmed; the candidates are listed in the
logs with the reason `no Indian theatrical release date`.

**Movies are imported but never get shows.**
A TMDB-managed movie only becomes eligible once its Indian theatrical date is
known and reached. Check `indiaTheatricalReleaseDate` in the admin API. A
movie that is excluded (`excludeFromAutoShows`) or archived is skipped on
purpose.

**The run is recorded as `SKIPPED`.**
Another instance held the lease. The run is retried by the next scheduled
execution. The lease always expires, so a crashed instance cannot block the
job permanently.

**A movie is not retired although it is older than 60 days.**
Check `retirementHoldReason` in the admin API. It names the exact hold:
future shows, open bookings, an unresolved payment workflow, administrator
protection or an extended active period.

**Duplicate TMDB ids are reported.**
They are pre-existing rows that share a TMDB id. Resolve them manually, or
leave them: the unique index was skipped on purpose and the automatic sync
never creates a duplicate. Never delete a movie that has shows, bookings,
tickets or payments — archive it instead.

**The frontend shows a movie that is not in the catalogue.**
A retired movie stays reachable through its direct link so an old bookmark
still explains itself. The page shows a "no longer in theatres" notice and
does not offer the booking flow.
