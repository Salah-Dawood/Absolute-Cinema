# CinemaSaar recommendation-service

Independent Java 17 Spring Boot project. Existing recommendation code was moved from the nested project without changing its implementation. This project retains its Spring Initializr Maven wrapper, Boot 4.0.0 POM, actuator dependencies, application entry point, and original context test.

## IntelliJ

1. Open this directory in its own IntelliJ window as a Maven project. Select Java 17 and enable annotation processing for Lombok.
2. Run `com.cinemasaar.recommendationservice.RecommendationServiceApplication` with the recommendation-service module classpath.
3. Set `MOVIE_SERVICE_URL=http://localhost:8082`. Set `OPENAI_API_KEY` for live AI and optionally `OPENAI_MODEL` (default `gpt-4o-mini`). The default port is 8083.
4. Start the sibling movies-service separately on 8082. It needs its own MongoDB/OMDb configuration. This recommendation project has no MongoDB or movies-service Java dependency.
5. Call `POST http://localhost:8083/api/recommendations` using the examples in `docs/requests.http`.

Leaving the OpenAI key unset enables deterministic fallback. OpenAI failures or invalid recommendations also fall back. If the movie catalogue cannot be fetched, the endpoint returns 503 rather than inventing movies. An empty catalogue returns EMPTY_CATALOGUE.

## Maven

Run commands from this directory using this project's own wrapper:

```sh
./mvnw clean verify
./mvnw spring-boot:run
```

Executable JAR: `target/recommendation-service-0.0.1-SNAPSHOT.jar`.

## Integration

`MovieClient` reads only `GET ${MOVIE_SERVICE_URL}/api/movies/showing`. JSON fields used are `id`, `title`, `genre`, `language`, `runtime`, `plot`, `isShowing`; unknown fields are ignored. IDs are opaque strings. There are no imports from `com.cinemasaar.moviesservice` and no direct database access.

The OpenAI client uses `/v1/responses`, schema-constrained IDs/reasons and `OPENAI_API_KEY`. The service validates IDs and explanations against the fetched catalogue. Mood, genres, language, energy level, preferred duration and description are retained. Fallback ranking uses genre/language/duration and stable ID ordering. Recommendations never assert seat availability.

See `docs/openapi.json` for this service's API contract and `docs/VERIFICATION.md` for the relocation checks and external-service limitations.
