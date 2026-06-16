-- Fix: account deletion fails with HTTP 500 on databases created before the
-- UserStatus enum gained the DELETED value.
--
-- Root cause: Hibernate generates a CHECK constraint for the enum-backed `status`
-- column at table-creation time. `spring.jpa.hibernate.ddl-auto=update` does NOT
-- alter existing CHECK constraints, so on any database created before DELETED was
-- added, `users_status_check` still rejects 'DELETED'. AuthService.deleteAccount()
-- sets status = DELETED, which then violates the stale constraint.
--
-- This script is idempotent. Run it once against each pre-existing environment
-- (dev / staging / prod). Freshly created databases already include all enum
-- values and do not need it.
--
--   psql -U postgres -h <host> -d CC -v ON_ERROR_STOP=1 -f db/migrations/2026-06-16__fix_users_status_check.sql
--
-- The value set must match the full com.chikchak.model.UserStatus enum:
--   ACTIVE, INACTIVE, SUSPENDED, PENDING_VERIFICATION, DELETED

ALTER TABLE users DROP CONSTRAINT IF EXISTS users_status_check;

ALTER TABLE users ADD CONSTRAINT users_status_check
    CHECK (status IN ('ACTIVE', 'INACTIVE', 'SUSPENDED', 'PENDING_VERIFICATION', 'DELETED'));
