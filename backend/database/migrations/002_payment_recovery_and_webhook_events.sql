-- ============================================================
-- SRH CinePass payment recovery + Razorpay webhook idempotency
--
-- Additive and idempotent: every column is added only when it is
-- missing, and the webhook table is created only when it is absent.
-- No existing row is modified, reinterpreted or deleted.
--
-- The application also uses Hibernate's additive schema update, so
-- this file is for review, fresh-managed databases, and manual DDL
-- application before deployment.
-- ============================================================

-- ------------------------------------------------------------
-- bookings: explicit recovery state for captured payments that
-- could not be confirmed automatically.
-- ------------------------------------------------------------

SET @dbname = DATABASE();

SET @tablename = 'bookings';
SET @columnname = 'payment_recovery_status';
SET @prepared_statement = (
    SELECT IF(
        (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
         WHERE TABLE_SCHEMA = @dbname
           AND TABLE_NAME = @tablename
           AND COLUMN_NAME = @columnname) > 0,
        'SELECT 1',
        'ALTER TABLE bookings ADD COLUMN payment_recovery_status VARCHAR(30) NULL'
    )
);
PREPARE alter_bookings FROM @prepared_statement;
EXECUTE alter_bookings;
DEALLOCATE PREPARE alter_bookings;

SET @columnname = 'payment_recovery_reason';
SET @prepared_statement = (
    SELECT IF(
        (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
         WHERE TABLE_SCHEMA = @dbname
           AND TABLE_NAME = @tablename
           AND COLUMN_NAME = @columnname) > 0,
        'SELECT 1',
        'ALTER TABLE bookings ADD COLUMN payment_recovery_reason VARCHAR(500) NULL'
    )
);
PREPARE alter_bookings FROM @prepared_statement;
EXECUTE alter_bookings;
DEALLOCATE PREPARE alter_bookings;

SET @columnname = 'payment_recovery_at';
SET @prepared_statement = (
    SELECT IF(
        (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
         WHERE TABLE_SCHEMA = @dbname
           AND TABLE_NAME = @tablename
           AND COLUMN_NAME = @columnname) > 0,
        'SELECT 1',
        'ALTER TABLE bookings ADD COLUMN payment_recovery_at DATETIME(6) NULL'
    )
);
PREPARE alter_bookings FROM @prepared_statement;
EXECUTE alter_bookings;
DEALLOCATE PREPARE alter_bookings;

-- ------------------------------------------------------------
-- razorpay_webhook_events: one row per Razorpay event ID.
-- The unique event_id is the idempotency guarantee.
-- ------------------------------------------------------------

CREATE TABLE IF NOT EXISTS razorpay_webhook_events (
    id BIGINT NOT NULL AUTO_INCREMENT,

    event_id VARCHAR(120) NOT NULL,
    event_type VARCHAR(80) NULL,
    status VARCHAR(30) NOT NULL,

    razorpay_order_id VARCHAR(100) NULL,
    razorpay_payment_id VARCHAR(100) NULL,
    booking_id BIGINT NULL,

    message VARCHAR(500) NULL,

    received_at DATETIME(6) NOT NULL,
    processed_at DATETIME(6) NULL,

    PRIMARY KEY (id),

    CONSTRAINT uk_razorpay_webhook_event_id
        UNIQUE (event_id),

    INDEX idx_webhook_event_status (status),
    INDEX idx_webhook_event_received_at (received_at)
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_unicode_ci;
