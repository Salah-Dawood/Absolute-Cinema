package com.cinemasaar.queue.reservation.model;

import java.math.BigDecimal;

public record AddonLine(String addonId, String name, int quantity, BigDecimal unitPrice) {

    public BigDecimal lineTotal() {
        return unitPrice.multiply(BigDecimal.valueOf(quantity));
    }
}
