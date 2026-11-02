package com.cinemasaar.bookings.Model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Add-ons attach to the whole booking
 */

@AllArgsConstructor
@NoArgsConstructor
@Data
public class BookingAddon {
    private String addonId;
    private String name;
    private BigDecimal unitPrice;
    private int quantity;
    private BigDecimal lineTotal;
}
