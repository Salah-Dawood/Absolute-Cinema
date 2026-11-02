package com.cinemasaar.bookings.Service;

import com.cinemasaar.bookings.Repository.AddonRepository;
import com.cinemasaar.bookings.Repository.BookingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@RequiredArgsConstructor
@Service
public class BookingManagementService {

    private final BookingRepository bookingRepository;
    private final AddonRepository addonRepository;

}
