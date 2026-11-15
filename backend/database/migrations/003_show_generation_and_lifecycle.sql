-- ============================================================
-- SRH CinePass automatic show generation + lifecycle
--
-- Additive and idempotent. It never drops or truncates a table,
-- and it never deletes a show, booking, ticket, seat or payment.
--
-- The application still uses Hibernate's additive schema update
-- (spring.jpa.hibernate.ddl-auto=update). This file exists for
-- review, for fresh managed databases, and for environments where
-- DDL is applied manually before deployment.
-- ============================================================

SET @dbname = DATABASE();

-- ------------------------------------------------------------
-- shows.status
--
-- Nullable for backward compatibility: pre-existing rows stay
-- NULL and are treated as SCHEDULED by the application. The
-- cleanup job only moves SCHEDULED/NULL rows to EXPIRED.
-- ------------------------------------------------------------

SET @tablename = 'shows';
SET @columnname = 'status';

SET @prepared_statement = (
    SELECT IF(
        (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
         WHERE TABLE_SCHEMA = @dbname
           AND TABLE_NAME = @tablename
           AND COLUMN_NAME = @columnname) > 0,
        'SELECT 1',
        'ALTER TABLE shows ADD COLUMN status VARCHAR(20) NULL'
    )
);

PREPARE alter_shows FROM @prepared_statement;
EXECUTE alter_shows;
DEALLOCATE PREPARE alter_shows;

-- ------------------------------------------------------------
-- Lookup index for date-range and cleanup queries.
-- ------------------------------------------------------------

SET @indexname = 'idx_shows_show_time';

SET @prepared_statement = (
    SELECT IF(
        (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS
         WHERE TABLE_SCHEMA = @dbname
           AND TABLE_NAME = @tablename
           AND INDEX_NAME = @indexname) > 0,
        'SELECT 1',
        'CREATE INDEX idx_shows_show_time ON shows (show_time)'
    )
);

PREPARE alter_shows FROM @prepared_statement;
EXECUTE alter_shows;
DEALLOCATE PREPARE alter_shows;

SET @indexname = 'idx_shows_theatre_show_time';

SET @prepared_statement = (
    SELECT IF(
        (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS
         WHERE TABLE_SCHEMA = @dbname
           AND TABLE_NAME = @tablename
           AND INDEX_NAME = @indexname) > 0,
        'SELECT 1',
        'CREATE INDEX idx_shows_theatre_show_time ON shows (theater_id, show_time)'
    )
);

PREPARE alter_shows FROM @prepared_statement;
EXECUTE alter_shows;
DEALLOCATE PREPARE alter_shows;

-- ------------------------------------------------------------
-- Uniqueness: movie + theatre + EXACT showtime.
--
-- This deliberately does NOT include the calendar date alone, so a
-- theatre may run the same movie several times per day.
--
-- The index is added only when the table currently has no duplicate
-- (movie_id, theater_id, show_time) groups. If duplicates exist the
-- statement is skipped instead of failing the deployment. Inspect
-- and resolve them manually first:
--
--   SELECT movie_id, theater_id, show_time, COUNT(*) AS copies
--   FROM shows
--   GROUP BY movie_id, theater_id, show_time
--   HAVING COUNT(*) > 1;
--
-- Never resolve duplicates by deleting shows that have bookings,
-- tickets or payments. Cancel/expire them instead.
-- ------------------------------------------------------------

SET @duplicate_groups = (
    SELECT COUNT(*) FROM (
        SELECT movie_id, theater_id, show_time
        FROM shows
        GROUP BY movie_id, theater_id, show_time
        HAVING COUNT(*) > 1
    ) AS duplicate_shows
);

SET @indexname = 'uk_shows_movie_theatre_time';

SET @index_exists = (
    SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS
    WHERE TABLE_SCHEMA = @dbname
      AND TABLE_NAME = @tablename
      AND INDEX_NAME = @indexname
);

SET @prepared_statement = IF(
    @duplicate_groups = 0 AND @index_exists = 0,
    'ALTER TABLE shows ADD UNIQUE INDEX uk_shows_movie_theatre_time (movie_id, theater_id, show_time)',
    'SELECT 1'
);

PREPARE alter_shows FROM @prepared_statement;
EXECUTE alter_shows;
DEALLOCATE PREPARE alter_shows;

-- ------------------------------------------------------------
-- show_generation_lease
--
-- Single row (id = 1) used to serialise show generation across
-- backend instances. A crashed instance leaves a lease behind, so
-- locked_until always expires and a later run can take over.
-- The database uniqueness index above remains the authoritative
-- duplicate guard; the lease only avoids wasted work.
-- ------------------------------------------------------------

CREATE TABLE IF NOT EXISTS show_generation_lease (
    id BIGINT NOT NULL,

    owner VARCHAR(120) NULL,
    locked_until DATETIME(6) NULL,
    updated_at DATETIME(6) NULL,

    PRIMARY KEY (id)
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_unicode_ci;
