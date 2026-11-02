package com.cinemasaar.bookings.Service;

import com.cinemasaar.bookings.Enums.BookingStatus;
import com.cinemasaar.bookings.Exception.ApiException;
import com.cinemasaar.bookings.Model.Booking;
import com.cinemasaar.bookings.Repository.BookingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@RequiredArgsConstructor
@Service
public class BookingManagementService {

    private final BookingRepository bookingRepository;

}
