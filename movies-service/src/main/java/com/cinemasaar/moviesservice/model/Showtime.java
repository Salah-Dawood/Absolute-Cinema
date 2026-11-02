package com.cinemasaar.moviesservice.model;

import lombok.*;
import java.time.Instant;
import java.math.BigDecimal;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.*;
import org.springframework.data.mongodb.core.index.CompoundIndex;

// Stores showtime definitions in the catalogue domain.
@Getter @Setter @NoArgsConstructor
@Document(collection = "showtimes")

public class Showtime {
    @Id private String id;
    @Field("movie_id") private String movieId;
    @Field("screen_id") private String screenId;
    @Field("starts_at") private Instant startsAt;
    @Field("ends_at") private Instant endsAt;
    @Field(value = "base_price", targetType = FieldType.DECIMAL128) private BigDecimal basePrice;
    @Field("status") private ShowtimeStatus status;
    @Field("created_by_user_id") private String createdByUserId;
    @Field("created_at") private Instant createdAt;
}
