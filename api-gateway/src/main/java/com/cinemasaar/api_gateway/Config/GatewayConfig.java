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
    @Value("${services.booking-management.url}") private String bookingManagementUrl;

    @Bean
    public RouteLocator routes(RouteLocatorBuilder builder) {
        return builder.routes()

                .route("auth-service", r -> r
                        .path("/api/auth/**", "/api/users/**")
                        .filters(this::common)
                        .uri(authUrl))

                // movies-service: catalogue, theaters, screens, physical seats, showtimes
                .route("movie-service", r -> r
                        .path("/api/movies/**", "/api/showtimes/**", "/api/theaters/**",
                              "/api/screens/**", "/api/seats/**")
                        .filters(this::common)
                        .uri(movieUrl))

                .route("recommendation-service", r -> r
                        .path("/api/recommendations/**")
                        .filters(this::common)
                        .uri(recommendationUrl))

                // queue-service uses /api/v1/... and currently also handles
                // seat holds, checkout, payments and add-ons
                .route("queue-service", r -> r
                        .path("/api/v1/queue/**", "/api/v1/carts/**",
                              "/api/v1/bookings/**", "/api/v1/payments/**",
                              "/api/v1/addons/**",
                              "/api/v1/showtimes/*/seats",
                              "/api/v1/admin/showtimes/*/seats")
                        .filters(this::common)
                        .uri(queueUrl))

                // bookings service: booking history, tickets, cancellations, audit logs
                .route("booking-management-service", r -> r
                        .path("/api/bookings/**", "/api/admin/bookings/**",
                              "/api/admin/audit-logs/**", "/api/tickets/**")
                        .filters(this::common)
                        .uri(bookingManagementUrl))

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
