package com.cinemasaar.bookings.Model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigDecimal;

@NoArgsConstructor
@AllArgsConstructor
@Data
@Builder
@Document("addons")
public class Addon {
    @Id
    private String id;
    private String name;
    private String description;
    private BigDecimal price;
    private boolean active;
}
