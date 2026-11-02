# api-gateway

The single entry point of the Cinema System. All client requests go through the gateway, which validates the JWT and forwards each request to the right service.

| | |
|---|---|
| **Port** | `8000` (configurable with `GATEWAY_PORT`) |
| **Stack** | Java 17, Spring Boot 3.5.6, Spring Cloud Gateway (WebFlux), Spring Security |
| **Package** | `com.cinemasaar.api_gateway` |

```
Frontend ──▶ api-gateway :8000 ──┬──▶ auth-service :8081
                                 ├──▶ movie-service :8082
                                 ├──▶ recommendation-service :8083
                                 ├──▶ queue-service :8084
                                 ├──▶ booking-service (seats) :8085
                                 └──▶ bookings (booking management) :8086
```

---

## Features

- **Routing**: forwards each request to a service based on its path. The path is not changed: `GET :8000/api/movies/5` → `GET movie-service:8082/api/movies/5`.
- **JWT validation**: validates tokens issued by auth-service in one place.
- **User headers**: passes the authenticated user to services as `X-User-Id`, `X-User-Role` and `X-User-Email`.
- **Admin protection**: all `/api/admin/**` and `/api/v1/admin/**` paths require role `ADMIN`.
- **CORS**: allows the frontend to call the API from the browser.
- **Request logging**: logs every request, e.g. `POST /api/auth/login -> 200 (85 ms)`.

---

## Routing table

| Service | Port | Paths |
|---|---|---|
| auth-service | 8081 | `/api/auth/**`, `/api/users/**` |
| movie-service | 8082 | `/api/movies/**`, `/api/showtimes/**`, `/api/theaters/**`, `/api/admin/movies/**`, `/api/admin/showtimes/**` |
| recommendation-service | 8083 | `/api/recommendations/**` |
| queue-service | 8084 | `/api/v1/queue/**`, `/api/v1/carts/**`, `/api/v1/bookings/**`, `/api/v1/payments/**`, `/api/v1/addons/**`, `/api/v1/showtimes/*/seats`, `/api/v1/admin/showtimes/*/seats` |
| booking-service (seats) | 8085 | `/api/reservations/**`, `/api/seats/**` |
| bookings (booking management) | 8086 | `/api/bookings/**`, `/api/admin/bookings/**`, `/api/admin/audit-logs/**`, `/api/tickets/**` |

Requests to a path not listed here return `404`. Routes are defined in `Config/GatewayConfig.java`.

---

## Access rules

| Path | Access |
|---|---|
| `/api/auth/**` | Public |
| `GET /api/movies/**`, `GET /api/showtimes/**` | Public |
| `GET /api/v1/showtimes/*/seats`, `GET /api/v1/addons/**` | Public |
| `/api/admin/**`, `/api/v1/admin/**` | `ADMIN` only (`403` otherwise) |
| All other paths | Any authenticated user (`401` otherwise) |

---

## Headers forwarded to services

For requests with a valid token, the gateway adds:

| Header | Value | Example |
|---|---|---|
| `X-User-Id` | User's MongoDB id | `6727a1f3c2b4e81d9f0a1234` |
| `X-User-Role` | `USER` or `ADMIN` | `USER` |
| `X-User-Email` | User's email | `sara@example.com` |

- The original `Authorization: Bearer ...` header is also forwarded.
- Any `X-User-*` headers sent by the client are removed before the real values are added, so they cannot be spoofed.
- On public paths without a token, these headers are not present.

Reading them in a service:

```java
@PostMapping
public Booking create(@RequestHeader("X-User-Id") String userId,
                      @RequestBody CreateBookingRequest request) {
    return bookingService.create(userId, request);
}
```

Since CORS and token validation are handled by the gateway, downstream services don't need their own CORS or JWT configuration.

When calling a service directly (bypassing the gateway), these headers can be set manually for testing:
```
X-User-Id: test-user-1
X-User-Role: ADMIN
```

---

## Error responses

```json
{
  "timestamp": "2026-11-02T10:20:31.839Z",
  "status": 401,
  "error": "Unauthorized",
  "message": "Missing or invalid token",
  "path": "/api/bookings"
}
```

| Status | Cause |
|---|---|
| `401` | Missing, invalid or expired token |
| `403` | Valid token without `ADMIN` role on an admin path |
| `404` | No route matches the path |
| `500` / `503` | Target service is not running |

---

## Configuration

| Variable | Required | Default |
|---|---|---|
| `JWT_SECRET` | ✅ | Must be identical to auth-service |
| `GATEWAY_PORT` | | `8000` |
| `FRONTEND_URL` | | `http://localhost:8080,http://localhost:5173,http://localhost:3000` |
| `AUTH_SERVICE_URL` | | `http://localhost:8081` |
| `MOVIE_SERVICE_URL` | | `http://localhost:8082` |
| `RECOMMENDATION_SERVICE_URL` | | `http://localhost:8083` |
| `QUEUE_SERVICE_URL` | | `http://localhost:8084` |
| `SEAT_RESERVATION_SERVICE_URL` | | `http://localhost:8085` |
| `BOOKING_MANAGEMENT_SERVICE_URL` | | `http://localhost:8086` |

In docker-compose, service URLs use container names, e.g. `AUTH_SERVICE_URL=http://auth-service:8081`.

---

## Running

Run `GatewayApplication` with `JWT_SECRET` set, or:

```bash
mvn spring-boot:run
```

> The gateway runs on WebFlux. Adding `spring-boot-starter-web` to this project prevents it from starting.

---

## Project structure

```
api-gateway/
├── pom.xml
└── src/main/
    ├── java/com/cinemasaar/api_gateway/
    │   ├── GatewayApplication.java
    │   ├── Config/
    │   │   ├── GatewayConfig.java        # routing table
    │   │   └── SecurityConfig.java       # JWT validation, access rules, CORS
    │   └── Filter/
    │       ├── RequestLoggingFilter.java # logs every request
    │       └── UserHeadersFilter.java    # adds X-User-* headers
    └── resources/
        └── application.properties
```