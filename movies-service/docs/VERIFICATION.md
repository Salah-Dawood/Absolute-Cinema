# Separation verification — 2 November 2026

The recommendation implementation was reused without changing its controllers, services, DTOs, clients, configuration or exception handling. Source/test/configuration hashes match the original nested code. The new project's own Initializr entry point and original context test were preserved. Package remains `com.cinemasaar.recommendationservice`.

## Independent builds

Run from each respective directory using its own Maven wrapper:

- `movies-service`: `./mvnw -q clean verify` — PASS, 34 tests, no failures/errors/skips. Spring Boot 4.1.2 / Java 17.
- `recommendation-service`: `./mvnw -q clean verify` — PASS, 14 tests, no failures/errors/skips. Retained Initializr Spring Boot 4.0.0 / Java 17 POM, actuator dependencies and wrapper.
- Both builds produced their own executable `target/<service-name>-0.0.1-SNAPSHOT.jar`.
- Recommendation tests include strict OpenAI request/response handling, invalid IDs, malformed/incomplete output, catalogue HTTP failures, preferences, deterministic fallback and Spring Boot startup/validation.

## Actual local HTTP smoke test

Started the two independently built JARs in separate processes with isolated in-memory Mongo wire-protocol test data. No live Atlas data was accessed or changed.

- Movies-service started on temporary port 18082; `GET /api/movies/showing` returned HTTP 200 and the seeded catalogue movie.
- Recommendation-service started on its configured port 8083; `/actuator/health` returned HTTP 200 and UP.
- Set `MOVIE_SERVICE_URL=http://localhost:18082`, explicitly left the OpenAI key empty, then called `POST /api/recommendations` on 8083. It returned HTTP 200, source FALLBACK and the exact seeded movie ID from movies-service.
- All processes created for this smoke test were stopped afterward.

Port 8082 was already occupied by an existing Java process. Its catalogue endpoint returned 404. It was left running; therefore startup of this rebuilt movies JAR specifically on port 8082 could not be verified in this session. Its saved configuration remains 8082. Stop the old IntelliJ run before starting the rebuilt movies-service on 8082, and leave MOVIE_SERVICE_URL at its default http://localhost:8082.

## Limits

No paid/live OpenAI request was made. OpenAI integration is preserved and tested through mocked HTTP responses; live model entitlement, keys and network access remain unverified. No live OMDb or Atlas check was made. The Mongo fake does not implement replica-set transactions, so concurrency/rollback still needs staging Atlas or a local replica set. Gateway ADMIN protection and booking/hold coordination remain existing team integration dependencies.

The old nested folder was removed only after independent builds, source checks, startup and HTTP communication passed. Initializr scaffold backup: `/tmp/cinemasaar-recommendation-initializr-backup` (temporary local backup, not a permanent archive).
