# ChikChak Project - CLAUDE.md

## Project Overview
ChikChak is a home services marketplace app in Tel Aviv connecting customers with verified technicians (plumbers, electricians, AC repair, etc). Built with Spring Boot (Java 17) backend + FlutterFlow frontend, connected via ngrok during development.

## Tech Stack
- **Backend**: Spring Boot 3.2.1, Java 17, PostgreSQL, IntelliJ IDEA
- **Frontend**: FlutterFlow (no-code, visual builder) — Claude cannot edit FlutterFlow directly, only provide guidance
- **Database**: PostgreSQL (localhost:5432, database: CC)
- **API Testing**: Postman
- **Tunneling**: ngrok (development)
- **Version Control**: GitHub (private repo)

## Architecture
- Backend: model/ dto/ repository/ service/ controller/ mapper/ security/ config/ exception/
- DTOs are essential — never expose entities directly
- JacksonConfig handles null-safe serialization, BigDecimal formatting, DateTime with 'Z' suffix
- MapStruct for entity ↔ DTO mapping (RatingMapper, BookingMapper, UserMapper)
- JWT authentication with role-based access (CUSTOMER, TECHNICIAN, ADMIN)

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
- Commission model: Customers pay fixed ₪20 service fee + technician's diagnosis fee + distance fee
- Escrow payment: Money held until customer confirms technician arrival
- Auto-confirm: Payment auto-releases 10 minutes after technician marks arrival (AutoConfirmScheduler.java)
- Booking statuses: PENDING → TECHNICIAN_NOTIFIED → TIME_DISCUSSION → ACCEPTED → SCHEDULED → ON_THE_WAY → ARRIVED → IN_PROGRESS → ASSESSMENT_COMPLETE → COMPLETED
- Also: CANCELLED, DISPUTED
- Payment statuses: PENDING, ESCROW, PROCESSING, COMPLETED, FAILED, REFUNDED, CANCELLED, DISPUTED
- Time selection: Customer picks date + 2-hour slot. All datetime fields (preferredDatetime, scheduledDatetime, proposedDatetime) represent the START of a 2-hour window. Frontend adds +2 hours for display.
- Rating: After booking COMPLETED, customer can rate 1-5 stars with optional comment. Booking has `isRated` flag to prevent duplicate ratings. Technician's averageRating recalculated on each new rating.

## Current State
### Customer flow (mostly complete):
- Login/register, category selection, technician list, booking review, create booking
- Tracking page with 7-step timeline (uses statusLevel page state + getStatusLevel custom function)
- Chat system with optimistic updates and 2-second polling
- Time proposal: technician proposes new time → persistent banner on tracking page → accept/chat/cancel
- Arrival confirmation: technician marks arrived → customer confirms → payment released (or dispute → admin review)
- Auto-confirm scheduler: runs every 60 seconds, auto-releases payment 10 minutes after arrival marked
- Cancel booking with refund (credits or bank) via /api/bookings/{id}/decline-booking
- Rating system: Rating entity, RatingRepository, RatingMapper, RatingService, RatingController, SubmitRatingRequest
- Rating screen in FlutterFlow (in progress)

### Not yet built:
- Floating booking bubble (persistent across pages when hasActiveBooking = true)
- Technician flow (receiving/responding to bookings, updating status)
- Professional Info Page (pricing breakdown per job type)
- Arabic language support for system messages (MessageTemplates pattern ready)
- Reschedule feature: TIME_DISCUSSION status shows persistent banner on tracking page with proposed time + Accept/Chat/Cancel

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
### Booking lifecycle:
- POST /api/bookings — Create booking
- GET /api/bookings/{id} — Get booking by ID
- GET /api/bookings/number/{bookingNumber} — Get booking by number
- GET /api/bookings/active — Get active bookings
- PUT /api/bookings/{id}/status — Update booking status (technician)
- POST /api/bookings/respond — Technician accept/discuss/decline (TechnicianResponseRequest with ACCEPT/DISCUSS_TIME/DECLINE)
- POST /api/bookings/{id}/propose-time — Technician proposes new time
- POST /api/bookings/{id}/accept-proposed-time — Customer accepts proposed time
- POST /api/bookings/{id}/decline-booking — Customer cancels with refund (RefundChoiceRequest with CREDITS/BANK)
- POST /api/bookings/{id}/confirm-arrival — Customer confirms technician arrived
- POST /api/bookings/{id}/dispute-arrival — Customer disputes arrival
- POST /api/bookings/{id}/process-refund — Process refund with customer's choice
- POST /api/bookings/calculate-price — Calculate price before booking

### Ratings:
- POST /api/ratings — Submit rating (SubmitRatingRequest: bookingId, score 1-5, comment)
- GET /api/ratings/my — Get current customer's ratings
- GET /api/ratings/technician/{technicianId} — Get all ratings for a technician
- GET /api/ratings/booking/{bookingId} — Get rating for a specific booking

### Chat:
- POST /api/chat/send — Send message (accepts bookingId OR chatId)
- GET /api/chat/booking/{bookingId} — Get messages for booking
- GET /api/chat/{chatId} — Get chat details
- GET /api/chat/{chatId}/messages — Get messages by chat ID
- GET /api/chat/my-chats — Get all chats for current user
- POST /api/chat/{chatId}/mark-read — Mark messages as read
- GET /api/chat/{chatId}/unread-count — Get unread count

### Auth:
- POST /api/auth/login — Login
- POST /api/auth/register — Register
- GET /api/auth/me — Get current user
- PUT /api/auth/profile — Update profile
- POST /api/auth/change-password — Change password

### Technician:
- GET /api/technician/bookings — Get technician's bookings
- GET /api/technician/pending-requests — Get pending booking requests
- PUT /api/technician/profile — Update technician profile
- PUT /api/technician/availability — Update availability
- GET /api/technician/dashboard — Get dashboard data

### Customer:
- GET /api/customer/bookings — Get customer's bookings
- POST /api/customer/search-technicians — Search technicians
- GET /api/customer/dashboard — Get dashboard data

### Public:
- GET /api/public/services — Get service categories
- GET /api/public/health — Health check

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

## Testing Credentials
- Customer: customer@test.com
- Technician: tech@test.com / tech2@test.com
- Admin: admin@chikchak.com / admin123
- Technician actions tested via Postman until technician flow is built