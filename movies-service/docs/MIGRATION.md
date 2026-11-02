# Existing MongoDB documents

The initial Movie class stored camelCase property names and numeric metadata as strings. The ERD requires snake_case storage and numeric ratings/counts. JSON API fields stay camelCase; `_id` is exposed as `id` and must be treated as an opaque string.

Before upgrading an existing Atlas database:

1. Back up/export `movies` and pause all catalogue writers. Use a staging copy first. No database migration was run by this implementation.
2. Inspect document keys. Rename each camelCase field only when its snake_case counterpart is absent: `imdbId` → `imdb_id`, `imdbRating` → `imdb_rating`, `imdbVotes` → `imdb_votes`, `boxOffice` → `box_office`, `posterUrl` → `poster_url`, `websiteUrl` → `website_url`, `totalSeasons` → `total_seasons`, `isShowing` → `is_showing`, `omdbLastSyncedAt` → `omdb_last_synced_at`, `createdAt` → `created_at`, `updatedAt` → `updated_at`. If both keys exist, reconcile their values manually rather than overwriting either value blindly.
3. Convert `imdb_rating` to Decimal128 and `imdb_votes`, `metascore`, `total_seasons` to BSON integers. Strip commas from votes. Convert `N/A`, blank and invalid values to null. Mongo `$convert` supports `onError:null` and `onNull:null`; inspect results before replacing production values.
4. Verify all movie records have a real `tt` IMDb ID, title, `is_showing` boolean, and creation/update BSON dates. Preserve existing `_id` values so cross-service references remain valid. Investigate invalid or duplicate IMDb IDs before enabling writers.
5. Merge duplicate IMDb IDs deliberately with teammates if they are referenced. The unique IMDb index must be created successfully before imports are accepted. A sparse index allows historical documents with no IMDb ID but not repeated explicit null values; fix these before startup.
6. BSON dates represent instants. The original `LocalDateTime` conversion used the JVM time zone. Verify old timestamps rather than assuming that a displayed local time was UTC. New API timestamps are ISO-8601 UTC; send `Z` or an explicit offset.
7. Start the service on staging, verify `GET /api/movies`, IMDb lookups and `/showing`, then repeat the reviewed migration process in production. MongoDB index creation is enabled at startup; existing conflicting data causes startup to fail rather than silently disabling duplicate prevention.

ERD adaptations: Mongo string IDs replace SQL `bigserial`/`bigint`; prices are Decimal128/BigDecimal; numeric movie fields follow the ERD. Internal `schedule_version` on screens and `catalogue_version` on theaters support concurrent catalogue writes. Theater location is required by the API as a useful cinema constraint, although the diagram permits null. New showtimes must start in the future. These are explicit, revisable application rules.
