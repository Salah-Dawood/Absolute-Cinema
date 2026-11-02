package com.cinemasaar.api_gateway.Config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.GatewayFilterSpec;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Routing table: which path goes to which service.
 * The path is forwarded unchanged, e.g.
 *   GET localhost:8000/api/movies/5  ->  GET movie-service:8082/api/movies/5
 *
 * Teammates: your controllers must use these path prefixes.
 */
@Configuration
public class GatewayConfig {

    @Value("${services.auth.url}")               private String authUrl;
    @Value("${services.movie.url}")              private String movieUrl;
    @Value("${services.recommendation.url}")     private String recommendationUrl;
    @Value("${services.queue.url}")              private String queueUrl;
    @Value("${services.seat-reservation.url}")   private String seatReservationUrl;
    @Value("${services.booking-management.url}") private String bookingManagementUrl;
    @Value("${services.audit.url}")              private String auditUrl;
    @Value("${services.notification.url}")       private String notificationUrl;

    @Bean
    public RouteLocator routes(RouteLocatorBuilder builder) {
        return builder.routes()

                // Member 1
                .route("auth-service", r -> r
                        .path("/api/auth/**", "/api/users/**")
                        .filters(this::common)
                        .uri(authUrl))

                // Member 2
                .route("movie-service", r -> r
                        .path("/api/movies/**", "/api/showtimes/**", "/api/theaters/**",
                              "/api/admin/movies/**", "/api/admin/showtimes/**")
                        .filters(this::common)
                        .uri(movieUrl))

                .route("recommendation-service", r -> r
                        .path("/api/recommendations/**")
                        .filters(this::common)
                        .uri(recommendationUrl))

                // Member 3
                .route("queue-service", r -> r
                        .path("/api/queue/**")
                        .filters(this::common)
                        .uri(queueUrl))

                .route("seat-reservation-service", r -> r
                        .path("/api/reservations/**", "/api/seats/**")
                        .filters(this::common)
                        .uri(seatReservationUrl))

                // Member 4
                .route("booking-management-service", r -> r
                        .path("/api/bookings/**", "/api/addons/**", "/api/payments/**",
                              "/api/admin/bookings/**", "/api/admin/addons/**")
                        .filters(this::common)
                        .uri(bookingManagementUrl))

                .route("audit-service", r -> r
                        .path("/api/admin/audit/**")
                        .filters(this::common)
                        .uri(auditUrl))

                .route("notification-service", r -> r
                        .path("/api/tickets/**", "/api/notifications/**")
                        .filters(this::common)
                        .uri(notificationUrl))

                .build();
    }

    /**
     * Filters applied to every route.
     * CORS is handled here in the gateway; if a service also adds CORS headers,
     * the browser would see them twice and reject the response, so duplicates are removed.
     */
    private GatewayFilterSpec common(GatewayFilterSpec f) {
        return f.dedupeResponseHeader(
                "Access-Control-Allow-Origin Access-Control-Allow-Credentials",
                "RETAIN_UNIQUE");
    }
}
