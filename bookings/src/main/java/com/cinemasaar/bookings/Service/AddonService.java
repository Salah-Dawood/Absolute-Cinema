package com.cinemasaar.bookings.Service;

import com.cinemasaar.bookings.Model.Addon;
import com.cinemasaar.bookings.Repository.AddonRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@RequiredArgsConstructor
@Service
public class AddonService {
    private final AddonRepository addonRepository;

    public List<Addon> getActiveAddons() {
        return addonRepository.findByActiveTrue();
    }

}
