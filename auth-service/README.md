# auth-service

Handles **user accounts, login and JWT tokens** for the Cinema System.
Every other service relies on the token this service issues to know *who* the user is and *what role* they have.

| | |
|---|---|
| **Port** | `8081` |
| **Base path** | `/api/auth`, `/api/users` |
| **Database** | MongoDB Atlas, database `movie_db`, collection `users` |
| **Stack** | Java 17, Spring Boot 3.5.6, Spring Security, Spring Data MongoDB, jjwt 0.12.6 |
| **Package** | `com.cinemasaar.auth_service` |

---

## Features

- **Register** a new account (always created with role `USER`).
- **Log in** with email and password and receive a **JWT**.
- **Passwords hashed with BCrypt**: the plain password is never stored or returned.
- **Unique emails**: emails are stored lowercase, and duplicates are rejected (`409`).
- **Get your own profile** with a token.
- **Role-based access**: two roles, `USER` and `ADMIN`.
- **Admin user management**: list users, view a user, promote/demote roles.
- **Admin seeding**: the first admin is created automatically on startup from environment variables.
- **Consistent JSON errors** for validation, auth and not-found cases.

---

## How authentication works

```
1. Client  ── POST /api/auth/login ──────────────▶ auth-service
2. Client  ◀───────────── { token: "eyJhbGci..." } ─┘
3. Client  ── GET /api/... + Authorization: Bearer <token> ──▶ api-gateway ──▶ any service
```

1. The user logs in and gets a token.
2. The frontend stores the token and sends it on **every** request in the header:
   ```
   Authorization: Bearer <token>
   ```
3. The token expires after **24 hours** (configurable). After that the user must log in again.

---

## Endpoints

### Summary

| Method | Path | Who can call it | Description |
|---|---|---|---|
| `POST` | `/api/auth/register` | Public | Create an account and get a token |
| `POST` | `/api/auth/login` | Public | Log in and get a token |
| `GET` | `/api/users/me` | Any logged-in user | Get your own profile |
| `GET` | `/api/users` | `ADMIN` | List all users |
| `GET` | `/api/users/{id}` | `ADMIN` | Get one user by id |
| `PATCH` | `/api/users/{id}/role` | `ADMIN` | Change a user's role |

---

### `POST /api/auth/register`

Creates a new account with role `USER` and logs the user in straight away.

**Request**
```json
{
  "name": "Sara",
  "email": "sara@example.com",
  "password": "password123"
}
```

| Field | Rules |
|---|---|
| `name` | required |
| `email` | required, valid email, unique |
| `password` | required, at least 8 characters |

**Response `201 Created`**
```json
{
  "token": "eyJhbGciOiJIUzI1NiJ9...",
  "tokenType": "Bearer",
  "expiresInMs": 86400000,
  "userId": "6727a1f3c2b4e81d9f0a1234",
  "name": "Sara",
  "email": "sara@example.com",
  "role": "USER"
}
```

**Errors:** `400` invalid fields · `409` email already registered

---

### `POST /api/auth/login`

**Request**
```json
{
  "email": "sara@example.com",
  "password": "password123"
}
```

**Response `200 OK`**: same shape as register.

**Errors:** `400` invalid fields · `401` invalid email or password

---

### `GET /api/users/me`

Returns the profile of the user who owns the token.

**Headers:** `Authorization: Bearer <token>`

**Response `200 OK`**
```json
{
  "id": "6727a1f3c2b4e81d9f0a1234",
  "name": "Sara",
  "email": "sara@example.com",
  "role": "USER",
  "createdAt": "2026-11-02T10:20:31.839Z"
}
```

**Errors:** `401` missing/invalid/expired token

---

### `GET /api/users`  (ADMIN)

Lists all users. **Response `200 OK`:** an array of user objects (same shape as `/me`).

**Errors:** `401` no token · `403` not an admin

---

### `GET /api/users/{id}`  (ADMIN)

Returns one user. **Errors:** `401` · `403` · `404` user not found

---

### `PATCH /api/users/{id}/role`  (ADMIN)

**Request**
```json
{ "role": "ADMIN" }
```
`role` must be `USER` or `ADMIN`.

**Response `200 OK`:** the updated user.

**Errors:** `400` invalid role · `401` · `403` · `404`

---

## Error format

All errors return the same JSON shape:

```json
{
  "timestamp": "2026-11-02T10:20:31.839Z",
  "status": 400,
  "error": "Bad Request",
  "message": "Validation failed",
  "path": "/api/auth/register",
  "fieldErrors": {
    "email": "Email is not valid",
    "password": "Password must be at least 8 characters"
  }
}
```

`fieldErrors` only appears on validation errors.

| Status | Meaning |
|---|---|
| `400` | Invalid input |
| `401` | Wrong credentials, or missing/invalid/expired token |
| `403` | Logged in, but your role isn't allowed |
| `404` | User not found |
| `405` | Wrong HTTP method (e.g. GET instead of POST) |
| `409` | Email already registered |

---

## Integration with other services

### Token contents

The JWT is signed with **HS256** using the shared `JWT_SECRET`. Decoded payload:

```json
{
  "sub": "sara@example.com",
  "userId": "6727a1f3c2b4e81d9f0a1234",
  "role": "USER",
  "name": "Sara",
  "iat": 1762078831,
  "exp": 1762165231
}
```

| Claim | Meaning |
|---|---|
| `sub` | User's email |
| `userId` | MongoDB id of the user. **Use this to link your data to a user** (e.g. `booking.userId`) |
| `role` | `USER` or `ADMIN` |
| `name` | Display name |
| `exp` | Expiry time (seconds since epoch) |

### Through the gateway (recommended)

The **api-gateway** checks the token before forwarding the request, and passes the user to downstream services as headers:

| Header | Example |
|---|---|
| `X-User-Id` | `6727a1f3c2b4e81d9f0a1234` |
| `X-User-Role` | `USER` |
| `X-User-Email` | `sara@example.com` |

Reading them in a controller:

```java
@GetMapping("/my-bookings")
public List<Booking> myBookings(@RequestHeader("X-User-Id") String userId) {
    return bookingService.findByUser(userId);
}
```

### Admin-only endpoints

Admin endpoints live under `/api/admin/**` (e.g. `/api/admin/showtimes`, `/api/admin/bookings`). The gateway blocks non-admins on those paths with `403`.

### Getting a token for testing

1. Run auth-service.
2. `POST http://localhost:8081/api/auth/login` (or register first).
3. Copy `token` from the response.
4. In Postman → **Authorization** → **Bearer Token** → paste.

---

## Running locally

### Environment variables

| Variable | Required | Description |
|---|---|---|
| `MONGO_DB` | ✅ | Atlas connection string, e.g. `mongodb+srv://user:pass@cluster0.xxxxx.mongodb.net/?retryWrites=true&w=majority` |
| `JWT_SECRET` | ✅ | Random string, **at least 32 characters**. **Must be the same in api-gateway.** |
| `ADMIN_EMAIL` | | Email of the admin created on startup |
| `ADMIN_PASSWORD` | | Password of that admin |

**IntelliJ:** Run → Edit Configurations → AuthApplication → Environment variables:
```
MONGO_DB=mongodb+srv://...;JWT_SECRET=...;ADMIN_EMAIL=admin@cinema.com;ADMIN_PASSWORD=admin12345
```

> ⚠️ Never commit real values. Spring does not read `.env` files automatically.

**Atlas:** Network Access must allow your IP (or `0.0.0.0/0` for development), otherwise the connection times out.

### Start

Run `AuthApplication` from IntelliJ, or:
```bash
mvn spring-boot:run
```

### Tests
```bash
mvn test
```
Runs the full register → login → `/me` → admin-check flow with an in-memory fake database (no Atlas needed).

---

## Project structure

```
auth-service/
├── pom.xml
└── src/main/
    ├── java/com/cinemasaar/auth_service/
    │   ├── AuthApplication.java
    │   ├── Controller/
    │   │   ├── AuthController.java        # /api/auth/register, /login
    │   │   └── UserController.java        # /api/users/**
    │   ├── Service/
    │   │   ├── AuthService.java           # register + login logic
    │   │   ├── UserService.java           # profile + admin user management
    │   │   └── JwtService.java            # create / validate tokens
    │   ├── Repository/
    │   │   └── UserRepository.java        # MongoDB access
    │   ├── Model/
    │   │   ├── User.java                  # "users" collection
    │   │   ├── Role.java                  # USER, ADMIN
    │   │   ├── Requests/                  # RegisterRequest, LoginRequest, UpdateRoleRequest
    │   │   └── Responses/                 # AuthResponse, UserResponse
    │   ├── Security/
    │   │   ├── SecurityConfig.java        # public vs protected routes, BCrypt
    │   │   ├── JwtAuthFilter.java         # reads the Bearer token on each request
    │   │   └── AuthenticatedUser.java     # the logged-in user
    │   ├── Exceptions/
    │   │   ├── ApiException.java
    │   │   └── GlobalExceptionHandler.java
    │   └── Config/
    │       └── AdminSeeder.java           # creates first admin on startup
    └── resources/
        └── application.properties
```