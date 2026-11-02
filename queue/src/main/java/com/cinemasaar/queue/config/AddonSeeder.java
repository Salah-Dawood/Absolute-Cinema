package com.cinemasaar.queue.config;

import com.cinemasaar.queue.reservation.model.Addon;
import com.cinemasaar.queue.reservation.repository.AddonRepository;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Component;

@Component
public class AddonSeeder implements ApplicationRunner {

    private final AddonRepository addons;

    public AddonSeeder(AddonRepository addons) {
        this.addons = addons;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (addons.count() > 0) {
            return;
        }
        try {
            addons.insert(List.of(
                    new Addon("popcorn", "Popcorn", new BigDecimal("5.00")),
                    new Addon("drink", "Soft Drink", new BigDecimal("3.50")),
                    new Addon("combo", "Popcorn + Drink", new BigDecimal("7.50")),
                    new Addon("candy", "Candy", new BigDecimal("2.50"))));
        } catch (DuplicateKeyException anotherInstanceSeededFirst) {
            // Fixed ids make seeding idempotent across instances.
        }
    }
}
