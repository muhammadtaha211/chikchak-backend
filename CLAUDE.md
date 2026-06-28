# Takin Project - CLAUDE.md

## Project Overview
Takin is a home services marketplace app in Israel (Tel Aviv area) connecting customers with verified technicians (plumbers, electricians, AC repair, movers, etc). Built with Spring Boot (Java 17) backend + FlutterFlow frontend. **The app is LIVE on both the Google Play Store and the Apple App Store.** Primary market is Hebrew/Israel (the catalog and system messages are Hebrew, with Arabic also seeded for job types).

> **Naming note**: The public product/brand is **Takin**. The codebase is the legacy internal name **ChikChak** — Java package `com.chikchak`, Maven artifact `chikchak-backend`, `spring.application.name=ChikChak`, DB `CC`. They are the same project; don't be confused by the two names.

## Tech Stack
- **Backend**: Spring Boot 3.2.1, Java 17, PostgreSQL, IntelliJ IDEA
- **Frontend**: FlutterFlow (no-code, visual builder) — Claude cannot edit FlutterFlow directly, only provide guidance
- **Database**: PostgreSQL (local dev: localhost:5432, database `CC`; prod: via `DB_URL` env var)
- **Real-time**: WebSocket/STOMP for chat (`/ws`, `WebSocketConfig`), plus polling fallback
- **Push notifications**: Firebase Cloud Messaging (firebase-admin, `FirebaseConfig`, `DeviceTokenService`)
- **Email**: Spring Mail (Gmail SMTP) — best-effort operator notifications (e.g. account-deletion requests)
- **Security hardening**: per-IP rate limiting (Bucket4j + Caffeine, `RateLimitFilter`), security headers (HSTS, nosniff, referrer policy), request size limits
- **API Testing**: Postman; Swagger/OpenAPI UI in non-prod (`springdoc`, disabled in prod)
- **Hosting**: deployed to a cloud host (Railway) in production; ngrok used only for local dev tunneling
- **Version Control**: GitHub (private repo)

## Architecture
- Package root: `com.chikchak` (legacy name — see Naming note above)
- Backend: model/ dto/ repository/ service/ controller/ mapper/ security/ config/ exception/
- DTOs are essential — never expose entities directly
- JacksonConfig handles null-safe serialization, BigDecimal formatting, DateTime with 'Z' suffix
- MapStruct for entity ↔ DTO mapping (RatingMapper, BookingMapper, UserMapper, JobTypeMapper, etc.)
- JWT authentication (7-day expiry) with role-based access (CUSTOMER, TECHNICIAN, ADMIN); routes locked down in `SecurityConfig` (`/api/admin/**`=ADMIN, `/api/technician/**`=TECHNICIAN, `/api/customer/**`=CUSTOMER, `/api/auth/**` & `/api/public/**`=public)
- Static legal/support pages served publicly: `delete-account.html`, `privacy-policy.html`, `terms-of-service.html`, `support.html` (required by the stores)

## Session Workflow
### At the start of EVERY session:
1. Create a new git branch for this task: `git checkout -b task/descriptive-name`
2. Read this CLAUDE.md fully
3. Understand the current state of the project before making changes

### During the session:
- Make backend changes on the branch
- After backend changes compile and verify: `./mvnw compile` (or equivalent)
- Test the specific endpoints affected using curl or describe Postman tests
- After backend is verified, provide a clear FlutterFlow frontend workflow:
  - What API calls to create/modify
  - What page states to add
  - What UI components to build
  - What action flows to wire up
  - If any UI components or pages need to be added, give me a prompt for the FlutterFlow AI to create the skeleton for me (the page states and parameters need to be added manually by me)
- Commit changes with clear messages describing what was done

### At the end of the session:
- Commit all changes to the branch
- Provide a summary of what was done and what remains
- SUGGEST updates to this CLAUDE.md file (do NOT edit it directly — I will review and approve)

### DO NOT:
- Edit CLAUDE.md without asking first
- Push to main/master branch directly
- Make changes without compiling and verifying first

## Key Business Logic
> ⚠️ **PAYMENTS ARE DORMANT.** The app is currently **FREE for both customers and technicians** — no money moves through it. Payment/escrow/refund/pricing code exists in the backend (PaymentStatus, RefundStatus, PricingService, the refund endpoints, AutoConfirmScheduler's payment-release step) but **none of it is surfaced in the FlutterFlow frontend** and nothing is wired to a payment provider. Treat all payment, commission, escrow, and refund logic as inactive scaffolding. **Do not add refund/payment logic to new features** unless explicitly reactivating the payment system. The schedulers still run (they flip internal status fields), but the customer-facing "payment released" chat message is commented out while in no-payment mode.

### Pricing model (designed, currently dormant)
- Intended commission model: fixed ₪20 service fee + technician's diagnosis/visit fee + distance fee. Config in `application.properties` (`chikchak.service-fee=20`, `chikchak.currency=ILS`, `chikchak.max-distance-km=50`, `chikchak.base-distance-rate=2.5`).
- Standard visit/diagnosis fee ₪80 (MOVERS ₪0) — `DataInitializer.STANDARD_DIAGNOSIS_FEE`.
- Job-type prices are **platform-set** ranges (`JobType.minPrice`/`maxPrice`, EN/HE/AR names), seeded in `DataInitializer`. Per-technician pricing (`TechnicianJobPricing`) is **deprecated/dormant** — replaced by the platform-set ranges.

### Booking lifecycle (active)
- Booking statuses: PENDING → TECHNICIAN_NOTIFIED → TIME_DISCUSSION → ACCEPTED → SCHEDULED → ON_THE_WAY → ARRIVED → IN_PROGRESS → ASSESSMENT_COMPLETE → COMPLETED; also CANCELLED, DISPUTED.
- Payment statuses (internal only, not user-facing): PENDING, ESCROW, PROCESSING, COMPLETED, FAILED, REFUNDED, CANCELLED, DISPUTED.
- Auto-confirm (`AutoConfirmScheduler`, every 60s): 10 min after a technician marks ARRIVED with no customer action, flips ARRIVED → IN_PROGRESS (and internal payment status, dormant).
- Booking timeouts (`BookingTimeoutScheduler`, every 5 min): auto-cancels bookings a technician didn't answer within 2h, and time proposals a customer didn't answer within 24h (Hebrew system message + notification).
- Time selection: Customer picks date + 2-hour slot. All datetime fields (preferredDatetime, scheduledDatetime, proposedDatetime) represent the START of a 2-hour window. Frontend adds +2 hours for display.
- Rating: After booking COMPLETED, customer can rate 1-5 stars with optional comment. Booking has `isRated` flag to prevent duplicate ratings; customer can also skip via dismiss-rating. Technician's averageRating recalculated on each new rating.

## Current State
**Both the customer flow and the technician flow are complete and shipped. The app is live on Google Play and the App Store.**

### Launch & go-to-market status (as of June 2026)
- App is **published on both stores**.
- **Technician outreach has not started yet** — planned to begin ~July 1, 2026 (right after the founder's graduation on June 30).
- Sequencing strategy: **onboard a solid base of technicians FIRST**, then advertise to customers. Don't drive customer demand before there's technician supply.
- Implication for marketing work: near-term priority is **technician acquisition** (supply side), not customer ads.

### Customer flow (complete):
- Login/register (+ password reset via Firebase phone verification), category selection, technician search/list, booking review, create booking
- Tracking page with step timeline (statusLevel page state + getStatusLevel custom function)
- Chat system with optimistic updates (WebSocket + polling)
- Time proposal: technician proposes new time → persistent banner on tracking page → accept/chat/cancel
- Arrival confirmation: technician marks arrived → customer confirms (payment-release step is dormant) or disputes → admin review
- Cancel booking via /api/bookings/{id}/decline-booking (refund-choice flow exists but is dormant — app is free)
- Rating system (submit / skip), block & report users, guest technician search (browse before signup)

### Technician flow (complete):
- Receive/respond to booking requests (accept / discuss-time / decline), propose new time, update booking status through the lifecycle
- Technician dashboard, job history, badge counts for navbar/tabs, availability toggle, profile management
- Technician verification: new technicians start unverified; an admin approves them before they appear in search

### Platform / safety / compliance (complete):
- **Admin backend** (`/api/admin/**`, consumed by an external admin website): approve/reject technicians, list/suspend/reactivate users, manage & force-status bookings, review content reports, process account-deletion requests, audit log
- **Moderation** (`/api/moderation/**`): users can block and report each other / content (App Store & Play UGC-safety requirement)
- **Account & data deletion**: public no-login intake (`/api/public/account-deletion-request` + `delete-account.html`) and in-app `DELETE /api/auth/account` (Google Play Data Safety / GDPR)
- **Push notifications**: FCM device-token registration (`/api/user/device-token`) + server-side notifications on booking events
- **Technician demand signal**: when a customer search returns no available technician, they can register interest (`/api/customer/notify-when-available`)
- **Rate limiting & security headers** on all traffic

### Not yet built / pending:
- Floating booking bubble (persistent across pages when hasActiveBooking = true)
- Arabic UI / system-message localization (job-type catalog already has Arabic names; system messages are still Hebrew-only)
- Reactivation of the payment/escrow system (deferred — app is intentionally free for now)

## Rating System (current implementation)
- **Entity**: Rating (score 1-5, comment, links to booking/customer/technician)
- **Booking flag**: `isRated` (Boolean) prevents duplicate ratings
- **Auto-recalculation**: technician's averageRating updated on each new rating via `recalculateTechnicianRating()`
- **Mapper**: RatingMapper (MapStruct) maps Rating → RatingDto with bookingId, bookingNumber, customerId, customerName, technicianId
- **Request DTO**: SubmitRatingRequest (bookingId, score 1-5, comment)
- **Response DTO**: RatingDto

## Critical FlutterFlow Lessons (IMPORTANT — read these before giving FlutterFlow guidance)
- **DateTime fields in custom data types MUST be String, not DateTime** — FlutterFlow silently fails parsing DateTime
- **Never use Icon parameters in components** — hardcode icons inside, use conditional visibility
- **Never leave Icon Color as Unset** — crashes the entire page silently
- **OR chains with 5+ conditions crash components** — use custom functions + page state instead
- **Complex data type page state variables delay page render** — wrap content in null checks
- **"Parse API Response as Data Type" toggle** — keep OFF, use predefined JSON paths instead
- **AppBar bindings to page state cause gray screen** — if page state not loaded yet, page crashes. Move dynamic AppBar content inside ConditionalBuilder or add null checks.
- **Optimistic updates**: add message locally first, send API non-blocking, polling replaces when count >= local count
- **Polling condition**: only replace list when API count >= local count (prevents optimistic message flicker)
- **Component parameters**: keep components simple (String, Boolean, Integer). Avoid passing complex data types as parameters.
- **Page state initialization**: FlutterFlow initializes ALL page state variables before rendering. Complex types can delay render.
- **Lottie loading animation**: used for page loading states. isLoading page state (default true), set to false after API calls complete.

## Key API Endpoints
> All responses are wrapped in `ApiResponse<T>` (`{ success, message, data }`).

### Booking lifecycle:
- POST /api/bookings/calculate-price — Calculate price before booking (dormant pricing)
- POST /api/bookings — Create booking
- GET /api/bookings/{id} — Get booking by ID
- GET /api/bookings/number/{bookingNumber} — Get booking by number
- GET /api/bookings/active — Get active bookings for current user
- POST /api/bookings/respond — Technician accept/discuss/decline (TechnicianResponseRequest: ACCEPT/DISCUSS_TIME/DECLINE)
- PUT /api/bookings/{id}/status — Update booking status (BookingUpdateRequest)
- POST /api/bookings/{id}/propose-time — Technician proposes new time
- POST /api/bookings/{id}/accept-proposed-time — Customer accepts proposed time
- POST /api/bookings/{id}/decline-booking — Customer cancels (no body; refund choice deferred to refund page — dormant)
- POST /api/bookings/{id}/process-refund — Process refund with customer's choice (RefundChoiceRequest CREDITS/BANK — dormant)
- GET /api/bookings/unresolved-refunds — Bookings with a pending refund choice for current customer (dormant)
- POST /api/bookings/{id}/confirm-arrival — Customer confirms technician arrived
- POST /api/bookings/{id}/dispute-arrival — Customer disputes arrival (→ admin review)
- POST /api/bookings/{id}/dismiss-rating — Customer skips rating for a completed booking
- POST /api/bookings/{id}/photos — Append image URL(s) to a booking (AddPhotoRequest)

### Ratings:
- POST /api/ratings — Submit rating (SubmitRatingRequest: bookingId, score 1-5, comment)
- GET /api/ratings/my — Get current customer's ratings
- GET /api/ratings/technician/{technicianId} — Get all ratings for a technician
- GET /api/ratings/booking/{bookingId} — Get rating for a specific booking

### Chat (also real-time over WebSocket `/ws`, STOMP `/topic/chat/{chatId}`):
- POST /api/chat/send — Send message (SendChatMessageRequest: accepts bookingId OR chatId)
- GET /api/chat/my-chats — Get all chats for current user
- GET /api/chat/{chatId} — Get chat details
- GET /api/chat/{chatId}/messages — Get messages by chat ID (paginated)
- POST /api/chat/{chatId}/mark-read — Mark messages as read
- GET /api/chat/{chatId}/unread-count — Get unread count
- GET /api/chat/booking/{bookingId} — Get messages for a booking (legacy)

### Auth:
- POST /api/auth/login — Login
- POST /api/auth/register — Register (customer or technician)
- POST /api/auth/reset-password — Reset password after Firebase phone verification (public)
- GET /api/auth/me — Get current user
- PUT /api/auth/profile — Update profile
- PUT /api/auth/profile-image — Update only profile image URL
- POST /api/auth/change-password — Change password
- DELETE /api/auth/account — Permanently delete current user's account (anonymize + revoke)

### Technician:
- GET /api/technician/bookings — Get technician's bookings (paginated)
- GET /api/technician/pending-requests — Get pending booking requests
- GET /api/technician/bookings/time-discussion — Bookings awaiting customer response to a proposed time
- GET /api/technician/bookings/history — Completed/cancelled job history (paginated)
- GET /api/technician/badge-counts — Counts for navbar/tab badges
- GET /api/technician/profile — Get current technician profile
- PUT /api/technician/profile — Update technician profile
- PUT /api/technician/availability?available={bool} — Update availability
- GET /api/technician/dashboard — Get dashboard data

### Customer:
- GET /api/customer/bookings — Get customer's bookings (all statuses, paginated)
- POST /api/customer/search-technicians — Search technicians (TechnicianSearchRequest → TechnicianSearchResponse; hides blocked techs)
- GET /api/customer/dashboard — Get dashboard data
- POST /api/customer/notify-when-available — Register interest when search returns no technicians (CreateDemandRequestDto)

### User (any authenticated role):
- POST /api/user/device-token — Register FCM device token for push (RegisterDeviceTokenRequest)

### Moderation (any authenticated user):
- POST /api/moderation/block/{userId} — Block a user
- DELETE /api/moderation/block/{userId} — Unblock a user
- GET /api/moderation/blocked — List users you've blocked
- POST /api/moderation/report — Report a user / chat message / rating (CreateReportRequest)

### Admin (ROLE_ADMIN — external admin website):
- Technicians: GET /api/admin/technicians, GET /api/admin/technicians/pending, GET /api/admin/technicians/{id}, POST /api/admin/technicians/{id}/approve, POST /api/admin/technicians/{id}/reject
- Users: GET /api/admin/users, GET /api/admin/users/{id}, POST /api/admin/users/{id}/suspend, POST /api/admin/users/{id}/reactivate
- Bookings: GET /api/admin/bookings, GET /api/admin/bookings/{id}, POST /api/admin/bookings/{id}/cancel, POST /api/admin/bookings/{id}/status (force status)
- Deletion requests: GET /api/admin/deletion-requests, POST /api/admin/deletion-requests/{id}/process, POST /api/admin/deletion-requests/{id}/reject
- Reports: GET /api/admin/reports, POST /api/admin/reports/{id}/dismiss, POST /api/admin/reports/{id}/remove-content
- Audit: GET /api/admin/logs
- (Most write actions accept an optional `AdminReasonRequest` body recorded in the audit log)

### Job pricing / job types:
- GET /api/public/job-types/{serviceType} — Predefined job types (EN/HE/AR names + platform price range) for a service type
- GET /api/public/technician-pricing/{technicianId}, GET/PUT /api/technician/job-pricing — **deprecated/dormant** per-technician pricing

### Public (no auth):
- GET /api/public/services — Service type enum values
- GET /api/public/health — Health check
- POST /api/public/search-technicians — Guest technician search (no JWT; PII stripped, no block filtering)
- POST /api/public/account-deletion-request — Public account/data-deletion intake (Google Play Data Safety)

## Custom Dart Functions in FlutterFlow
- `getStatusLevel(status)` — converts status string to integer 0-6 for timeline
- `buildPreferredTime(selectedDate, slot)` — combines date picker + time slot dropdown into ISO timestamp
- `formatTime(timestamp)` — formats timestamp to HH:mm, handles "now" for optimistic messages
- `formatTimeRange(timestamp)` — formats single datetime as "Feb 15, 10:00 - 12:00" range
- `getCurrentTimestamp()` — returns current time as ISO string (use "now" workaround instead)

## FlutterFlow App State
- `authToken` (String) — JWT token
- `currentUser` (Data: User) — logged in user
- `activeBooking` (Data: BookingResponse) — current active booking
- `hasActiveBooking` (Boolean) — whether user has an active booking

## Core Principles
- **Simplicity First**: Make every change as simple as possible. Minimal code impact.
- **No Laziness**: Find root causes. No temporary fixes. Senior developer standards.
- **Minimal Impact**: Changes should only touch what's necessary. Avoid introducing bugs.
- **Prefer frontend solutions**: When possible, solve on FlutterFlow side to reduce backend bug risk.
- **Function-first**: Complete functionality before UI polish.
- **Test before suggesting**: Verify backend compiles and endpoints work before providing FlutterFlow workflow.

## Marketing Skills (installed globally)
A marketing skills suite (`coreyhaines31/marketingskills`) is installed via symlink at the global/user scope (`C:\Users\USER\.claude\skills\` → `C:\Users\USER\.agents\skills\`). These are Claude Code Skills, NOT backend code — they apply to growth/marketing tasks for Takin (e.g. App Store listing, launch, ads, landing copy), not to the Spring Boot app.

- **How to use**: invoke via the Skill tool, or they auto-surface based on each skill's `description` triggers. Skills cross-reference each other (e.g. `copywriting` → "for email copy, see `emails`"). Use `find-skills` to navigate/route when unsure which to pick.
- **Start here for any marketing work**: run `product-marketing` first — it writes `.agents/product-marketing.md` (product, ICP, positioning) that the other skills read for context. Then `marketing-plan` / `marketing-ideas` for strategy.
- **Available skills by area**:
  - *Strategy & research*: marketing-plan, marketing-ideas, product-marketing, content-strategy, customer-research, marketing-psychology, competitor-profiling, competitors
  - *Copy & content*: copywriting, copy-editing, ad-creative, emails, cold-email, social, sms, image, video
  - *Web & conversion*: cro, signup, onboarding, paywalls, popups, pricing, free-tools, lead-magnets, site-architecture
  - *SEO & discovery*: seo-audit, ai-seo, programmatic-seo, schema, directory-submissions, aso
  - *Acquisition & growth*: ads, ab-testing, analytics, launch, referrals, co-marketing, community-marketing, public-relations, prospecting, revops, sales-enablement, churn-prevention
- **Note**: `aso` (App Store Optimization) and `launch` help with store discovery, but the **near-term GTM priority is technician (supply-side) acquisition** — see the Launch & go-to-market status above. Skills like `prospecting`, `cold-email`, and `community-marketing` are most relevant for recruiting technicians before customer advertising begins.

## Testing Credentials

- Admin: (note: the real admin account is seeded from `ADMIN_EMAIL`/`ADMIN_PASSWORD` env vars, not hardcoded)
- Both customer and technician flows are now live in the app; admin actions go through the external admin website / Postman