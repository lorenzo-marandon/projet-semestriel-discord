# discord-api

`discord-api` is a Discord-like REST backend built with Java 21, Quarkus 3.31.2,
Hibernate ORM with Panache, and PostgreSQL. It manages users, guilds, roles, channels,
guild membership, and user role assignments. Discord4J integration is not implemented.

The Maven artifact ID and container image name are `discord-api`. Java sources use the
`org.acme.discord` package. The database name `products` is a historical project setting.

## REST API

The API exposes CRUD operations below `/api/users`, `/api/guilds`, `/api/roles`, and
`/api/channels`. It also exposes these association operations:

- `PUT`, `DELETE`, and `GET /api/guilds/{guildId}/members[/{userId}]`
- `PUT`, `DELETE`, and `GET /api/users/{userId}/roles[/{roleId}]`

Association mutations are idempotent. A user must be a member of the role's guild before
the role can be assigned. Removing a guild member also removes that user's role assignments
for the guild while preserving assignments in other guilds.

All six list operations return a page object:

```json
{
  "items": [],
  "page": 0,
  "size": 20,
  "totalItems": 0,
  "totalPages": 0
}
```

`page` defaults to `0`; `size` defaults to `20` and must be between `1` and `100`.
Available filters are:

| Endpoint | Filters |
|---|---|
| `GET /api/users` | `username`: case-insensitive partial match |
| `GET /api/guilds` | `name`: case-insensitive partial match |
| `GET /api/roles` | `guildId`: exact UUID |
| `GET /api/channels` | `guildId`: exact UUID; `type`: `TEXT` or `VOICE` (combined with AND) |

Text filters trim surrounding spaces and treat `%` and `_` literally. Supplied empty
filters and invalid pagination, UUID, or enum values return `400 INVALID_REQUEST`.

API errors share the same safe JSON structure:

```json
{
  "code": "VALIDATION_ERROR",
  "message": "Request validation failed",
  "timestamp": "2026-01-15T10:45:00",
  "violations": [
    {"field": "username", "message": "Username is required"}
  ]
}
```

Documented application responses use `200`, `201`, `204`, `400`, `404`, `409`, and `500`
as applicable. Quarkus may return technical `405 Method Not Allowed` and `415 Unsupported
Media Type` responses; their bodies are not guaranteed to use the application error format.

Swagger contains the authoritative request and response schemas, constraints, examples,
status codes, pagination rules, filters, and business conflicts for all 26 operations.

## Prerequisites

- Java 21
- Docker, for local PostgreSQL and PostgreSQL Testcontainers
- The Maven wrapper included in this repository

## Run locally

Start PostgreSQL, then run Quarkus in development mode:

```bash
docker compose up -d postgres
DB_PORT=15432 ./mvnw quarkus:dev
```

PowerShell equivalent:

```powershell
docker compose up -d postgres
$env:DB_PORT = "15432"
.\mvnw.cmd quarkus:dev
```

Local endpoints:

| Purpose | URL |
|---|---|
| Swagger UI | <http://localhost:8080/swagger-ui/> |
| OpenAPI document (YAML by default, JSON with `Accept: application/json`) | <http://localhost:8080/q/openapi> |
| Aggregate health | <http://localhost:8080/q/health> |
| Liveness | <http://localhost:8080/q/health/live> |
| Readiness | <http://localhost:8080/q/health/ready> |
| Health UI | <http://localhost:8080/q/health-ui/> |
| Quarkus dev UI | <http://localhost:8080/q/dev-ui> |

## Tests

Integration tests exercise the complete HTTP-to-database chain against an ephemeral
PostgreSQL Testcontainers instance. They never use the development database. Docker must
be running before the full build.

```bash
./mvnw clean verify
```

On Windows:

```powershell
.\mvnw.cmd clean verify
```

| Test type | Runner |
|---|---|
| Unit and application tests | Surefire + JUnit 5 |
| Architecture rules | Surefire + ArchUnit |
| REST and persistence integration | Failsafe + QuarkusTest + REST Assured + Testcontainers |
| Generated OpenAPI contract | Failsafe + structural JSON assertions |

The generated OpenAPI files are written below `target/openapi-spec/` during a build and are
not committed.

## Project layout

```text
src/main/java/org/acme/discord/
|-- api/                    REST resources and HTTP exception mappers
|-- application/            Use cases, services, and API DTOs
|-- domain/                 Framework-independent domain records
`-- infrastructure/
    |-- mapper/             DTO/entity mapping
    `-- persistence/        JPA entities and Panache repositories
```

## Living documentation

The source entry point is [`docs/index.adoc`](docs/index.adoc). Generate HTML and PDF with:

```bash
./mvnw verify -Pdocs
```

Outputs are written to `target/living-docs/html/index.html` and
`target/living-docs/pdf/index.pdf`. See [docs/README.md](docs/README.md) for the source layout.

## Container images

The existing profiles build the configured `discord-api` image:

```bash
./mvnw package -Pjvm -DskipTests
./mvnw package -Pnative -DskipTests
```
