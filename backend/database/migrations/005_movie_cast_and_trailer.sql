-- ============================================================
-- SRH CinePass movie cast and YouTube trailer metadata
--
-- Additive and idempotent. It never drops or truncates a table,
-- and it never deletes a movie, show, booking, ticket, seat or
-- payment row. Both new columns are nullable, so pre-existing rows
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
-- movie.trailer_key
--
-- YouTube video key for the movie's trailer. The full embed URL
-- is constructed by the frontend. NULL means no trailer is
-- available.
-- ------------------------------------------------------------

SET @columnname = 'trailer_key';

SET @prepared_statement = (
    SELECT IF(
        (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
         WHERE TABLE_SCHEMA = @dbname
           AND TABLE_NAME = @tablename
           AND COLUMN_NAME = @columnname) > 0,
        'SELECT 1',
        'ALTER TABLE movie ADD COLUMN trailer_key VARCHAR(50) NULL'
    )
);

PREPARE alter_movie FROM @prepared_statement;
EXECUTE alter_movie;
DEALLOCATE PREPARE alter_movie;

-- ------------------------------------------------------------
-- movie.cast_json
--
-- JSON array of principal cast members:
-- [{"name":"...","character":"...","profilePath":"/..."}].
-- NULL or empty when no cast information is available.
-- ------------------------------------------------------------

SET @columnname = 'cast_json';

SET @prepared_statement = (
    SELECT IF(
        (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
         WHERE TABLE_SCHEMA = @dbname
           AND TABLE_NAME = @tablename
           AND COLUMN_NAME = @columnname) > 0,
        'SELECT 1',
        'ALTER TABLE movie ADD COLUMN cast_json TEXT NULL'
    )
);

PREPARE alter_movie FROM @prepared_statement;
EXECUTE alter_movie;
DEALLOCATE PREPARE alter_movie;