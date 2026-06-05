# state-collector

Quarkus-based scheduled service that periodically fetches aircraft state vectors from the [OpenSky Network](https://opensky-network.org/) API and publishes them to an Apache Kafka topic as Avro-serialized messages.

## Prerequisites

- JDK 21+
- Apache Maven 3.9+
- OpenSky Network API credentials (for dev/prod modes)

### OpenSky API Credentials

This application uses OAuth2 client-credentials to authenticate with OpenSky. Register for a free account at [OpenSky Network](https://opensky-network.org/) and create an API client to obtain your `client_id` and `client_secret`.

```bash
export OPENSKY_CLIENT_ID=<your_client_id>
export OPENSKY_CLIENT_SECRET=<your_client_secret>
```

## Running the application in dev mode

Dev mode uses **real** OpenSky API endpoints and automatically starts **Kafka Dev Services** locally:

```bash
./mvnw quarkus:dev
```

> **Dev Services that start automatically:**
> - **Kafka** — a local Kafka broker is started on a random port (via `quarkus-messaging-kafka`)
>
> **Disabled in dev mode:**
> - **WireMock** — the application calls the real OpenSky API instead of mocks

> **_NOTE:_**  Quarkus ships with a Dev UI available in dev mode at <http://localhost:8080/q/dev/>.

## Configuration Profiles

| Profile | REST Client | Auth Server | Kafka Connector | WireMock |
|---------|-------------|-------------|-----------------|----------|
| `%dev`  | Real OpenSky | Real OpenSky | `smallrye-kafka` + Dev Services | Disabled |
| `%prod` | Real OpenSky | Real OpenSky | `smallrye-kafka` (external broker) | N/A |
| `%test` | WireMock mock | WireMock mock | `smallrye-in-memory` | Enabled |

## Running the BDD Test Suite

This project follows BDD practices using Cucumber. All scenarios must pass before any production code is considered complete:

```bash
./mvnw test -Dtest=CucumberTest
```

The test suite covers:
- Fetching state vectors from OpenSky
- Handling 429 rate-limit responses gracefully
- OAuth2 Bearer token authentication
- Avro serialization and Kafka publishing

## Packaging and running the application

Package for production:

```bash
./mvnw package
```

Run the packaged application:

```bash
java -jar target/quarkus-app/quarkus-run.jar
```

Build an über-jar:

```bash
./mvnw package -Dquarkus.package.jar.type=uber-jar
java -jar target/*-runner.jar
```

## Creating a native executable

```bash
./mvnw package -Dnative
```

Or build in a container (no GraalVM required):

```bash
./mvnw package -Dnative -Dquarkus.native.container-build=true
./target/state-collector-1.0.0-SNAPSHOT-runner
```

## Related Guides

- [Quarkus Scheduler](https://quarkus.io/guides/scheduler)
- [Quarkus REST Client](https://quarkus.io/guides/rest-client)
- [Quarkus Messaging Kafka](https://quarkus.io/guides/kafka)
- [Quarkus OIDC Client](https://quarkus.io/guides/security-openid-connect-client-reference)
- [Quarkus Cucumber](https://docs.quarkiverse.io/quarkus-cucumber/dev/index.html)
