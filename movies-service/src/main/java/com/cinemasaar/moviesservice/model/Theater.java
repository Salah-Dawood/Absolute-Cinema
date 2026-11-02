package com.cinemasaar.moviesservice.model;

import lombok.*;
import java.time.Instant;
import java.math.BigDecimal;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.*;
import org.springframework.data.mongodb.core.index.CompoundIndex;

// Stores theater definitions in the catalogue domain.
@Getter @Setter @NoArgsConstructor
@Document(collection = "theaters")

public class Theater {
    @Id private String id;
    @Field("name") private String name;
    @Field("location") private String location;
}
