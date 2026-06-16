-- Performance: add indexes on the foreign-key / status columns that the polling
-- endpoints filter and sort on. Without these, every poll (tracker, chat, chats tab,
-- homepage banners, technician badge counts) does a sequential scan that gets slower
-- as the bookings / chat_messages tables grow — the single biggest cause of rising
-- latency (loading spinners) under load.
--
-- The same indexes are declared as @Index on the JPA entities, so local dev
-- (spring.jpa.hibernate.ddl-auto=update) creates them automatically. Production runs
-- ddl-auto=validate, which does NOT create indexes, so apply this script there. Index
-- NAMES match the @Index names so the two paths converge (no duplicates).
--
-- Idempotent. CREATE INDEX CONCURRENTLY does not lock the table for writes, so it is
-- safe to run against a live production database. It must run OUTSIDE a transaction
-- block (psql autocommit is fine — do not wrap this file in BEGIN/COMMIT).
--
--   psql -U postgres -h <host> -d CC -v ON_ERROR_STOP=1 -f db/migrations/2026-06-16__add_performance_indexes.sql

-- bookings: customer/technician active-booking lookups, paginated history, auto-confirm scan
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_bookings_customer_status
    ON bookings (customer_id, status);
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_bookings_technician_status
    ON bookings (technician_id, status);
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_bookings_customer_created
    ON bookings (customer_id, created_at);
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_bookings_technician_status_created
    ON bookings (technician_id, status, created_at);
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_bookings_status_arrival
    ON bookings (status, arrival_marked_at);

-- chats: chat-list lookups (findChatsForUser matches customer_id OR technician_id) + booking join
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_chats_customer
    ON chats (customer_id);
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_chats_technician
    ON chats (technician_id);
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_chats_booking
    ON chats (booking_id);

-- chat_messages: message history (ordered) and unread-count aggregation
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_chat_messages_chat_created
    ON chat_messages (chat_id, created_at);
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_chat_messages_chat_isread
    ON chat_messages (chat_id, is_read);

-- technician_profiles: profile lookup on every technician request + matching/search
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_tech_profiles_user
    ON technician_profiles (user_id);
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_tech_profiles_type_verified_avail
    ON technician_profiles (service_type, is_verified, is_available);

-- payment_transactions: technician earnings sum + payment lookups by booking
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_payment_tx_user_status_type
    ON payment_transactions (user_id, status, transaction_type);
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_payment_tx_booking
    ON payment_transactions (booking_id);

-- ratings: per-technician rating lists / recalculation
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_ratings_technician
    ON ratings (technician_id);
