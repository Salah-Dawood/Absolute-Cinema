package com.cinemasaar.bookings.Controller;

import com.cinemasaar.bookings.Exception.ApiException;
import com.cinemasaar.bookings.Model.Booking;
import com.cinemasaar.bookings.Model.Notification;
import com.cinemasaar.bookings.Model.Ticket;
import com.cinemasaar.bookings.Service.BookingManagementService;
import com.cinemasaar.bookings.Service.NotificationService;
import com.cinemasaar.bookings.Service.QRCodeService;
import com.cinemasaar.bookings.Service.TicketService;
import com.cinemasaar.bookings.dto.Requests.SendTicketEmailRequest;
import com.cinemasaar.bookings.dto.Response.TicketResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RequiredArgsConstructor
@RestController
@RequestMapping("/bookings")
public class BookingController {

    private final BookingManagementService bookingService;
    private final TicketService ticketService;
    private final QRCodeService qrCodeService;
    private final NotificationService notificationService;

    @GetMapping("/me")
    public List<Booking> myBookings(
            @RequestHeader(value = "X-User-Id", defaultValue = "test-user") String userId) {
        return bookingService.getMyBookings(userId);
    }

    @GetMapping("/{id}")
    public Booking getOne(
            @PathVariable String id,
            @RequestHeader(value = "X-User-Id", defaultValue = "test-user") String userId,
            @RequestHeader(value = "X-User-Role", defaultValue = "USER") String role) {
        return bookingService.getBooking(id, userId, role);
    }

    @GetMapping("/{id}/ticket")
    public TicketResponse ticket(
            @PathVariable String id,
            @RequestHeader(value = "X-User-Id", defaultValue = "test-user") String userId,
            @RequestHeader(value = "X-User-Role", defaultValue = "USER") String role) {
        Booking booking = bookingService.getBooking(id, userId, role);
        Ticket ticket = ticketService.getOrIssue(booking);
        return TicketResponse.from(ticket, qrCodeService.toPngDataUrl(ticket.getTicketCode()));
    }

    @PostMapping("/{id}/ticket/email")
    public Notification emailTicket(
            @PathVariable String id,
            @RequestHeader(value = "X-User-Id", defaultValue = "test-user") String userId,
            @RequestHeader(value = "X-User-Role", defaultValue = "USER") String role,
            @RequestHeader(value = "X-User-Email", required = false) String headerEmail,
            @Valid @RequestBody(required = false) SendTicketEmailRequest request) {

        String email = (request != null && request.email() != null && !request.email().isBlank())
                ? request.email()
                : headerEmail;
        if (email == null || email.isBlank()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Email address is required");
        }

        Booking booking = bookingService.getBooking(id, userId, role);
        Ticket ticket = ticketService.getOrIssue(booking);
        return notificationService.sendTicketEmail(booking, ticket, email);
    }

    @GetMapping("/{id}/notifications")
    public List<Notification> notifications(
            @PathVariable String id,
            @RequestHeader(value = "X-User-Id", defaultValue = "test-user") String userId,
            @RequestHeader(value = "X-User-Role", defaultValue = "USER") String role) {
        bookingService.getBooking(id, userId, role);
        return notificationService.getForBooking(id);
    }
}