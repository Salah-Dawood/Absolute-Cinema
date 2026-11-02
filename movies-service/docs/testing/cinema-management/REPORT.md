# Cinema management — live Postman API test report

Date: 2 November 2026 (Asia/Bahrain). Target: `http://localhost:8082`. All requests used Postman CLI with `--no-report-events`; no results were published to a workspace.

38 suite requests passed 60 Postman assertions. Three successful setup requests (theater, screen, seat generation) were made during diagnosis and their results were independently verified by subsequent GET assertions. The table below includes those setup requests. Expected 4xx responses cause nonzero CLI exits despite passing assertions; original statuses, assertion results and exit codes are preserved in report.json.

| Feature | Method and endpoint | Expected | Actual | Result |
|---|---|---:|---:|---|
| Create theater | `POST /api/theaters` | 201 | 201 | PASS |
| Create screen | `POST /api/screens` | 201 | 201 | PASS |
| Generate 30 physical seats | `POST /api/seats/generate` | 201 | 201 | PASS |
| Existing movie: Inception | `GET /api/movies/6ae871d6a6456bb77994130a` | 200 | 200 | PASS |
| Existing movie: Guardians of the Galaxy Vol. 2 | `GET /api/movies/6ae8769ca6456bb77994130b` | 200 | 200 | PASS |
| List theaters / discover reusable fixture | `GET /api/theaters` | 200 | 200 | PASS |
| Retrieve theater by ID | `GET /api/theaters/6ae8789aa6456bb77994130c` | 200 | 200 | PASS |
| List screens by theater | `GET /api/screens/theater/6ae8789aa6456bb77994130c` | 200 | 200 | PASS |
| Retrieve screen / verify theater reference | `GET /api/screens/6ae87901a6456bb77994130d` | 200 | 200 | PASS |
| Retrieve all screens | `GET /api/screens` | 200 | 200 | PASS |
| Inspect existing physical seats | `GET /api/seats/screen/6ae87901a6456bb77994130d` | 200 | 200 | PASS |
| Retrieve seats / verify unique physical definitions | `GET /api/seats/screen/6ae87901a6456bb77994130d` | 200 | 200 | PASS |
| Inspect screen schedule before validation | `GET /api/showtimes/screen/6ae87901a6456bb77994130d` | 200 | 200 | PASS |
| Reject duplicate A1 seat (case normalized) | `POST /api/seats` | 409 | 409 | PASS |
| Reject duplicate seat-generation rows | `POST /api/seats/generate` | 400 | 400 | PASS |
| Reject duplicate seat generation | `POST /api/seats/generate` | 409 | 409 | PASS |
| Reject screen with missing theater | `POST /api/screens` | 404 | 404 | PASS |
| Reject seat with missing screen | `POST /api/seats` | 404 | 404 | PASS |
| Reject seat number zero | `POST /api/seats` | 400 | 400 | PASS |
| Reject blank theater name | `POST /api/theaters` | 400 | 400 | PASS |
| Create showtime: Inception | `POST /api/showtimes` | 201 | 201 | PASS |
| Retrieve showtime: Inception | `GET /api/showtimes/6ae87987a6456bb77994132c` | 200 | 200 | PASS |
| Verify showtime movie relationship: Inception | `GET /api/showtimes/movie/6ae871d6a6456bb77994130a` | 200 | 200 | PASS |
| Create showtime: Guardians of the Galaxy Vol. 2 | `POST /api/showtimes` | 201 | 201 | PASS |
| Retrieve showtime: Guardians of the Galaxy Vol. 2 | `GET /api/showtimes/6ae8798aa6456bb77994132d` | 200 | 200 | PASS |
| Verify showtime movie relationship: Guardians of the Galaxy Vol. 2 | `GET /api/showtimes/movie/6ae8769ca6456bb77994130b` | 200 | 200 | PASS |
| Reject overlapping showtime | `POST /api/showtimes` | 409 | 409 | PASS |
| Reject missing movie reference | `POST /api/showtimes` | 404 | 404 | PASS |
| Reject missing screen reference | `POST /api/showtimes` | 404 | 404 | PASS |
| Reject equal start/end | `POST /api/showtimes` | 400 | 400 | PASS |
| Reject negative price | `POST /api/showtimes` | 400 | 400 | PASS |
| Reject past showtime | `POST /api/showtimes` | 400 | 400 | PASS |
| Reject missing creator header | `POST /api/showtimes` | 400 | 400 | PASS |
| Retrieve all showtimes | `GET /api/showtimes` | 200 | 200 | PASS |
| Verify screen showtimes | `GET /api/showtimes/screen/6ae87901a6456bb77994130d` | 200 | 200 | PASS |
| Verify theater showtimes | `GET /api/showtimes/theater/6ae8789aa6456bb77994130c` | 200 | 200 | PASS |
| Verify upcoming showtimes | `GET /api/showtimes/upcoming` | 200 | 200 | PASS |
| Verify screens persisted under theater | `GET /api/screens/theater/6ae8789aa6456bb77994130c` | 200 | 200 | PASS |
| Verify theater persisted in list | `GET /api/theaters` | 200 | 200 | PASS |
| Verify validation did not add seats | `GET /api/seats/screen/6ae87901a6456bb77994130d` | 200 | 200 | PASS |
| Reject layout changes after scheduling | `POST /api/seats` | 409 | 409 | PASS |
| Unsupported multipart content type (diagnostic requests) | `POST /api/theaters`, `/api/screens`, `/api/seats`, `/api/showtimes` | 415 | 503 | FAIL — source fix tested; restart pending |

## Created resources

- Theater: `6ae8789aa6456bb77994130c` — CinemaSaar Saar - API Test, Saar, Bahrain.
- Screen: `6ae87901a6456bb77994130d` — Screen 1 - API Test, belonging to that theater.
- Seats: 30 physical definitions (A1–A10, B1–B10, C1–C10). Full individual IDs are listed below and in [fixtures.json](fixtures.json).

| Seat | MongoDB ID |
|---|---|
| A1 | `6ae87906a6456bb77994130e` |
| A2 | `6ae87906a6456bb77994130f` |
| A3 | `6ae87906a6456bb779941310` |
| A4 | `6ae87906a6456bb779941311` |
| A5 | `6ae87906a6456bb779941312` |
| A6 | `6ae87906a6456bb779941313` |
| A7 | `6ae87906a6456bb779941314` |
| A8 | `6ae87906a6456bb779941315` |
| A9 | `6ae87906a6456bb779941316` |
| A10 | `6ae87906a6456bb779941317` |
| B1 | `6ae87906a6456bb779941318` |
| B2 | `6ae87906a6456bb779941319` |
| B3 | `6ae87906a6456bb77994131a` |
| B4 | `6ae87906a6456bb77994131b` |
| B5 | `6ae87906a6456bb77994131c` |
| B6 | `6ae87906a6456bb77994131d` |
| B7 | `6ae87906a6456bb77994131e` |
| B8 | `6ae87906a6456bb77994131f` |
| B9 | `6ae87906a6456bb779941320` |
| B10 | `6ae87906a6456bb779941321` |
| C1 | `6ae87906a6456bb779941322` |
| C2 | `6ae87906a6456bb779941323` |
| C3 | `6ae87906a6456bb779941324` |
| C4 | `6ae87906a6456bb779941325` |
| C5 | `6ae87906a6456bb779941326` |
| C6 | `6ae87906a6456bb779941327` |
| C7 | `6ae87906a6456bb779941328` |
| C8 | `6ae87906a6456bb779941329` |
| C9 | `6ae87906a6456bb77994132a` |
| C10 | `6ae87906a6456bb77994132b` |

## Future showtimes

- Inception: `6ae87987a6456bb77994132c` — 3 November 2026, 18:00–20:40 Bahrain time (15:00–17:40 UTC).
- Guardians of the Galaxy Vol. 2: `6ae8798aa6456bb77994132d` — 3 November 2026, 21:00–23:30 Bahrain time (18:00–20:30 UTC).
- Both are SCHEDULED, reference the same screen, and have basePrice 4.50. No seat availability or booking state is claimed. Creator is the development test header `cinemasaar-api-test-admin`, not a verified authenticated identity.

## Diagnosis and focused fix

The initial requests supplied bodies through the documented Postman `--body @file` syntax. This CLI emitted multipart/form-data despite the JSON header. The running API classified HttpMediaTypeNotSupportedException as a generic 503. JVM exception tracing established the cause. The diagnostics did not find a MongoDB write or transaction problem. Sending inline raw JSON succeeded. All initial failures are retained under initial-attempt/, screen-attempt/ and multipart-attempt/.

A specific unsupported-content-type handler was added in GlobalExceptionHandler: it returns HTTP 415 with a safe message. HttpValidationTest now checks this regression. `./mvnw -q -o verify` passed all 37 tests and packaged the JAR. The user-owned process on 8082 was not restarted; restart it in IntelliJ to apply the fix. Live 415 behavior remains unverified until then.

## Persistence and reruns

Successful creation was checked through independent GET endpoints by ID and through list/filter queries. Movie → showtime → screen → theater references matched, all 30 seat combinations were unique, and failed validations left the seat count at 30. This is real HTTP read-back evidence from the configured running service; no direct Atlas console query or credential inspection was performed. Existing movie records were not modified or deleted.

Rerun from the movies-service folder using `python3 scripts/test_cinema_management.py`. The runner finds the named theater/screen, reuses their seats and future screenings, and writes local evidence here. It does not delete records. On reruns after scheduling, duplicate-seat creation checks are skipped because immutable-layout validation masks them; the original full-run evidence retains those checks. If fixtures have been manually altered, inspect them before rerunning. Avoid parallel runner executions.

## Scope limits

No destructive deletion, edits to existing booked showtimes, cancellation, simultaneous reservation/concurrency load, gateway authentication, or teammate service was tested. Protected showtime edits/cancellations still require the existing booking/hold coordination contract. No recommendation-service files were modified. This run confirms the requested cinema creation/read/validation flows, not the complete booking integration.
