package com.cinemasaar.bookings.Controller;

import com.cinemasaar.bookings.Enums.BookingStatus;
import com.cinemasaar.bookings.Model.Booking;
import com.cinemasaar.bookings.Service.BookingManagementService;
import com.cinemasaar.bookings.Service.CancellationService;
import com.cinemasaar.bookings.Service.NotificationService;
import com.cinemasaar.bookings.dto.Requests.CancelBookingRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/admin/bookings")
public class AdminBookingController {

    private final BookingManagementService bookingService;
    private final CancellationService cancellationService;
    private final NotificationService notificationService;

    @GetMapping
    public List<Booking> all(
            @RequestParam(required = false) BookingStatus status,
            @RequestHeader(value = "X-User-Role", defaultValue = "USER") String role) {
        AdminGuard.requireAdmin(role);
        return bookingService.getAllBookings(status);
    }

    @PostMapping("/{id}/cancel")
    public Booking cancel(
            @PathVariable String id,
            @RequestHeader("X-User-Id") String adminUserId,
            @RequestHeader(value = "X-User-Role", defaultValue = "USER") String role,
            @Valid @RequestBody CancelBookingRequest request) {
        AdminGuard.requireAdmin(role);
        Booking cancelled = cancellationService.cancelByAdmin(id, adminUserId, request.reason());
        notificationService.sendCancellationEmail(cancelled);
        return cancelled;
    }
}