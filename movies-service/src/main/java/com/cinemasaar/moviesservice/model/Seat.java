package com.cinemasaar.moviesservice.model;

import lombok.*;
import java.time.Instant;
import java.math.BigDecimal;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.*;
import org.springframework.data.mongodb.core.index.CompoundIndex;

// Stores seat definitions in the catalogue domain.
@Getter @Setter @NoArgsConstructor
@Document(collection = "seats")
@CompoundIndex(name = "unique_physical_seat", def = "{'screen_id':1,'row_label':1,'seat_number':1}", unique = true)
public class Seat {
    @Id private String id;
    @Field("screen_id") private String screenId;
    @Field("row_label") private String rowLabel;
    @Field("seat_number") private Integer seatNumber;
}
