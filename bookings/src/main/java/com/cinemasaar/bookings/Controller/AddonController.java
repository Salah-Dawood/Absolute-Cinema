package com.cinemasaar.bookings.Controller;

import com.cinemasaar.bookings.Model.Addon;
import com.cinemasaar.bookings.Service.AddonService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RequiredArgsConstructor
@RestController
@RequestMapping("/addons")
public class AddonController {
    private final AddonService addonService;

    @GetMapping
    public List<Addon> list() {
        return addonService.getActiveAddons();
    }
}
