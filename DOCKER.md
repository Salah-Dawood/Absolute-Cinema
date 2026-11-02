# Docker build and test guide

## 1. Prerequisites

Start Docker Desktop with Linux containers, then open PowerShell in the repository root:

```powershell
cd D:\Concurrency
docker version
docker compose version
```

Docker builds the Java applications using Maven in a build stage; you do not need Maven or Java installed on your host. Each runtime image uses `eclipse-temurin:17-jre`, `/tmp`, `/app/logs`, and `/app/app.jar`. Logs go to container stdout. The default Spring profile is used; no separate dev configuration is required.

## 2. Build all six images

```powershell
docker compose config --quiet
docker compose build
```

Both commands should exit successfully. The first build downloads base images and Maven dependencies. Subsequent builds reuse caches. Image builds compile tests but skip running them; a successful build alone is not a functional test.

## 3. Start the stack

```powershell
docker compose up -d
docker compose ps -a
docker compose images
```

Expect seven running containers (six applications plus MongoDB). `mongo-init` should show `Exited (0)`: it is a one-shot replica-set initializer. MongoDB should show `healthy`. Application `Up` means the process is running, not necessarily ready; allow a few minutes on the first startup and use the requests below to verify readiness.

In Docker Desktop, open **Containers**, expand **cinemasaar**, and select a service to view its logs. The **Images** tab shows the six `cinemasaar/*` images.

| Compose service | Source directory | Host port |
| --- | --- | --- |
| api-gateway | api-gateway | 8000 |
| auth-service | auth-service | 8081 |
| movies-service | movies-service | 8082 |
| recommendation-service | recommendation-service | 8083 |
| queue-service | queue | 8084 |
| booking-service | bookings | 8086 |
| mongo | MongoDB 7 | 27017 |

Use the gateway for application requests. There is no frontend in this stack, so opening the gateway root does not display a website.

## 4. Inspect startup and database readiness

```powershell
docker compose logs --tail 50 api-gateway auth-service movies-service recommendation-service queue-service booking-service
docker compose exec mongo mongosh --quiet --eval 'db.hello().isWritablePrimary'
```

Look for each application's `Started ...Application` message. The MongoDB command should print `true`. Transactions require the replica set, which Compose initializes automatically.

For live logs (Ctrl+C stops following logs, not the containers):

```powershell
docker compose logs -f queue-service
```

## 5. Test HTTP requests through the gateway

Run these commands in the same PowerShell window. They create a uniquely named demo user in the local database.

```powershell
$base = 'http://localhost:8000'
$email = "docker-$([guid]::NewGuid().ToString('N'))@example.com"
$body = @{
    name = 'Docker Test'
    email = $email
    password = 'Password123'
} | ConvertTo-Json

$registered = Invoke-RestMethod "$base/api/auth/register" -Method Post -ContentType 'application/json' -Body $body
$registered | Select-Object userId, email, role

$loginBody = @{ email = $email; password = 'Password123' } | ConvertTo-Json
$login = Invoke-RestMethod "$base/api/auth/login" -Method Post -ContentType 'application/json' -Body $loginBody
$headers = @{ Authorization = "Bearer $($login.token)" }

Invoke-WebRequest "$base/api/movies" -UseBasicParsing | Select-Object StatusCode, Content
Invoke-WebRequest "$base/api/v1/addons" -UseBasicParsing | Select-Object StatusCode, Content
Invoke-WebRequest "$base/api/bookings/me" -Headers $headers -UseBasicParsing | Select-Object StatusCode, Content
Invoke-RestMethod 'http://localhost:8083/actuator/health'
```

Expected: registration returns a `USER`, login returns a token, the three gateway GET requests return HTTP 200, and recommendation health returns `UP`. Movies and bookings may be empty (`[]`) on a fresh database. Add-ons should contain the default catalog. These checks cover routing, authentication, database access, and service readiness; they do not test a complete purchase or external providers.

Verify that a protected endpoint rejects requests without a token:

```powershell
curl.exe -i http://localhost:8000/api/bookings/me
```

Expected: HTTP `401 Unauthorized`.

## 6. Configuration

Compose uses its own local MongoDB `movie_db` database, regardless of an Atlas `MONGODB_URI` in your existing `.env`. Data lives in the named volume `cinemasaar_mongo-data`.

The local demo admin defaults to `admin@cinemasaar.com` / `Admin12345`. Optional `.env` entries are `JWT_SECRET` (at least 32 characters), `ADMIN_EMAIL`, `ADMIN_PASSWORD`, `FRONTEND_URL`, `OMDB_API_KEY`, `OPENAI_API_KEY`, `MAIL_USERNAME`, and `MAIL_PASSWORD`. Keep secrets out of Git. The checked-in credentials are local demo defaults; replace them before shared deployment. All mapped ports are exposed on the host.

OMDb imports, OpenAI-backed recommendations, and email delivery require their respective credentials. Those provider operations are not covered by the startup checks. Changing admin seed credentials does not change an existing account's password.

After changing environment settings, recreate containers with `docker compose up -d`. For host-side MongoDB tools, use `mongodb://localhost:27017/movie_db?directConnection=true`; the replica-set member name `mongo` resolves inside Docker's network.

## 7. Rebuild one service

For example, after editing queue code:

```powershell
docker compose build queue-service
docker compose up -d --no-deps queue-service
docker compose logs --tail 50 queue-service
```

You can also build its image independently: `docker build -t cinemasaar/queue-service:latest ./queue`. Use Compose to supply database configuration and networking when running it.

## 8. Stop, restart, or reset

```powershell
docker compose stop
docker compose start
```

Remove containers and the network while preserving database data:

```powershell
docker compose down
```

For a fresh database, `docker compose down -v` also **deletes the stack's MongoDB data**. Then run `docker compose up -d` again.

## Troubleshooting

- **Port already allocated:** stop the conflicting container/process or change the host side of the relevant port mapping.
- **Connection refused immediately after startup:** inspect logs and wait for the application's `Started` message.
- **Restarting container:** use `docker compose logs --tail 100 <service>` to inspect the startup failure.
- **Mongo initialization failure:** inspect `docker compose logs mongo-init mongo` before starting dependent services.
- Run these commands from the repository root: `queue/docker-compose.yml` is a separate Mongo-only setup for host-based queue development.
