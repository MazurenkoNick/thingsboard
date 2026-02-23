# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

ThingsBoard is an open-source IoT platform (v4.3.0-RC). Java 17, Spring Boot 3.4, Maven multi-module project. Apache 2.0 licensed.

## Build Commands

```bash
# Full build (skip tests for speed)
mvn clean install -DskipTests

# Full build with tests
mvn clean install

# Build specific module (e.g., application)
mvn clean install -f application/pom.xml -DskipTests

# Build only protobuf modules
./build_proto.sh

# Format license headers (required before commits)
mvn -T 1C license:format

# Frontend (from ui-ngx/)
cd ui-ngx && yarn install
yarn start          # Dev server with hot reload at localhost:4200
yarn build:prod     # Production build
yarn lint           # ESLint
```

## Testing

```bash
# Run all tests in a module
mvn test -f application/pom.xml

# Run a single test class
mvn test -f application/pom.xml -Dtest=DeviceControllerTest

# Run a single test method
mvn test -f application/pom.xml -Dtest=DeviceControllerTest#testFindDeviceById
```

Tests use the `test` Spring profile. Most transports and edges are disabled by default in `application/src/test/resources/application-test.properties` for faster context startup. Individual tests enable what they need via `@TestPropertySource`.

**Test base class hierarchy:**
`AbstractControllerTest` → `AbstractNotifyEntityTest` → `AbstractWebTest` → `AbstractInMemoryStorageTest`

Controller/API tests extend `AbstractControllerTest`. Transport integration tests extend `AbstractTransportIntegrationTest` (which also extends `AbstractControllerTest`). Tests use TestContainers for PostgreSQL.

## Architecture

### Module Structure

- **`application/`** — Main Spring Boot server. Contains REST controllers (`controller/`), service implementations (`service/`), and the actor system (`actors/`).
- **`common/`** — Shared libraries: `data/` (domain model), `dao-api/` (DAO interfaces), `queue/` (message queue abstraction), `transport/` (transport API), `actor/` (actor framework), `proto/` (protobuf definitions), `message/` (internal messages), `cluster-api/`, `edge-api/`, `agent-api/`.
- **`dao/`** — Data access layer. JPA/SQL implementations of DAO interfaces from `common/dao-api/`.
- **`rule-engine/`** — `rule-engine-api/` (interfaces) and `rule-engine-components/` (built-in rule nodes).
- **`transport/`** — Protocol-specific transport implementations: `mqtt/`, `http/`, `coap/`, `lwm2m/`, `snmp/`.
- **`ui-ngx/`** — Angular 18 frontend (component prefix: `tb-`, path aliases: `@app/`, `@core/`, `@modules/`, `@shared/`, `@home/`).
- **`msa/`** — Microservices packaging: Docker images, JS executor, monitoring, black-box tests.
- **`edqs/`** — Entity Data Query Service.

### Actor System

Custom actor framework in `common/actor/`. The hierarchy: **AppActor** → **TenantActor** → **DeviceActor** / **RuleChainActor** → **RuleNodeActor**. Actors process messages asynchronously via mailboxes and dispatchers. `ActorSystemContext` (in `application/`) provides shared dependencies to all actors.

### Data Flow

1. Device data arrives via transports (MQTT, HTTP, CoAP, etc.)
2. Transport layer converts to protobuf messages and publishes to queues
3. Rule engine processes messages through configurable rule chains (chains of rule nodes)
4. Rule nodes can transform data, trigger alarms, save telemetry, send notifications, etc.
5. Data is persisted via the DAO layer to PostgreSQL (and optionally Cassandra for timeseries)

### Multi-Tenancy

Tenant isolation is enforced at every layer. Entities belong to tenants. Each tenant has its own actor subtree, rule chains, and data partition.

### Configuration

Main config: `application/src/main/resources/thingsboard.yml`. Uses environment variable substitution with defaults: `"${ENV_VAR:default_value}"`.

### DAO Pattern

Interfaces in `common/dao-api/`, SQL implementations in `dao/`. Uses Spring Data JPA with custom repositories. Entities have JPA `*Entity` classes (in `dao/`) mapped to domain model POJOs (in `common/data/`).

### Protobuf/gRPC

Proto definitions in `common/proto/src/main/proto/`. Used for inter-service communication (transport API, queue messages). Run `./build_proto.sh` after modifying `.proto` files.

## Conventions

- **License header** required on all source files. Run `mvn -T 1C license:format` to auto-format. Template: `license-header-template.txt`.
- **Lombok** is used extensively (`@Slf4j`, `@Data`, `@Builder`, etc.).
- Tests use JUnit 4 runner (`@RunWith(SpringRunner.class)`) with Spring Boot Test and AssertJ assertions.
- Frontend Angular components use `tb-` prefix.
