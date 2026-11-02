package com.cinemasaar.queue.reservation.dto;

import com.cinemasaar.queue.reservation.model.Addon;
import java.math.BigDecimal;

public record AddonResponse(String id, String name, BigDecimal price) {

    public static AddonResponse from(Addon addon) {
        return new AddonResponse(addon.getId(), addon.getName(), addon.getPrice());
    }
}
