# Endpoint reference

The local OpenAPI file is the tooling contract. Responses use JSON camelCase; all IDs are opaque strings. Lists return arrays (no pagination). GET `/api/movies/search` combines all supplied filters using AND; title/genre/language use literal case-insensitive substring matching, year/type are exact. Empty filters return all movies. OMDb search uses provider pagination (page 1–100); importing is explicit.

New records return 201, reads/updates return 200, successful deletes return 204. Invalid input is 400, unknown/referenced missing records 404, duplicates/overlap/protected changes 409, failed OMDb response 502, unavailable configuration/dependencies/rate limits 503. Errors contain timestamp, status, error, message, path. The recommendation endpoint belongs to the separate sibling service.

## Movies and venues (port 8082)

- `GET /api/movies` — All local movies.
- `GET /api/movies/showing` — Currently showing movies.
- `GET /api/movies/{id}` — Movie by MongoDB ID.
- `GET /api/movies/imdb/{imdbId}` — Local movie by IMDb ID.
- `GET /api/movies/search` — Combined literal title/genre/language filters and exact year/type/showing filters.
- `GET /api/movies/omdb/search` — Search OMDb titles without importing.
- `POST /api/movies/import/{imdbId}` — Import OMDb movie; duplicate IMDb ID returns 409 (ADMIN via gateway).
- `POST /api/movies/{id}/refresh` — Refresh OMDb metadata preserving local showing state (ADMIN via gateway).
- `PUT /api/movies/{id}` — Replace editable local metadata; IMDb ID is immutable (ADMIN via gateway).
- `PATCH /api/movies/{id}/showing` — Set local showing flag (ADMIN via gateway).
- `GET /api/theaters` — List theaters.
- `GET /api/theaters/{id}` — Theater by ID.
- `POST /api/theaters` — Create theater (ADMIN via gateway).
- `PUT /api/theaters/{id}` — Update theater (ADMIN via gateway).
- `DELETE /api/theaters/{id}` — Delete theater only when no dependent definitions remain (ADMIN via gateway).
- `GET /api/screens` — List screens.
- `GET /api/screens/{id}` — Screen by ID.
- `POST /api/screens` — Create screen (ADMIN via gateway).
- `PUT /api/screens/{id}` — Update screen (ADMIN via gateway).
- `DELETE /api/screens/{id}` — Delete screen only when no dependent definitions remain (ADMIN via gateway).
- `GET /api/screens/theater/{theaterId}` — Screens belonging to theater.
- `GET /api/seats/screen/{screenId}` — Physical seat definitions ordered by row and number.
- `POST /api/seats` — Create physical seat; row labels normalize to uppercase (ADMIN via gateway).
- `POST /api/seats/generate` — Generate rows atomically; duplicate input returns conflict/error (ADMIN via gateway).
- `GET /api/showtimes` — All showtimes.
- `GET /api/showtimes/{id}` — Showtime by ID.
- `GET /api/showtimes/upcoming` — Future SCHEDULED showtimes ordered by start.
- `GET /api/showtimes/movie/{movieId}` — Showtimes for movie.
- `GET /api/showtimes/screen/{screenId}` — Showtimes for screen.
- `GET /api/showtimes/theater/{theaterId}` — Showtimes for theater.
- `POST /api/showtimes` — Schedule movie on screen with no overlap (ADMIN via gateway).
- `PUT /api/showtimes/{id}` — Edit scheduled showtime after booking/hold authorization (ADMIN via gateway).
- `PATCH /api/showtimes/{id}/status` — Transition SCHEDULED to CANCELLED or COMPLETED; terminal states cannot reopen (ADMIN via gateway).

Screen/seat/showtime references are validated. Theatre deletion is blocked by screens; screen deletion is blocked by seats or any showtime. Physical seats cannot change after a screen has been scheduled, so create the full layout first. No physical-seat deletion endpoint is exposed.

Create/update showtimes using `movieId`, `screenId`, `startsAt`, `endsAt`, `basePrice`. Timestamps must be future instants with offsets; start must precede end. Adjacent screenings are allowed (`[start,end)`); cancelled intervals do not block new schedules. Prices permit 10 integer digits and 2 fractional digits. Currency is not in the ERD and must be agreed by the team. Clients should send monetary JSON numbers precisely and avoid binary floating-point arithmetic.

Create requires the trusted `X-User-Id` header. The gateway must overwrite this value from the authenticated identity. Caller-supplied status/creator cannot alter persisted creation metadata. Status starts SCHEDULED; only CANCELLED or COMPLETED may follow. COMPLETED is allowed only after the end time. Terminal statuses cannot be reopened. No showtime delete endpoint exists.

## Recommendation service

The separate [recommendation-service](../../recommendation-service/README.md) owns `POST /api/recommendations` on 8083. Its [OpenAPI contract](../../recommendation-service/docs/openapi.json) and [request examples](../../recommendation-service/docs/requests.http) live in that project. It reads this service through HTTP only.

## Sample local workflow

Use [requests.http](requests.http) to import a movie, set its showing flag, create a theater/screen/physical seats and schedule a future showtime. JSON request/response fields and errors are defined in [openapi.json](openapi.json).

## Mark Inception as currently showing

Reuse the existing endpoint. In Postman select **PATCH**, URL `http://localhost:8082/api/movies/6ae871d6a6456bb77994130a/showing`, and Body → raw → JSON:

```json
{"isShowing":true}
```

Send `Content-Type: application/json`. Success returns **200 OK** and the full updated movie document, including `id: "6ae871d6a6456bb77994130a"`, `imdbId: "tt1375666"`, `title: "Inception"` and `isShowing: true`. Only `is_showing` and `updated_at` are written to MongoDB; other metadata is preserved. An unknown movie returns 404; a missing/null flag returns 400.

Verify with `GET http://localhost:8082/api/movies` and `GET http://localhost:8082/api/movies/showing`: both should contain this ID with `isShowing: true`. This example describes the request; consult the chat verification result for whether a live update was performed.
