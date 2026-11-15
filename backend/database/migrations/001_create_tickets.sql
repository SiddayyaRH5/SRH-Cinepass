-- ============================================================
-- SRH CinePass ticket table
--
-- This script is intentionally additive and idempotent. The
-- application currently uses Hibernate's additive schema update
-- (spring.jpa.hibernate.ddl-auto=update), so this file is provided
-- for review, for fresh-managed databases, and for environments
-- where DDL is applied manually before deployment.
--
-- It never drops, truncates or recreates an existing table and it
-- never deletes customer data.
-- ============================================================

CREATE TABLE IF NOT EXISTS tickets (
    id BIGINT NOT NULL AUTO_INCREMENT,

    booking_id BIGINT NOT NULL,

    -- Opaque 256-bit SecureRandom token, Base64URL encoded (43 chars).
    verification_token VARCHAR(88) NOT NULL,

    -- Customer-facing, non-secret reference (SRH-XXXXXXXXXXXX).
    ticket_reference VARCHAR(32) NOT NULL,

    status VARCHAR(20) NOT NULL DEFAULT 'BOOKED',

    created_at DATETIME(6) NOT NULL,

    verified_at DATETIME(6) NULL,
    verified_by BIGINT NULL,
    cancelled_at DATETIME(6) NULL,

    -- Optimistic-lock guard used in addition to pessimistic locking.
    version BIGINT NOT NULL DEFAULT 0,

    PRIMARY KEY (id),

    CONSTRAINT uk_tickets_booking
        UNIQUE (booking_id),

    CONSTRAINT uk_tickets_verification_token
        UNIQUE (verification_token),

    CONSTRAINT uk_tickets_reference
        UNIQUE (ticket_reference),

    CONSTRAINT fk_tickets_booking
        FOREIGN KEY (booking_id) REFERENCES bookings (id),

    CONSTRAINT fk_tickets_verified_by
        FOREIGN KEY (verified_by) REFERENCES users (id),

    INDEX idx_tickets_status (status),
    INDEX idx_tickets_created_at (created_at),
    INDEX idx_tickets_verified_at (verified_at)
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_unicode_ci;
