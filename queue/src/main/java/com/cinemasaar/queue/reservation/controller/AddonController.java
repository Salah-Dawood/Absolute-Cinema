package com.cinemasaar.queue.reservation.controller;

import com.cinemasaar.queue.reservation.dto.AddonResponse;
import com.cinemasaar.queue.reservation.repository.AddonRepository;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/addons")
public class AddonController {

    private final AddonRepository addons;

    public AddonController(AddonRepository addons) {
        this.addons = addons;
    }

    @GetMapping
    public List<AddonResponse> list() {
        return addons.findByActiveTrue().stream()
                .map(AddonResponse::from)
                .toList();
    }
}
