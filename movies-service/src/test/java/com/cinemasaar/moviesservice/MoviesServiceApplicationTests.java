package com.cinemasaar.moviesservice;

import com.cinemasaar.moviesservice.model.*;
import com.cinemasaar.moviesservice.repository.*;
import org.junit.jupiter.api.Test;
import org.springframework.data.mongodb.core.mapping.*;
import org.springframework.data.mongodb.repository.Query;
import java.time.Instant;
import java.util.Set;
import static org.assertj.core.api.Assertions.*;

// Verifies Mongo field contracts without connecting to Atlas.
class MoviesServiceApplicationTests {
    @Test void erdFieldsAreMappedToSnakeCase() throws Exception {
        MongoMappingContext context = new MongoMappingContext();
        context.setInitialEntitySet(Set.of(Movie.class, Showtime.class, Seat.class));
        context.setSimpleTypeHolder(new org.springframework.data.mongodb.core.convert.MongoCustomConversions(java.util.List.of()).getSimpleTypeHolder());
        context.afterPropertiesSet();
        assertThat(context.getRequiredPersistentEntity(Movie.class).getRequiredPersistentProperty("imdbId").getFieldName()).isEqualTo("imdb_id");
        assertThat(context.getRequiredPersistentEntity(Showtime.class).getRequiredPersistentProperty("screenId").getFieldName()).isEqualTo("screen_id");
        assertThat(context.getRequiredPersistentEntity(Seat.class).getRequiredPersistentProperty("rowLabel").getFieldName()).isEqualTo("row_label");
    }
    @Test void overlapRepositoryUsesStrictBoundariesAndExistenceProjection() throws Exception {
        Query query = ShowtimeRepository.class.getMethod("overlaps", String.class, Instant.class, Instant.class, String.class).getAnnotation(Query.class);
        assertThat(query.exists()).isTrue();
        assertThat(query.value()).contains("$lt", "$gt", "CANCELLED", "_id");
    }
}
