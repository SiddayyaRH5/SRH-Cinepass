# SRH CinePass — TMDB synchronization

## What is included

- `Movie.tmdbId` field (Hibernate `ddl-auto=update` adds the nullable `tmdb_id` column).
- `MovieRepository.findByTmdbId(...)` lookup.
- Existing `TmdbService` search/details/image URL integration.
- `TmdbSyncService` updates an existing movie row by its local database ID after checking the returned TMDB ID.
- `TmdbSyncController` exposes a sync endpoint protected by a server-side `TMDB_SYNC_KEY`.
- CORS allows the `X-SYNC-KEY` header.

## Configure secrets

Set these as environment variables locally and in Render. Do not commit real values:

- `TMDB_ACCESS_TOKEN`: TMDB API Read Access Token (v4).
- `TMDB_SYNC_KEY`: a separate long random secret used only for the sync endpoint.

The backend also requires its existing database and JWT environment variables (`DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, `JWT_SECRET`).

PowerShell: generate a random sync key and copy it without printing it:

```powershell
$bytes = New-Object byte[] 32
$rng = [System.Security.Cryptography.RandomNumberGenerator]::Create()
$rng.GetBytes($bytes)
$env:TMDB_SYNC_KEY = [Convert]::ToBase64String($bytes)
$rng.Dispose()
Set-Clipboard -Value $env:TMDB_SYNC_KEY
```

Add the same value as `TMDB_SYNC_KEY` in Render's backend Environment settings. Keep it private.

## Postman request

Use **POST**, not GET:

```text
POST http://localhost:8080/api/tmdb/sync/{LOCAL_MOVIE_ID}?tmdbId={VERIFIED_TMDB_ID}
```

Example only (the local movie ID `12` must actually exist):

```text
POST http://localhost:8080/api/tmdb/sync/12?tmdbId=157336
```

Headers:

```text
X-SYNC-KEY: <your TMDB_SYNC_KEY value>
```

Body: none.

A `405 Method Not Allowed` response means the request was sent with GET. Change the Postman method selector to POST. A `403` means the header is missing or does not match. A `503` means `TMDB_SYNC_KEY` is not configured on the backend.

## Build and run (Windows PowerShell)

From the `backend` folder:

```powershell
.\mvnw.cmd clean test
.\mvnw.cmd spring-boot:run
```

The database schema update is non-destructive to existing rows; verify the new column with:

```sql
SHOW COLUMNS FROM movie LIKE 'tmdb_id';
```

Do not run sync against a local movie until you have confirmed its local database ID and the matching TMDB ID/title.
