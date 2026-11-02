package com.cinemasaar.bookings.Model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BookedAddon {
    private String addonId;
    private String name;
    private int quantity;
    private BigDecimal unitPrice;
}