-- Adds the public account & data deletion feature (Google Play Data Safety).
--
-- Two changes, both needed on EXISTING databases:
--
-- 1. admin_action_logs_action_type_check — same Hibernate gotcha as
--    2026-06-16__fix_users_status_check.sql: the CHECK constraint for the
--    enum-backed `action_type` column was generated at table-creation time and
--    `ddl-auto=update` does NOT alter it. The new AdminActionType values
--    USER_DELETED and DELETION_REQUEST_REJECTED (written when an admin fulfills or
--    rejects a deletion request) would otherwise violate the stale constraint and
--    fail the whole transaction with HTTP 500.
--
-- 2. account_deletion_requests — a new table. Local dev (ddl-auto=update) creates
--    it automatically; production runs ddl-auto=validate, which does NOT create
--    tables, so it must be created here or the app will fail to start.
--
-- This script is idempotent. Run it once against each pre-existing environment
-- (dev / staging / prod). Freshly created databases already match and do not need it.
--
--   psql -U postgres -h <host> -d CC -v ON_ERROR_STOP=1 -f db/migrations/2026-06-18__add_account_deletion.sql

-- 1. Refresh the admin_action_logs action_type CHECK to the full
--    com.chikchak.model.AdminActionType enum.
ALTER TABLE admin_action_logs DROP CONSTRAINT IF EXISTS admin_action_logs_action_type_check;

ALTER TABLE admin_action_logs ADD CONSTRAINT admin_action_logs_action_type_check
    CHECK (action_type IN (
        'TECHNICIAN_APPROVED',
        'TECHNICIAN_REJECTED',
        'USER_SUSPENDED',
        'USER_REACTIVATED',
        'USER_DELETED',
        'BOOKING_CANCELLED',
        'BOOKING_STATUS_OVERRIDE',
        'DELETION_REQUEST_REJECTED'
    ));

-- 2. Create the deletion-request table (matches the JPA AccountDeletionRequest entity).
CREATE TABLE IF NOT EXISTS account_deletion_requests (
    id            bigserial    NOT NULL,
    created_at    timestamp(6),
    updated_at    timestamp(6),
    email         varchar(254) NOT NULL,
    full_name     varchar(100),
    phone_number  varchar(30),
    reason        text,
    processed_at  timestamp(6),
    processed_by  varchar(254),
    status        varchar(20)  NOT NULL
        CONSTRAINT account_deletion_requests_status_check
        CHECK (status IN ('PENDING', 'PROCESSED', 'REJECTED')),
    PRIMARY KEY (id)
);
