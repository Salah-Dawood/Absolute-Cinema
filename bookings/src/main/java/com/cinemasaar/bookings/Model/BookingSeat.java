package com.cinemasaar.bookings.Model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * One seat inside a booking
 */

@AllArgsConstructor
@NoArgsConstructor
@Data
public class BookingSeat {
    private String showtimeSeatId;
    private BigDecimal ticketPrice;
}
