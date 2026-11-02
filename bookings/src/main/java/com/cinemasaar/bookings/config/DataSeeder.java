package com.cinemasaar.bookings.config;

import com.cinemasaar.bookings.Model.Addon;
import com.cinemasaar.bookings.Repository.AddonRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

@Component
@RequiredArgsConstructor
public class DataSeeder implements CommandLineRunner {

    private final AddonRepository addonRepository;

    @Override
    public void run(String... args) {
        if (addonRepository.count() > 0) return;

        addonRepository.saveAll(List.of(
                Addon.builder().name("Popcorn (Large)").description("Salted or caramel")
                        .price(new BigDecimal("3.00")).active(true).build(),
                Addon.builder().name("Soft Drink").description("Any flavor, large")
                        .price(new BigDecimal("1.50")).active(true).build(),
                Addon.builder().name("Nachos").description("With cheese dip")
                        .price(new BigDecimal("2.50")).active(true).build(),
                Addon.builder().name("Combo").description("Popcorn + 2 drinks")
                        .price(new BigDecimal("5.00")).active(true).build()
        ));
    }
}
