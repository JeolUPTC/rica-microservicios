# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Overview

RICA is a university microservices workshop ("taller") built step by step; each commit is labeled "Paso N" (Step N). Code, identifiers, comments, log messages and commit messages are in **Spanish**. Keep new code in Spanish to match.

There is **no parent POM**. The repo holds four independent Spring Boot 4.1 / Spring Cloud 2025.1 / Java 25 Maven projects, each with its own `mvnw`:

| Module | Port(s) | Storage |
|---|---|---|
| `eureka-server` | 8761 | — |
| `gateway` (Spring Cloud Gateway, WebFlux) | 8000 | — |
| `investigadores-service` | HTTP 8080, gRPC 9090 | PostgreSQL (Flyway) |
| `publicaciones-service` | HTTP 8081 | MongoDB |

## Commands

Run Maven from inside a module directory (on Windows use `.\mvnw.cmd`):

```bash
./mvnw clean verify                          # build + tests
./mvnw test -Dtest=InvestigadorServiceTest   # single test class
./mvnw test -Dtest=InvestigadorServiceTest#metodo   # single test method
./mvnw spring-boot:run
```

`protobuf-maven-plugin` generates gRPC stubs from `src/main/proto` during compile; run a compile after editing `.proto` files.

Infrastructure (Docker):
- `investigadores-service/docker-compose.yml`: PostgreSQL 16 (`rica`/`rica`/`rica` on 5432) **and the Kafka broker** (KRaft, 9092). Both services need this one for Kafka.
- `publicaciones-service/docker-compose.yml`: MongoDB 7 on 27017.

Startup order: both compose stacks → `eureka-server` → `investigadores-service` → `publicaciones-service` → `gateway`.

Manual API requests are in the Bruno collection `bruno/RICA-Microservicios` (the `local` environment calls the services directly on 8080/8081, not through the gateway). Swagger UI comes from springdoc in both services.

Tests are plain JUnit 5 + Mockito + AssertJ unit tests on the application layer (no Spring context, no Testcontainers).

## Architecture

### investigadores-service: hexagonal layout
Under `investigadores/`:
- `dominio/`: `Investigador` aggregate (with `EstadoInvestigador`), `CorreoInstitucional` value object, domain exceptions.
- `aplicacion/`: `InvestigadorUseCase` (input port) implemented by `InvestigadorService`, plus the output ports `RepositorioInvestigadores` and `PublicadorEventos`, and `InvestigadorFactory`.
- `infraestructura/entrada/{web,grpc,mensajeria}` and `infraestructura/salida/{persistencia,mensajeria}`: adapters.

The schema is owned by Flyway (`db/migration/V*__*.sql`) and JPA runs with `ddl-auto: validate`. Any entity change needs a new migration.

### publicaciones-service: flat layout
Everything lives in a single `publicaciones/` package (controller, service, Mongo repository, listeners, gRPC client). It is deliberately not hexagonal, but it still uses a `VerificadorInvestigador` interface in front of the gRPC adapter.

Both services share a `compartido/` package (`GlobalExceptionHandler`, `RecursoNoEncontradoException`) and a `plataforma/StatusController`.

### Inter-service communication
- **Synchronous gRPC** (publicaciones → investigadores): `investigador.proto` is **duplicated** in both services and differs only in `java_package`. Keep the two copies in sync. The client is registered with `@ImportGrpcClients(target = "investigadores", ...)`, and the channel target is a static `localhost:9090` (it does not go through Eureka). `VerificadorInvestigadorGrpc` wraps the call in a programmatic Resilience4j `CircuitBreaker` with a 5s deadline and maps failures to `ServicioNoDisponibleException`.
- **Asynchronous Kafka** (String payloads with JSON bodies, key = the researcher's institutional email):
  - investigadores-service publishes only through the **Transactional Outbox**. `PublicadorEventos.publicar(...)` (implemented by `OutboxPublicadorEventos`) writes a row to the outbox table inside the caller's `@Transactional`. `OutboxPoller` (`@Scheduled`, every 2s, batches of 50) then sends pending rows to Kafka and marks them as published. Do not call `KafkaTemplate` directly from this service.
  - publicaciones-service publishes directly with `KafkaTemplate`.
- **Researcher deletion saga** (choreography):
  1. `DELETE` → `InvestigadorService.iniciarEliminacion` marks the researcher `EN_ELIMINACION` (idempotent) and emits `investigadores.en-eliminacion`.
  2. `InvestigadorEnEliminacionListener` (publicaciones) marks that researcher's publications as orphaned (`marcarComoHuerfana`) and emits `publicaciones.marcadas-huerfanas`.
  3. `PublicacionesMarcadasListener` (investigadores) calls `confirmarEliminacion`, which deletes the researcher only if it is still in `EN_ELIMINACION`.
- `investigadores.registrados` is emitted on registration. publicaciones-service only logs it.

### Discovery and gateway
All services register in Eureka with `prefer-ip-address: true`. The gateway routes `/api/investigadores/**` → `lb://investigadores-service` and `/api/publicaciones/**` → `lb://publicaciones-service` (the route config is under `spring.cloud.gateway.server.webflux.routes`).
