# CinemaSaar — Member 2

Java 17 / Spring Boot 4.1.2 / Maven. Movies runs on 8082; the separate sibling recommendation-service runs on 8083 using its own Initializr Spring Boot 4.0.0 project. No teammate service code was changed.

## Run in IntelliJ

1. Open this directory as a Maven project. Set Project SDK and Maven runner JRE to Java 17. Enable annotation processing for Lombok.
2. Open `../recommendation-service` in a separate IntelliJ window as its own Maven project. Each service has its own POM, wrapper, source tree, entry point, configuration and tests. See [recommendation setup](../recommendation-service/README.md).
3. Create an Application run configuration for `com.cinemasaar.moviesservice.MoviesServiceApplication`. Use the movies-service module classpath. Set `MONGODB_URI` to your Atlas connection string with `/movie_db`; set `OMDB_API_KEY` to your own key.
4. Before using existing data, follow [migration guidance](docs/MIGRATION.md). New empty databases need no migration. Atlas must allow transactions and index creation.
5. Run movies-service. Create a second Application configuration for `com.cinemasaar.recommendationservice.RecommendationServiceApplication`, using the recommendation-service classpath. Set `MOVIE_SERVICE_URL=http://localhost:8082`, `OPENAI_API_KEY`, and optionally `OPENAI_MODEL` (default `gpt-4o-mini`) in the sibling project's run configuration.
6. Run recommendation-service. Leaving `OPENAI_API_KEY` unset enables deterministic fallback. Missing `OMDB_API_KEY` returns 503 for OMDb operations; local catalogue reads remain usable.
7. Open [sample requests](docs/requests.http) in IntelliJ. Create theater, screen and seats before creating a showtime. Use a future offset-aware timestamp. Replace IDs with returned values. The `X-User-Id` header is a local development stand-in for the trusted gateway header.

`.env.example` contains placeholders only. Spring and IntelliJ do not automatically load `.env`; put actual values in run configuration environment variables. Never put secrets in source control.

## Command line

From this directory, after setting environment variables in your shell:

```sh
./mvnw spring-boot:run
```

In another terminal, from the sibling project:

```sh
cd ../recommendation-service
./mvnw spring-boot:run
```

Build and test from each service's own directory:

```sh
# In movies-service
./mvnw clean verify

# In ../recommendation-service
./mvnw clean verify
```

There is no shared parent POM, aggregator or Java dependency between the projects. Each project uses its own Maven wrapper. Each result is an executable JAR in its project's `target` directory.

## Documentation

- [Endpoint reference and examples](docs/API.md)
- [OpenAPI contract](docs/openapi.json) — local source of truth for consumers and tooling
- [Team integration decisions](docs/CONTRACT.md)
- [Existing Atlas data migration](docs/MIGRATION.md)
- [File inventory](docs/FILES.md)
- [Verification results and limits](docs/VERIFICATION.md)

All mutation routes need Member 1's ADMIN gateway enforcement and direct service network isolation before shared deployment. This project does not add separate authentication. Showtime structural edits and cancellation return 409 until the booking/hold mutation contract is connected. No seat holds, reservations, payments, or bookings are implemented here.

## Port check

An older Java application was already running on 8082 during separation and returned 404 for the catalogue route. Stop that old IntelliJ run before starting the rebuilt movies-service. The smoke test verified the rebuilt movies JAR on temporary port 18082 communicating with recommendations on 8083; saved defaults remain 8082/8083.
