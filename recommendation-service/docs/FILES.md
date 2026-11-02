# Recommendation file inventory

All implementation classes/configuration and four test classes were copied unchanged from the verified nested service. The Initializr entry point and original context test were retained.

- [src/main/java/com/cinemasaar/recommendationservice/RecommendationServiceApplication.java](../src/main/java/com/cinemasaar/recommendationservice/RecommendationServiceApplication.java) — Existing application source, configuration or automated test.
- [src/main/java/com/cinemasaar/recommendationservice/client/MovieClient.java](../src/main/java/com/cinemasaar/recommendationservice/client/MovieClient.java) — Reads the public movies REST contract, never another service's database.
- [src/main/java/com/cinemasaar/recommendationservice/client/OpenAiClient.java](../src/main/java/com/cinemasaar/recommendationservice/client/OpenAiClient.java) — Requests schema-constrained recommendations and treats returned IDs as untrusted input.
- [src/main/java/com/cinemasaar/recommendationservice/config/HttpConfig.java](../src/main/java/com/cinemasaar/recommendationservice/config/HttpConfig.java) — Bounds external request times so providers cannot block requests indefinitely.
- [src/main/java/com/cinemasaar/recommendationservice/controller/RecommendationController.java](../src/main/java/com/cinemasaar/recommendationservice/controller/RecommendationController.java) — Returns recommendations without asserting bookable seat availability.
- [src/main/java/com/cinemasaar/recommendationservice/dto/AiSelection.java](../src/main/java/com/cinemasaar/recommendationservice/dto/AiSelection.java) — Strict OpenAI output contains catalogue IDs and short reasons only.
- [src/main/java/com/cinemasaar/recommendationservice/dto/CatalogueMovie.java](../src/main/java/com/cinemasaar/recommendationservice/dto/CatalogueMovie.java) — Uses only catalogue fields needed to choose movies; it makes no availability claims.
- [src/main/java/com/cinemasaar/recommendationservice/dto/PreferencesRequest.java](../src/main/java/com/cinemasaar/recommendationservice/dto/PreferencesRequest.java) — Optional preferences guide ranking; limit defaults to three.
- [src/main/java/com/cinemasaar/recommendationservice/dto/RecommendationResponse.java](../src/main/java/com/cinemasaar/recommendationservice/dto/RecommendationResponse.java) — Identifies fallback output so callers can explain the source of recommendations.
- [src/main/java/com/cinemasaar/recommendationservice/exception/CatalogueUnavailableException.java](../src/main/java/com/cinemasaar/recommendationservice/exception/CatalogueUnavailableException.java) — Catalogue failure must never produce invented or stale movie IDs.
- [src/main/java/com/cinemasaar/recommendationservice/exception/GlobalExceptionHandler.java](../src/main/java/com/cinemasaar/recommendationservice/exception/GlobalExceptionHandler.java) — Returns safe errors without leaking provider payloads or credentials.
- [src/main/java/com/cinemasaar/recommendationservice/service/RecommendationService.java](../src/main/java/com/cinemasaar/recommendationservice/service/RecommendationService.java) — Validates all AI choices against the current catalogue and provides stable local ranking.
- [src/main/resources/application.properties](../src/main/resources/application.properties) — Existing application source, configuration or automated test.
- [src/test/java/com/cinemasaar/recommendationservice/ApplicationTest.java](../src/test/java/com/cinemasaar/recommendationservice/ApplicationTest.java) — Loads the real configuration and verifies HTTP validation before external calls.
- [src/test/java/com/cinemasaar/recommendationservice/MovieClientTest.java](../src/test/java/com/cinemasaar/recommendationservice/MovieClientTest.java) — Existing application source, configuration or automated test.
- [src/test/java/com/cinemasaar/recommendationservice/OpenAiClientTest.java](../src/test/java/com/cinemasaar/recommendationservice/OpenAiClientTest.java) — Existing application source, configuration or automated test.
- [src/test/java/com/cinemasaar/recommendationservice/RecommendationServiceApplicationTests.java](../src/test/java/com/cinemasaar/recommendationservice/RecommendationServiceApplicationTests.java) — Existing application source, configuration or automated test.
- [src/test/java/com/cinemasaar/recommendationservice/RecommendationServiceTest.java](../src/test/java/com/cinemasaar/recommendationservice/RecommendationServiceTest.java) — Existing application source, configuration or automated test.

Preserved: `pom.xml`, `mvnw`, `mvnw.cmd`, `.mvn/`, `.gitattributes`, Initializr actuator dependencies. Updated `.gitignore` to exclude local credentials. Added `.env.example`, README, and service-owned API/verification documentation.
