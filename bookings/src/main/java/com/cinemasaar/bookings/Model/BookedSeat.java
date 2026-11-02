package com.cinemasaar.bookings.Model;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
public class BookedSeat {
    private String seatId;
    private String rowLabel;
    private int seatNumber;
    private BigDecimal seatPrice;
    private List<BookedAddon> addons = new ArrayList<>();
}