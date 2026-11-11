# discord-api

`discord-api` is a Java 21 and Quarkus application under active development. Its current
codebase provides the Discord domain, persistence model, API contracts, mapping layer,
validation, and automated tests for users, guilds, roles, and channels.

> **Current status:** Discord CRUD REST endpoints and application services are not
> implemented yet. Their implementation starts with ticket #17. Swagger and OpenAPI are
> available for the Quarkus application, but they do not currently publish Discord CRUD
> operations.

The Maven artifact ID and container image name are `discord-api`. Java sources use the
canonical `org.acme.discord` package. The database name (`products`) remains a historical
identifier and is intentionally unchanged.

## Implemented foundation

- Domain records: `User`, `Guild`, `Role`, `Channel`, and `ChannelType`.
- JPA entities and Panache repositories backed by PostgreSQL.
- Separate create, full-update, and response DTO records.
- Dedicated mappers between requests, domain records, persistence entities, and response
  DTOs.
- Jakarta Bean Validation on incoming DTOs and persistence fields.
- Generic exception mappers already retained for the future REST layer.
- Unit tests for DTO validation and mapping.
- ArchUnit rules for domain isolation, layer dependencies, statelessness, and naming.
- PostgreSQL integration tests using Testcontainers.
- Technical integration tests for Health and OpenAPI.

## Project layout

```text
src/main/java/org/acme/discord/
├── api/error/                 Generic exception mappers; no Discord resources yet
├── application/dto/           Response, create, and update DTO records
├── domain/                    Framework-independent Discord records
└── infrastructure/
    ├── mapper/                User, Guild, Role, and Channel mappers
    └── persistence/           JPA entities and Panache repositories
```

## Prerequisites

- Java 21
- Docker, for PostgreSQL and Testcontainers
- The Maven wrapper included in this repository

## Run locally

Start only the PostgreSQL service declared in `compose.yml`:

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

Available technical endpoints:

| Purpose | URL |
|---|---|
| Swagger UI | <http://localhost:8080/swagger-ui> |
| OpenAPI document | <http://localhost:8080/q/openapi> |
| Aggregate health | <http://localhost:8080/q/health> |
| Liveness | <http://localhost:8080/q/health/live> |
| Readiness | <http://localhost:8080/q/health/ready> |
| Health UI | <http://localhost:8080/q/health-ui/> |
| Quarkus dev UI | <http://localhost:8080/q/dev-ui> |

There is no bundled HTTP request file because no Discord REST resource exists yet.

## Tests

No persistent project database is used by the integration tests. Testcontainers starts an
ephemeral PostgreSQL container for the test run.

```bash
./mvnw clean verify
```

| Test type | Location | Runner |
|---|---|---|
| DTO and mapper unit tests | `src/test/java/org/acme/discord/` | Surefire |
| Architecture rules | `src/test/java/org/acme/architecture/` | Surefire + ArchUnit |
| PostgreSQL mappings | `DiscordPersistenceIT.java` | Failsafe + Testcontainers |
| Health and OpenAPI | `TechnicalSpecsIT.java` | Failsafe + REST Assured |

## Documentation

The source entry point is [`docs/index.adoc`](docs/index.adoc). Generate HTML and PDF with:

```bash
./mvnw verify -Pdocs
```

Generated files:

- `target/living-docs/html/index.html`
- `target/living-docs/pdf/index.pdf`

See the [documentation guide](docs/README.md) for the source layout.

## Container images

The existing Maven profiles build the canonical `brunoe/discord-api` image name:

```bash
./mvnw package -Pjvm -DskipTests
./mvnw package -Pnative -DskipTests
```

The JVM profile produces tag `1.0.0-jvm`, which is also the default `IMAGE_TAG` used by
Compose. The native profile produces `1.0.0-native`. `IMAGE_GROUP=brunoe` remains
unchanged pending verification.
