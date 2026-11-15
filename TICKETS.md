# SRH CinePass — QR tickets and secure verification

This document describes the ticket feature that was added to the existing SRH CinePass application. It covers the data model, issuance rules, APIs, UI routes, configuration, tests and deployment notes.

## 1. Lifecycle

```
Seat selection
  → Booking created as PENDING (10-minute seat reservation)
  → Razorpay order created
  → Razorpay Checkout completes
  → server verifies signature + captured payment
     (browser: POST /api/bookings/verify-payment
      and/or Razorpay: POST /api/payments/razorpay/webhook)
  → BookingService.confirmPaidBooking(...)
       ├─ booking becomes CONFIRMED / PAID
       ├─ seats become permanently booked
       └─ TicketService.issueForConfirmedBooking(...)
            └─ one Ticket row with an opaque SecureRandom token
  → customer opens My Bookings → View Ticket
  → staff scans the QR → POST /api/staff/tickets/verify
  → backend locks the ticket row, checks the booking, marks it USED
```

Tickets are never issued from the booking button, from the frontend, or from a pending booking. The only issuance call is inside the existing paid-confirmation transaction.

## 2. Data model

`tickets` (one row per confirmed booking):

| Column | Purpose |
| --- | --- |
| `id` | Internal primary key. Never sent to staff or embedded in the QR. |
| `booking_id` | One-to-one booking link. Unique, so retries cannot create a second ticket. |
| `verification_token` | 32 `SecureRandom` bytes, Base64URL without padding (43 characters). Unique. QR payload is exactly this value. |
| `ticket_reference` | Non-secret customer-facing reference such as `SRH-ABCDEFGHJKLM`. Unique. |
| `status` | `BOOKED`, `USED`, `CANCELLED`, `EXPIRED`. |
| `created_at` | Issuance timestamp. |
| `verified_at`, `verified_by` | Admission audit trail. |
| `cancelled_at` | Cancellation/invalidation timestamp. |
| `version` | Optimistic-lock guard used alongside pessimistic locking. |
| `show_id` | Not duplicated: the show is reached through `booking.show`. |
| `expiration` | Not duplicated: expiry is derived from the show time and the admission window. |

Indexes: `status`, `created_at`, `verified_at`. Foreign keys: `booking_id → bookings.id`, `verified_by → users.id`.

Migration: `backend/database/migrations/001_create_tickets.sql` is additive and idempotent (`CREATE TABLE IF NOT EXISTS`). The existing Hibernate setting remains `ddl-auto=update`; the script is provided for review or managed schema application. No existing table is dropped, truncated or recreated.

## 3. Backend endpoints

| Method | Path | Access | Purpose |
| --- | --- | --- | --- |
| `GET` | `/api/tickets/me` | Authenticated customer | Tickets belonging to the authenticated user only. |
| `GET` | `/api/tickets/{ticketId}` | Owner or ADMIN | Digital ticket projection. |
| `GET` | `/api/tickets/{ticketId}/qr` | Owner or ADMIN | PNG QR. The token is never returned as text. |
| `POST` | `/api/staff/tickets/verify` | `ROLE_STAFF` or `ROLE_ADMIN` | Atomic admission. |
| `POST` | `/api/payments/razorpay/webhook` | Razorpay HMAC signature | Server-to-server payment confirmation; shares the idempotent confirmation path. |
| `GET` | `/api/admin/staff` | `ROLE_ADMIN` | List staff accounts. |
| `POST` | `/api/admin/staff` | `ROLE_ADMIN` | Create a staff account or promote an existing customer. |

Verification results: `VALID`, `ALREADY_USED`, `CANCELLED`, `INVALID`, `EXPIRED`, `NOT_CONFIRMED`, `NOT_YET_VALID`. A malformed or unknown token returns HTTP 400 with `INVALID`; all other results return HTTP 200 and must be interpreted by result value. Unauthenticated requests return 401 and customers receive 403 from Spring Security before the controller runs.

The staff response deliberately omits the verification token, email, phone number, database ticket ID and booking ID. It includes only the non-secret reference, movie, theatre, show, seats, quantity, customer name, verification timestamp and staff name.

## 4. Atomicity and idempotency

- `BookingRepository.findByRazorpayOrderIdForUpdate` locks the booking during confirmation, so a retried browser callback or webhook cannot issue two tickets.
- `tickets.booking_id` is unique, providing a second database-level guarantee.
- `razorpay_webhook_events.event_id` is unique, so a replayed webhook is acknowledged as `DUPLICATE` without re-running confirmation.
- A captured payment that arrives after the 10-minute reservation expires is recovered when the seats are still free; if they were resold, the booking becomes `PAYMENT_REVIEW` / `MANUAL_REVIEW` and no ticket is issued.
- `TicketRepository.findByVerificationTokenForUpdate` locks the ticket row with `PESSIMISTIC_WRITE`. Two simultaneous scans are serialized; the second observes `USED` and receives `ALREADY_USED`.
- `@Version` on `Ticket` adds optimistic-lock protection.
- Failed verification (invalid, cancelled, not confirmed, expired, not yet valid) never marks the ticket `USED`.
- Ticket issuance and booking confirmation share one transaction. If QR/ticket persistence fails, the booking confirmation is rolled back and the idempotent payment-verification call can be retried.
- Existing confirmed bookings without tickets are backfilled once at startup by `TicketBackfillRunner`. It only selects bookings with no ticket and is safe to run repeatedly. Disable with `TICKET_BACKFILL_ON_STARTUP=false`.

## 5. Admission window

Defaults:

- Opens 120 minutes before show time.
- Closes 30 minutes after show time.
- After the window, the ticket becomes `EXPIRED`.
- Before the window, the result is `NOT_YET_VALID` and the ticket remains `BOOKED`.

Configure with `TICKET_ADMISSION_OPENS_MINUTES` and `TICKET_ADMISSION_CLOSES_MINUTES`.

## 6. Rate limiting

`TicketVerificationRateLimiter` uses an in-process Caffeine window keyed by staff user ID. Default is 60 verification attempts per minute per staff account (`TICKET_VERIFY_RATE_LIMIT`). Exceeding it returns HTTP 429. This is appropriate for the current single Render instance; a multi-instance deployment should use a shared limiter such as Redis.

## 7. Frontend routes

| Route | Access | Page |
| --- | --- | --- |
| `/tickets/:ticketId` | Authenticated customer | `client/src/pages/Ticket.jsx` — digital ticket, QR, print/PDF |
| `/staff/scanner` | `STAFF` or `ADMIN` | `client/src/pages/Staff/StaffScanner.jsx` — camera + manual token |
| `/admin/staff` | `ADMIN` | `client/src/pages/Admin/StaffManagement.jsx` — staff access management |
| `/My-Bookings` | Authenticated customer | Shows ticket status and **View Ticket** |

The print layout in `client/src/index.css` hides navigation, controls and the footer. The browser's **Save as PDF** produces a single clean ticket containing the same authenticated QR image.

Camera handling in the scanner:

- Requests the environment-facing camera on mobile and the available webcam on laptops.
- Handles denied permission, no camera, camera in use, unsupported browser and non-HTTPS contexts.
- Stops and clears the scanner when leaving the page.
- Ignores repeated decodes while one verification request is in flight.
- Never admits a ticket based on a decoded QR alone; every scan is sent to the backend.

## 8. Configuration

Optional environment variables (all have safe defaults):

| Variable | Default | Purpose |
| --- | --- | --- |
| `TICKET_QR_SIZE` | `512` | PNG QR size in pixels (clamped to 256–1024). |
| `TICKET_ADMISSION_OPENS_MINUTES` | `120` | Admission opens before show time. |
| `TICKET_ADMISSION_CLOSES_MINUTES` | `30` | Admission closes after show time. |
| `TICKET_VERIFY_RATE_LIMIT` | `60` | Verification attempts per staff account per minute. |
| `TICKET_BACKFILL_ON_STARTUP` | `true` | Issue missing tickets for existing confirmed bookings. |

No new secrets, paid services or cloud infrastructure are required. QR generation is local ZXing. Camera access uses the browser API and requires HTTPS in production, which Vercel provides.

## 9. Tests

Backend (`cd backend && mvn -B test`):

- `TicketIssuanceTests` — paid confirmation issuance, retry idempotency, no ticket for pending bookings, unique opaque tokens, QR decode, cancellation, expiry, not-yet-valid, invalid token non-consumption, unconfirmed booking, sequential double scan.
- `TicketApiSecurityTests` — 401/403 enforcement, ownership, no token serialization, PNG QR access, staff-only verification, invalid/blank tokens, staff promotion by admin.
- `TicketConcurrencyTests` — two simultaneous scans admit exactly one staff member.
- `TicketTimeZoneTests` — JVM in UTC with IST business time, 18:00 IST show admitted at 18:05 IST, opening/closing boundaries, not-yet-valid, expired, reservation expiry recovery, resold-seat manual review.
- `RazorpayWebhookTests` — valid/invalid/missing signature, duplicate event, captured and failed payments, wrong amount/order, browser/webhook race.
- `TheatreVerificationSecurityTests` — public login/catalogue, 401/403 on verification admin actions, admin success, no password/KYC leakage.
- `RazorpayConfigurationValidatorTests` — production fail-fast without echoing secret values.
- `CinepassApplicationTests` — existing context smoke test.

Frontend (`cd client && npm test`):

- `Ticket.test.jsx` — rendering, QR display, print, access error.
- `MyBookings.test.jsx` — ticket status and **View Ticket** navigation.
- `StaffScanner.test.jsx` — customer rejection, camera permission failure, valid/invalid/already-used/unauthorized results, malformed token fallback.

Integration tests use H2 in MySQL mode (`backend/src/test/resources/application.properties`), so no production or Aiven database is touched.

## 10. Deployment notes

1. Deploy the backend with the existing Render configuration. Hibernate adds the `tickets` table; optionally run `001_create_tickets.sql` first in a managed environment.
2. Configure `JWT_SECRET`, `DB_*`, and `FRONTEND_URL` as before. Production also requires `APP_ENV=production`, `RAZORPAY_KEY_ID`, `RAZORPAY_KEY_SECRET`, and `RAZORPAY_WEBHOOK_SECRET`; the webhook URL and secret are configured in the Razorpay dashboard.
3. Deploy the frontend to Vercel. `VITE_API_BASE_URL` must point to the Render backend.
4. Create the first staff account from an existing ADMIN session at `/admin/staff`, or promote an existing customer by email.
5. Staff then sign in normally and open `/staff/scanner`.
6. Camera scanning requires HTTPS in production; manual token entry is available as a fallback.

No deployment was performed as part of this implementation.
