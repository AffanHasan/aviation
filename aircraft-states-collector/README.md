# Aircraft States Collector

A Spring Boot scheduler service that periodically fetches aircraft state vectors from the [OpenSky Network](https://opensky-network.org/) REST API using synchronous HTTP calls with OAuth2 client credentials authentication.

## Tech Stack

- Java 21
- Spring Boot 3.4.x
- Spring Scheduler
- Spring Security OAuth2 Client
- RestTemplate
- Cucumber (BDD)
- JUnit 5 + Mockito (TDD)
- Podman (containerization)

## Prerequisites

- JDK 21
- Maven 3.9+
- Podman 5.x
- OpenSky Network account with API credentials ([Get credentials](https://opensky-network.org/))

## Configuration

The service is configured via `application.yml`:

```yaml
opensky:
  base-url: https://opensky-network.org/api
  interval-ms: 300000          # Fetch interval (5 minutes in production)
  timeouts:
    connect-ms: 5000
    read-ms: 30000

spring:
  security:
    oauth2:
      client:
        registration:
          opensky:
            client-id: ${OPENSKY_CLIENT_ID}
            client-secret: ${OPENSKY_CLIENT_SECRET}
            authorization-grant-type: client_credentials
        provider:
          opensky:
            token-uri: https://auth.opensky-network.org/auth/realms/opensky-network/protocol/openid-connect/token
```

Set your credentials as environment variables:

```bash
export OPENSKY_CLIENT_ID="your-client-id"
export OPENSKY_CLIENT_SECRET="your-client-secret"
```

## Running Tests

### Unit Tests

Run all unit tests:

```bash
mvn test
```

Run a specific test class:

```bash
mvn test -Dtest=OpenSkyApiClientTest
mvn test -Dtest=StateVectorSchedulerTest
```

### Cucumber BDD Tests

Run all Cucumber scenarios:

```bash
mvn test -Dtest=CucumberTestRunner
```

Feature files are located at `src/test/resources/features/`.

## Running Locally

### With Maven

Pass credentials as JVM arguments via the Spring Boot Maven plugin:

```bash
mvn spring-boot:run \
  -Dspring-boot.run.jvmArguments="-DOPENSKY_CLIENT_ID=your-client-id -DOPENSKY_CLIENT_SECRET=your-client-secret"
```

Or with the dev profile (30-second fetch interval + DEBUG logging):

```bash
mvn spring-boot:run \
  -Dspring-boot.run.jvmArguments="-DOPENSKY_CLIENT_ID=your-client-id -DOPENSKY_CLIENT_SECRET=your-client-secret" \
  -Dspring-boot.run.profiles=dev
```

Alternatively, set them as shell environment variables before running:

```bash
export OPENSKY_CLIENT_ID="your-client-id"
export OPENSKY_CLIENT_SECRET="your-client-secret"
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

### With Podman

#### 1. Build the JAR

```bash
mvn clean package -DskipTests
```

#### 2. Build the container image

```bash
podman build -t aviation-scheduler:latest .
```

#### 3. Run the container

```bash
podman run -d \
  --name aviation-scheduler \
  -e OPENSKY_CLIENT_ID="your-client-id" \
  -e OPENSKY_CLIENT_SECRET="your-client-secret" \
  -e SPRING_PROFILES_ACTIVE=dev \
  aviation-scheduler:latest
```

The `dev` profile sets the fetch interval to **30 seconds** (instead of 5 minutes) so you can quickly verify logs.

#### 4. Verify logs

Tail the container logs:

```bash
podman logs -f aviation-scheduler
```

Expected output:

```
Fetched 472 state vectors from OpenSky API (time=1715974800)
OpenSky API credits remaining: 399
```

#### 5. Stop and cleanup

```bash
podman stop aviation-scheduler
podman rm aviation-scheduler
```

## API Endpoints Consumed

| Endpoint | Description |
|----------|-------------|
| `GET /states/all` | Retrieves all current aircraft state vectors |

## Architecture

```
┌─────────────────┐     ┌──────────────────┐     ┌─────────────────┐
│  @Scheduled     │────▶│  StateVector     │────▶│  OpenSky API    │
│  Job (5 min)    │     │  Fetcher Service │     │  /states/all    │
└─────────────────┘     └──────────────────┘     └─────────────────┘
                               │
                               ▼
                        ┌──────────────────┐
│  Spring OAuth2    │
│  Client Manager   │
└──────────────────┘
```

Key components:
- `StateVectorScheduler` — Triggers fetch at configured intervals
- `StateVectorFetcherService` — Orchestrates fetch and logging
- `OpenSkyApiClient` — Handles HTTP calls and JSON deserialization
- `OAuth2ClientConfig` — Configures Spring's `OAuth2AuthorizedClientManager` for automatic token refresh

## Domain Model

The OpenSky API returns state vectors as a 2D JSON array. The service maps these to typed Java records:

- `StateVectorResponse` — Container with `time` and `states`
- `StateVector` — Individual aircraft state with 18 fields (icao24, callsign, position, velocity, etc.)

## License

MIT
