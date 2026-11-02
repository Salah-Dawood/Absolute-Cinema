# Queue service

Admission queue, per-seat holds and checkout for CinemaSaar. Member 3's service:
it owns the path from "user picked a showtime" to "seats are paid for", and it
deliberately does **not** own movies, users or the final booking history.

- Port: `8084`
- Database: MongoDB 7 (single-node replica set, required for transactions)
- Auth seam for now: `X-User-Id` header (Member 1's JWT will replace it)

## The two concurrency resources

The design treats two resources separately because they fail in different ways:

1. **The showtime gate** (`showtime_gates`) limits how many users may look at
   seats at the same time. Admission is a single atomic `$inc` that only succeeds
   while `activeSessions < capacity`. A slot is released on cart expiry, payment
   confirmation, or admission expiry. Users wait in `queue_entries` and are
   promoted oldest-first by a scheduled worker.
2. **The seat itself** (`showtime_seats`) is the really contended resource. A
   click is a conditional update (`status = AVAILABLE` or an expired `HELD`), so
   exactly one concurrent request can move a seat to `HELD`. The winner's cart
   id is stamped on the seat in the same write.

Carts use `@Version` optimistic locking, so two clicks that modify the same cart
produce a `409 RETRY` instead of losing an item. Multi-seat checkout is
all-or-nothing: the booking is only confirmed if every seat is still held by the
cart.

## Lifecycle

```
join -> WAITING -> ADMITTED (token, 5 min) -> OPEN cart (15 min)
     -> seats HELD -> CHECKED_OUT -> payment PAID -> seats BOOKED
                                     cart COMPLETED, queue COMPLETED
```

The 15-minute timer belongs to the cart: every seat added inherits the cart's
`expiresAt`. It never rolls forward. Expiry releases seats, cancels a pending
payment, expires a pending booking and frees the gate slot in one transaction.
A cart whose payment is `PROCESSING` is never expired underneath the gateway; a
payment that lands after release becomes `REFUND_REQUIRED` and never re-takes
seats.

## Endpoints

| Method | Path | Purpose |
| --- | --- | --- |
| POST | `/api/v1/queue/{showtimeId}/join` | Join (or rejoin) the waiting room |
| GET | `/api/v1/queue/{showtimeId}/status` | Position, token, cart id |
| GET | `/api/v1/queue/{showtimeId}/stats` | Waiting count and gate usage |
| POST | `/api/v1/carts` | Consume admission token, start the 15-min cart |
| POST | `/api/v1/carts/{cartId}/seats/{seatId}` | Hold a seat immediately |
| DELETE | `/api/v1/carts/{cartId}/seats/{seatId}` | Release a held seat |
| PUT | `/api/v1/carts/{cartId}/seats/{seatId}/addons` | Add-ons for one seat |
| GET | `/api/v1/carts/{cartId}` | Cart and remaining seconds |
| POST | `/api/v1/carts/{cartId}/checkout` | Create booking + payment intent |
| POST | `/api/v1/payments/{intentId}/pay` | Simulated immediate payment |
| GET | `/api/v1/payments/{intentId}` | Payment status |
| GET | `/api/v1/bookings/{bookingId}` | Booking status |
| GET | `/api/v1/showtimes/{showtimeId}/seats` | Seat map |
| POST | `/api/v1/admin/showtimes/{showtimeId}/seats` | Seed a showtime (demo) |
| GET | `/api/v1/addons` | Add-on catalogue |

Payment is behind `PaymentGateway`; `SimulatedPaymentGateway` is the only
implementation, so a real provider can be dropped in without touching the
booking transaction.

## Run locally

```powershell
cd queue
docker compose up -d          # Mongo replica set + one-off rs.initiate
.\mvnw.cmd spring-boot:run    # http://localhost:8084
```

The URI default is `mongodb://localhost:27017/cinemasaar?replicaSet=rs0`; override
with `MONGO_URI`.

## Curl walkthrough

```powershell
$showtime = "demo-showtime"
$user = "alice"

# 1. seed 5 rows of 8 seats at 12.50 each
Invoke-RestMethod -Method Post -ContentType application/json `
  -Uri "http://localhost:8084/api/v1/admin/showtimes/$showtime/seats" `
  -Body '{"rows":["A","B","C","D","E"],"seatsPerRow":8,"price":12.50}'

# 2. join the waiting room and poll until ADMITTED
$headers = @{ "X-User-Id" = $user }
Invoke-RestMethod -Method Post -Headers $headers `
  -Uri "http://localhost:8084/api/v1/queue/$showtime/join"
$status = Invoke-RestMethod -Headers $headers `
  -Uri "http://localhost:8084/api/v1/queue/$showtime/status"
$status   # status = ADMITTED, admissionToken = ...

# 3. open a cart with the token
$cart = Invoke-RestMethod -Method Post -Headers $headers -ContentType application/json `
  -Uri "http://localhost:8084/api/v1/carts" `
  -Body (@{ admissionToken = $status.admissionToken } | ConvertTo-Json)

# 4. hold two seats, add popcorn to the first
Invoke-RestMethod -Method Post -Headers $headers `
  -Uri "http://localhost:8084/api/v1/carts/$($cart.cartId)/seats/A1"
Invoke-RestMethod -Method Post -Headers $headers `
  -Uri "http://localhost:8084/api/v1/carts/$($cart.cartId)/seats/A2"
Invoke-RestMethod -Method Put -Headers $headers -ContentType application/json `
  -Uri "http://localhost:8084/api/v1/carts/$($cart.cartId)/seats/A1/addons" `
  -Body '{"items":[{"addonId":"popcorn","quantity":2}]}'

# 5. checkout, then pay
$booking = Invoke-RestMethod -Method Post -Headers $headers `
  -Uri "http://localhost:8084/api/v1/carts/$($cart.cartId)/checkout"
$payHeaders = @{ "X-User-Id" = $user; "Idempotency-Key" = [guid]::NewGuid().ToString() }
Invoke-RestMethod -Method Post -Headers $payHeaders -ContentType application/json `
  -Uri "http://localhost:8084/api/v1/payments/$($booking.payment.paymentIntentId)/pay" `
  -Body '{"method":"CARD"}'

# 6. booking is CONFIRMED and A1/A2 are BOOKED
Invoke-RestMethod -Headers $headers `
  -Uri "http://localhost:8084/api/v1/bookings/$($booking.bookingId)"
```

To prove the seat race, run two carts for the same showtime and fire step 4 for
the same seat from two shells with the same `$showtime`: one returns `200`, the
other `409 SEAT_UNAVAILABLE`.

## Error codes

| Status | Code | Meaning |
| --- | --- | --- |
| 409 | `SEAT_UNAVAILABLE` | Another click won the seat |
| 409 | `SEATS_LOST` | One held seat disappeared before checkout |
| 409 | `CART_STATE` | Cart is empty/complete/not open |
| 409 | `RETRY` | Optimistic lock or transaction conflict; retry |
| 409 | `PAYMENT_IN_PROGRESS` / `PAYMENT_NOT_PAYABLE` | Payment state conflict |
| 410 | `CART_EXPIRED` / `ADMISSION_EXPIRED` | The 15/5-minute window closed |
| 403 | `ADMISSION_REQUIRED` | Missing, reused or foreign admission token |
| 503 | `BOOKING_BUSY` | Seat executor saturated; retry |

## Tests

Integration tests use Testcontainers with a real replica set and cover the same
seat race, the single-use admission token, all-or-nothing checkout, payment
idempotency and both expiry paths:

```powershell
.\mvnw.cmd test
```
