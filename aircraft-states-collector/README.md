# Aircraft States Collector

A Spring Boot scheduler service that periodically fetches aircraft state vectors from the [OpenSky Network](https://opensky-network.org/) REST API and publishes them to Apache Kafka as Avro-serialized messages.

## Tech Stack

- Java 21
- Spring Boot 3.4.x
- Spring Scheduler
- Spring Security OAuth2 Client
- Spring Kafka
- Apache Avro
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
  kafka:
    bootstrap-servers: ${KAFKA_BOOTSTRAP_SERVERS:localhost:9092}
    producer:
      key-serializer: org.apache.kafka.common.serialization.StringSerializer
      value-serializer: org.apache.kafka.common.serialization.ByteArraySerializer
      acks: all
      retries: 3
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

aviation:
  kafka:
    topic: aircraft.state.vectors
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
mvn test -Dtest=StateVectorKafkaPublisherTest
```

### Integration Tests (with Embedded Kafka)

```bash
mvn test -Dtest=StateVectorKafkaIntegrationTest
```

### Cucumber BDD Tests

Run all Cucumber scenarios:

```bash
mvn test -Dtest=CucumberTestRunner
```

Feature files are located at `src/test/resources/features/`.

> **Note:** `mvn test -Dtest=CucumberTestRunner` may report `Tests run: 0` even though all scenarios execute and pass. This is a known Surefire + `cucumber-junit-platform-engine` counting quirk — the `@Suite` class has no `@Test` methods, and Surefire does not count dynamic tests from the Cucumber engine. Verify results by looking at the `Scenario:` output or the generated `target/cucumber-report.html`.

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

#### Option A: Run Kafka + App together

Start a Kafka broker container:

```bash
podman run -d \
  --name kafka \
  -p 9092:9092 \
  -e KAFKA_NODE_ID=1 \
  -e KAFKA_PROCESS_ROLES=broker,controller \
  -e KAFKA_LISTENERS=PLAINTEXT://:9092,CONTROLLER://:9093 \
  -e KAFKA_ADVERTISED_LISTENERS=PLAINTEXT://localhost:9092 \
  -e KAFKA_CONTROLLER_LISTENER_NAMES=CONTROLLER \
  -e KAFKA_LISTENER_SECURITY_PROTOCOL_MAP=CONTROLLER:PLAINTEXT,PLAINTEXT:PLAINTEXT \
  -e KAFKA_CONTROLLER_QUORUM_VOTERS=1@localhost:9093 \
  -e KAFKA_OFFSETS_TOPIC_REPLICATION_FACTOR=1 \
  docker.io/apache/kafka:latest
```

Build and run the application container:

```bash
mvn clean package -DskipTests

podman build -t aviation-scheduler:latest .

podman run -d \
  --name aviation-scheduler \
  -e OPENSKY_CLIENT_ID="your-client-id" \
  -e OPENSKY_CLIENT_SECRET="your-client-secret" \
  -e SPRING_PROFILES_ACTIVE=dev \
  -e KAFKA_BOOTSTRAP_SERVERS=kafka:9092 \
  --network slirp4netns:allow_host_loopback=true \
  aviation-scheduler:latest
```

The `dev` profile sets the fetch interval to **30 seconds** (instead of 5 minutes) so you can quickly verify logs.

#### Verify logs

Tail the application logs:

```bash
podman logs -f aviation-scheduler
```

Expected output:

```
Fetched 472 state vectors from OpenSky API (time=1715974800)
OpenSky API credits remaining: 399
Published state vectors to Kafka topic aircraft.state.vectors with key 1715974800 at offset 0
```

Verify messages in Kafka:

```bash
podman exec -it kafka \
  /opt/kafka/bin/kafka-console-consumer.sh \
  --bootstrap-server localhost:9092 \
  --topic aircraft.state.vectors \
  --from-beginning \
  --property print.key=true
```

#### Stop and cleanup

```bash
podman stop aviation-scheduler kafka
podman rm aviation-scheduler kafka
```

## API Endpoints Consumed

| Endpoint | Description |
|----------|-------------|
| `GET /states/all` | Retrieves all current aircraft state vectors |

## Architecture

```
┌─────────────────┐     ┌──────────────────────┐     ┌─────────────────┐
│  @Scheduled     │────▶│  StateVectorFetcher  │────▶│  OpenSky API    │
│  Job (5 min)    │     │     Service          │     │  /states/all    │
└─────────────────┘     └──────────────────────┘     └─────────────────┘
                               │
                               ▼
                        ┌──────────────────────┐
                        │ StateVectorKafka     │
                        │    Publisher         │
                        └──────────────────────┘
                               │
                               ▼
                        ┌──────────────────────┐
                        │  KafkaTemplate       │
                        │  (key=responseTime,  │
                        │   value=Avro bytes)  │
                        └──────────────────────┘
                               │
                               ▼
                        ┌──────────────────────┐
                        │ Topic: aircraft.state│
                        │      .vectors        │
                        │  (1 partition)       │
                        └──────────────────────┘
```

Key components:
- `StateVectorScheduler` — Triggers fetch at configured intervals
- `StateVectorFetcherService` — Orchestrates fetch, logging, and Kafka publishing
- `OpenSkyApiClient` — Handles HTTP calls and JSON deserialization
- `StateVectorKafkaPublisher` — Serializes response to Avro and publishes to Kafka
- `OAuth2ClientConfig` — Configures Spring's `OAuth2AuthorizedClientManager` for automatic token refresh

## Kafka Topic Design

| Property | Value |
|----------|-------|
| Topic | `aircraft.state.vectors` |
| Partitions | 1 |
| Key | Response timestamp (e.g., `"1715974800"`) |
| Value | Avro-serialized `StateVectorResponseAvro` bytes |
| Serialization | Apache Avro binary (no Schema Registry required) |

## Avro Schema

The full API response is published as a single Avro message:

```
StateVectorResponseAvro
├── time: long
└── states: array<StateVectorAvro>
    ├── icao24: string
    ├── callsign: string?
    ├── originCountry: string
    ├── timePosition: int?
    ├── lastContact: int?
    ├── longitude: double?
    ├── latitude: double?
    ├── baroAltitude: double?
    ├── onGround: boolean?
    ├── velocity: double?
    ├── trueTrack: double?
    ├── verticalRate: double?
    ├── sensors: array<int>?
    ├── geoAltitude: double?
    ├── squawk: string?
    ├── spi: boolean?
    ├── positionSource: int?
    └── category: int?
```

Schema files are located at `src/main/avro/` and Java classes are auto-generated by the `avro-maven-plugin` during the `generate-sources` phase.

## Domain Model

The OpenSky API returns state vectors as a 2D JSON array. The service maps these to typed Java records:

- `StateVectorResponse` — Container with `time` and `states`
- `StateVector` — Individual aircraft state with 18 fields (icao24, callsign, position, velocity, etc.)

## License

MIT
