package com.cinemasaar.bookings.Service;

import com.cinemasaar.bookings.Enums.BookingStatus;
import com.cinemasaar.bookings.Exception.ApiException;
import com.cinemasaar.bookings.Model.Booking;
import com.cinemasaar.bookings.Repository.BookingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.List;

@RequiredArgsConstructor
@Service
public class BookingManagementService {

    private final BookingRepository bookingRepository;

    public Booking getBooking(String bookingId, String userId, String role) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Booking not found"));
        if (!booking.getUserId().equals(userId) && !"ADMIN".equals(role)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "You can only view your own bookings");
        }
        return booking;
    }

    public List<Booking> getMyBookings(String userId) {
        return bookingRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    public List<Booking> getAllBookings(BookingStatus status) {
        if (status == null) {
            return bookingRepository.findAllByOrderByCreatedAtDesc();
        }
        return bookingRepository.findByStatusOrderByCreatedAtDesc(status);
    }
}