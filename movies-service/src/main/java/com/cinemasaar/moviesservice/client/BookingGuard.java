package com.cinemasaar.moviesservice.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import com.cinemasaar.moviesservice.exception.ApiException;
import org.springframework.http.HttpStatus;

// Proposed Member 4 contract; fails closed when permission cannot be established.
@Component
public class BookingGuard {
    private final RestClient client;
    private final String baseUrl;
    public BookingGuard(RestClient.Builder builder, @Value("${booking.service.url:}") String baseUrl) {
        this.baseUrl = baseUrl; this.client = builder.build();
    }
    public void requireMutationAllowed(String id) {
        if (baseUrl.isBlank()) throw ApiException.conflict("Showtime changes require booking/hold authorization integration");
        Permission permission;
        try { permission = client.get().uri(baseUrl + "/api/internal/showtimes/{id}/mutation-permission", id).retrieve().body(Permission.class); }
        catch (Exception e) { throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE, "Unable to verify booking and hold state"); }
        if (permission == null || !permission.allowed()) throw ApiException.conflict("Showtime has bookings or active holds");
    }
    public record Permission(boolean allowed) {}
}
