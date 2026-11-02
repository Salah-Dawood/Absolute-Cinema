# Member 2 contract decisions
Source: supplied CinemaSaar ERD (Untitled.pdf, four pages), adapted to MongoDB.
IDs are opaque strings, including cross-service user IDs; JSON uses camelCase, MongoDB snake_case.
Numeric ratings use BigDecimal; votes, metascore and seasons use Integer. Missing OMDb N/A becomes null.
All timestamps are UTC Instants; clients must send ISO-8601 timestamps with an offset.
Showtime status: SCHEDULED, CANCELLED, COMPLETED. Cancelled intervals do not block scheduling.
Protected mutations require Member 1 gateway ADMIN enforcement and network isolation. Creator identity must come from a gateway-overwritten X-User-Id header. No standalone authentication is added.
No showtime delete endpoint. Structural updates/cancellation fail closed until Member 4 supplies a mutation authorization endpoint. Proposed GET /api/internal/showtimes/{id}/mutation-permission returns {"allowed":true}; this must cover bookings AND active holds and coordinate concurrent operations. This contract needs team agreement.
Schedule creation/update uses MongoDB transactions and a screen version write to serialize concurrent scheduling and physical layout changes. Atlas must support transactions (replica set). Retry transaction conflicts as HTTP 409. Physical layout becomes immutable once any showtime references it. No other member collections are accessed.
Services own only movies, theaters, screens, seats, showtimes. Member 3 owns availability/holds; Member 4 owns bookings. Recommendation output never asserts seat availability.
Recommendation port 8083 is provisional: teammate repos were not supplied. Both services can override SERVER_PORT.

## Consuming Member 2 APIs

Member 3: fetch `/api/showtimes/{id}` for `screenId`, time and status, then `/api/seats/screen/{screenId}` for physical IDs. Create and own `showtime_seats` independently. Validate SCHEDULED status and timing before admitting queues/holds. Never interpret a seat definition as an available seat. Define how schedule creation becomes known to your service (polling these REST endpoints initially; no event delivery is implemented).

Member 4: fetch `/api/showtimes/{id}` for movie/screen references and base price, `/api/movies/{id}` for ticket display, `/api/screens/{id}` and `/api/theaters/{id}` for venue details. Your service remains responsible for authoritative final prices, booking state and payments. No booking database collection is read here.

The proposed mutation-permission GET endpoint is only an adapter boundary, not a distributed lock. A check-then-write request alone cannot prevent a new hold/booking from racing the catalogue update. Before enabling edits/cancellations in a shared deployment, agree a coordinated freeze/version protocol spanning Members 2–4 (or keep these mutations disabled). BOOKING_SERVICE_URL must remain unset until that agreement. Booked-showtime cancellation/refunds are a separate Member 4 workflow, not handled here. This implementation conservatively refuses cancellation when permission is denied.

Member 1: enforce ADMIN for every POST/PUT/PATCH/DELETE under catalogue resource routes, including imports and seat generation. Decide whether remote OMDb search should be rate limited/protected to conserve quota. Strip spoofed X-User-Id headers and insert authenticated identity. Permit public catalogue GETs and decide recommendation access/rate limits centrally. No JWT validation or service-to-service credentials are invented here. Keep service ports unreachable from untrusted clients.

All services: string IDs are provisional MongoDB contracts replacing SQL numeric ERD IDs. Agree the format of user IDs and currency before integration. The supplied ERD specifies no currency. No historical data or teammate code was changed.

## Independent project layout

`movies-service` and `recommendation-service` are sibling projects, each opened and run independently. The recommendation service reads `/api/movies/showing` using MOVIE_SERVICE_URL (default http://localhost:8082). It has no movies-service Java dependency or MongoDB dependency. Configuration and tests live with their owning project.
