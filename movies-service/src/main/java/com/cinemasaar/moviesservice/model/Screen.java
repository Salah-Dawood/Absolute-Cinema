package com.cinemasaar.moviesservice.model;

import lombok.*;
import java.time.Instant;
import java.math.BigDecimal;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.*;
import org.springframework.data.mongodb.core.index.CompoundIndex;

// Stores screen definitions in the catalogue domain.
@Getter @Setter @NoArgsConstructor
@Document(collection = "screens")

public class Screen {
    @Id private String id;
    @Field("theater_id") private String theaterId;
    @Field("name") private String name;
    @com.fasterxml.jackson.annotation.JsonIgnore
    @Field("schedule_version") private Long scheduleVersion;
}
