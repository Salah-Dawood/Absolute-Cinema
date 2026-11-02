package com.cinemasaar.bookings.Controller;

import com.cinemasaar.bookings.Model.Ticket;
import com.cinemasaar.bookings.Service.TicketService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/tickets")
public class TicketController {

    private final TicketService ticketService;

    @GetMapping("/{code}")
    public Ticket verify(
            @PathVariable String code,
            @RequestHeader(value = "X-User-Role", defaultValue = "USER") String role) {
        AdminGuard.requireAdmin(role);
        return ticketService.verify(code);
    }

    @PostMapping("/{code}/check-in")
    public Ticket checkIn(
            @PathVariable String code,
            @RequestHeader(value = "X-User-Role", defaultValue = "USER") String role) {
        AdminGuard.requireAdmin(role);
        return ticketService.checkIn(code);
    }
}