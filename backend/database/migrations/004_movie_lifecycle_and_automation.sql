-- ============================================================
-- SRH CinePass movie lifecycle, TMDB catalogue and automation runs
--
-- Additive and idempotent. It never drops or truncates a table,
-- and it never deletes a movie, show, booking, ticket, seat or
-- payment row. Every new column is nullable, so pre-existing rows
-- stay readable and keep working with their old values.
--
-- The application still uses Hibernate's additive schema update
-- (spring.jpa.hibernate.ddl-auto=update). This file exists for
-- review, for fresh managed databases, and for environments where
-- DDL is applied manually before deployment.
-- ============================================================

SET @dbname = DATABASE();
SET @tablename = 'movie';

-- ------------------------------------------------------------
-- movie.status
--
-- Lifecycle state of a movie: ACTIVE or ARCHIVED. NULL means ACTIVE
-- for every row that existed before this feature, so the customer
-- catalogue and the scheduler behave exactly as before.
-- ------------------------------------------------------------

SET @columnname = 'status';

SET @prepared_statement = (
    SELECT IF(
        (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
         WHERE TABLE_SCHEMA = @dbname
           AND TABLE_NAME = @tablename
           AND COLUMN_NAME = @columnname) > 0,
        'SELECT 1',
        'ALTER TABLE movie ADD COLUMN status VARCHAR(20) NULL'
    )
);

PREPARE alter_movie FROM @prepared_statement;
EXECUTE alter_movie;
DEALLOCATE PREPARE alter_movie;

-- ------------------------------------------------------------
-- movie.source
--
-- How a movie row is managed: MANUAL (administrator owned, never
-- overwritten by the automatic sync) or TMDB (managed by the
-- catalogue synchronisation). NULL means MANUAL for legacy rows.
-- ------------------------------------------------------------

SET @columnname = 'source';

SET @prepared_statement = (
    SELECT IF(
        (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
         WHERE TABLE_SCHEMA = @dbname
           AND TABLE_NAME = @tablename
           AND COLUMN_NAME = @columnname) > 0,
        'SELECT 1',
        'ALTER TABLE movie ADD COLUMN source VARCHAR(20) NULL'
    )
);

PREPARE alter_movie FROM @prepared_statement;
EXECUTE alter_movie;
DEALLOCATE PREPARE alter_movie;

-- ------------------------------------------------------------
-- movie.india_theheatrical_release_date
--
-- The confirmed Indian theatrical release date. It is the reference
-- for show eligibility and for the automatic retirement. It is
-- never a worldwide, digital, streaming, physical or TV date.
-- ------------------------------------------------------------

SET @columnname = 'india_theatrical_release_date';

SET @prepared_statement = (
    SELECT IF(
        (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
         WHERE TABLE_SCHEMA = @dbname
           AND TABLE_NAME = @tablename
           AND COLUMN_NAME = @columnname) > 0,
        'SELECT 1',
        'ALTER TABLE movie ADD COLUMN india_theatrical_release_date DATE NULL'
    )
);

PREPARE alter_movie FROM @prepared_statement;
EXECUTE alter_movie;
DEALLOCATE PREPARE alter_movie;

-- ------------------------------------------------------------
-- movie.exclude_from_auto_shows and movie.exclusion_reason
--
-- Administrator override: the scheduler must not create new shows
-- for an excluded movie. The movie stays in the customer catalogue.
-- ------------------------------------------------------------

SET @columnname = 'exclude_from_auto_shows';

SET @prepared_statement = (
    SELECT IF(
        (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
         WHERE TABLE_SCHEMA = @dbname
           AND TABLE_NAME = @tablename
           AND COLUMN_NAME = @columnname) > 0,
        'SELECT 1',
        'ALTER TABLE movie ADD COLUMN exclude_from_auto_shows BIT NULL'
    )
);

PREPARE alter_movie FROM @prepared_statement;
EXECUTE alter_movie;
DEALLOCATE PREPARE alter_movie;

SET @columnname = 'exclusion_reason';

SET @prepared_statement = (
    SELECT IF(
        (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
         WHERE TABLE_SCHEMA = @dbname
           AND TABLE_NAME = @tablename
           AND COLUMN_NAME = @columnname) > 0,
        'SELECT 1',
        'ALTER TABLE movie ADD COLUMN exclusion_reason VARCHAR(500) NULL'
    )
);

PREPARE alter_movie FROM @prepared_statement;
EXECUTE alter_movie;
DEALLOCATE PREPARE alter_movie;

-- ------------------------------------------------------------
-- movie.admin_protected
--
-- When set, the automatic retirement job never archives the movie.
-- ------------------------------------------------------------

SET @columnname = 'admin_protected';

SET @prepared_statement = (
    SELECT IF(
        (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
         WHERE TABLE_SCHEMA = @dbname
           AND TABLE_NAME = @tablename
           AND COLUMN_NAME = @columnname) > 0,
        'SELECT 1',
        'ALTER TABLE movie ADD COLUMN admin_protected BIT NULL'
    )
);

PREPARE alter_movie FROM @prepared_statement;
EXECUTE alter_movie;
DEALLOCATE PREPARE alter_movie;

-- ------------------------------------------------------------
-- movie.active_until
--
-- Administrator managed end of the active period. While the business
-- date is on or before this date the movie is not retired.
-- ------------------------------------------------------------

SET @columnname = 'active_until';

SET @prepared_statement = (
    SELECT IF(
        (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
         WHERE TABLE_SCHEMA = @dbname
           AND TABLE_NAME = @tablename
           AND COLUMN_NAME = @columnname) > 0,
        'SELECT 1',
        'ALTER TABLE movie ADD COLUMN active_until DATE NULL'
    )
);

PREPARE alter_movie FROM @prepared_statement;
EXECUTE alter_movie;
DEALLOCATE PREPARE alter_movie;

-- ------------------------------------------------------------
-- movie.retirement_hold_reason
--
-- Why the automatic retirement was postponed. Cleared once the movie
-- is actually archived.
-- ------------------------------------------------------------

SET @columnname = 'retirement_hold_reason';

SET @prepared_statement = (
    SELECT IF(
        (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
         WHERE TABLE_SCHEMA = @dbname
           AND TABLE_NAME = @tablename
           AND COLUMN_NAME = @columnname) > 0,
        'SELECT 1',
        'ALTER TABLE movie ADD COLUMN retirement_hold_reason VARCHAR(500) NULL'
    )
);

PREPARE alter_movie FROM @prepared_statement;
EXECUTE alter_movie;
DEALLOCATE PREPARE alter_movie;

-- ------------------------------------------------------------
-- movie.archived_at
--
-- When the movie was retired. The row itself is never deleted.
-- ------------------------------------------------------------

SET @columnname = 'archived_at';

SET @prepared_statement = (
    SELECT IF(
        (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
         WHERE TABLE_SCHEMA = @dbname
           AND TABLE_NAME = @tablename
           AND COLUMN_NAME = @columnname) > 0,
        'SELECT 1',
        'ALTER TABLE movie ADD COLUMN archived_at DATETIME(6) NULL'
    )
);

PREPARE alter_movie FROM @prepared_statement;
EXECUTE alter_movie;
DEALLOCATE PREPARE alter_movie;

-- ------------------------------------------------------------
-- movie.tmdb_synced_at and movie.last_sync_error
--
-- Synchronisation audit information for the administrator API. The
-- error text never contains a credential.
-- ------------------------------------------------------------

SET @columnname = 'tmdb_synced_at';

SET @prepared_statement = (
    SELECT IF(
        (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
         WHERE TABLE_SCHEMA = @dbname
           AND TABLE_NAME = @tablename
           AND COLUMN_NAME = @columnname) > 0,
        'SELECT 1',
        'ALTER TABLE movie ADD COLUMN tmdb_synced_at DATETIME(6) NULL'
    )
);

PREPARE alter_movie FROM @prepared_statement;
EXECUTE alter_movie;
DEALLOCATE PREPARE alter_movie;

SET @columnname = 'last_sync_error';

SET @prepared_statement = (
    SELECT IF(
        (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
         WHERE TABLE_SCHEMA = @dbname
           AND TABLE_NAME = @tablename
           AND COLUMN_NAME = @columnname) > 0,
        'SELECT 1',
        'ALTER TABLE movie ADD COLUMN last_sync_error VARCHAR(1000) NULL'
    )
);

PREPARE alter_movie FROM @prepared_statement;
EXECUTE alter_movie;
DEALLOCATE PREPARE alter_movie;

-- ------------------------------------------------------------
-- Lookup indexes for the catalogue queries.
-- ------------------------------------------------------------

SET @indexname = 'idx_movie_tmdb_id';

SET @prepared_statement = (
    SELECT IF(
        (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS
         WHERE TABLE_SCHEMA = @dbname
           AND TABLE_NAME = @tablename
           AND INDEX_NAME = @indexname) > 0,
        'SELECT 1',
        'CREATE INDEX idx_movie_tmdb_id ON movie (tmdb_id)'
    )
);

PREPARE alter_movie FROM @prepared_statement;
EXECUTE alter_movie;
DEALLOCATE PREPARE alter_movie;

SET @indexname = 'idx_movie_status';

SET @prepared_statement = (
    SELECT IF(
        (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS
         WHERE TABLE_SCHEMA = @dbname
           AND TABLE_NAME = @tablename
           AND INDEX_NAME = @indexname) > 0,
        'SELECT 1',
        'CREATE INDEX idx_movie_status ON movie (status)'
    )
);

PREPARE alter_movie FROM @prepared_statement;
EXECUTE alter_movie;
DEALLOCATE PREPARE alter_movie;

SET @indexname = 'idx_movie_source';

SET @prepared_statement = (
    SELECT IF(
        (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS
         WHERE TABLE_SCHEMA = @dbname
           AND TABLE_NAME = @tablename
           AND INDEX_NAME = @indexname) > 0,
        'SELECT 1',
        'CREATE INDEX idx_movie_source ON movie (source)'
    )
);

PREPARE alter_movie FROM @prepared_statement;
EXECUTE alter_movie;
DEALLOCATE PREPARE alter_movie;

-- ------------------------------------------------------------
-- Uniqueness: one row per TMDB movie id.
--
-- This deliberately allows many rows with a NULL tmdb_id, because
-- every manually created movie has no TMDB id. MySQL treats NULLs
-- as distinct in a unique index, so the manual movies are unaffected.
--
-- The index is added only when the table currently has no duplicate
-- non-null tmdb_id groups. If duplicates exist the statement is
-- skipped instead of failing the deployment. Inspect them first:
--
--   SELECT tmdb_id, COUNT(*) AS copies
--   FROM movie
--   WHERE tmdb_id IS NOT NULL
--   GROUP BY tmdb_id
--   HAVING COUNT(*) > 1;
--
-- Never resolve duplicates by deleting a movie that has shows,
-- bookings, tickets or payments. Archive it instead.
-- ------------------------------------------------------------

SET @duplicate_groups = (
    SELECT COUNT(*) FROM (
        SELECT tmdb_id
        FROM movie
        WHERE tmdb_id IS NOT NULL
        GROUP BY tmdb_id
        HAVING COUNT(*) > 1
    ) AS duplicate_movies
);

SET @indexname = 'uk_movie_tmdb_id';

SET @index_exists = (
    SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS
    WHERE TABLE_SCHEMA = @dbname
      AND TABLE_NAME = @tablename
      AND INDEX_NAME = @indexname
);

SET @prepared_statement = IF(
    @duplicate_groups = 0 AND @index_exists = 0,
    'ALTER TABLE movie ADD UNIQUE INDEX uk_movie_tmdb_id (tmdb_id)',
    'SELECT 1'
);

PREPARE alter_movie FROM @prepared_statement;
EXECUTE alter_movie;
DEALLOCATE PREPARE alter_movie;

-- ------------------------------------------------------------
-- automation_run
--
-- Append-only audit rows for the daily automation. They record what
-- the scheduler did, so an administrator can confirm that the job
-- ran and see its counters. No credential or customer data is stored.
-- ------------------------------------------------------------

CREATE TABLE IF NOT EXISTS automation_run (
    id BIGINT NOT NULL AUTO_INCREMENT,

    trigger_type VARCHAR(20) NOT NULL,

    started_at DATETIME(6) NOT NULL,

    finished_at DATETIME(6) NULL,

    outcome VARCHAR(20) NOT NULL,

    pages_processed INT NULL,
    movies_discovered INT NULL,
    movies_inserted INT NULL,
    movies_updated INT NULL,
    movies_skipped INT NULL,
    movies_archived INT NULL,
    movies_protected INT NULL,
    shows_created INT NULL,
    shows_skipped INT NULL,
    shows_failed INT NULL,

    message VARCHAR(1000) NULL,

    PRIMARY KEY (id)
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_unicode_ci;

SET @indexname = 'idx_automation_run_started_at';

SET @prepared_statement = (
    SELECT IF(
        (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS
         WHERE TABLE_SCHEMA = @dbname
           AND TABLE_NAME = 'automation_run'
           AND INDEX_NAME = @indexname) > 0,
        'SELECT 1',
        'CREATE INDEX idx_automation_run_started_at ON automation_run (started_at)'
    )
);

PREPARE alter_automation_run FROM @prepared_statement;
EXECUTE alter_automation_run;
DEALLOCATE PREPARE alter_automation_run;
