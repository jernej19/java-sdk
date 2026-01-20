# PandaScore Java SDK

## Overview

The PandaScore Java SDK enables customers to consume live esports odds over
RabbitMQ and to recover missed data through HTTP endpoints. It ships with typed
data models, an automatic reconnection handler and helper utilities so that a
customer can focus on processing events.

## Features

- **RabbitMQ feed integration** – connect to the PandaScore AMQPS feed and
  receive structured JSON events.
- **Automatic reconnection & recovery** – `EventHandler` monitors heartbeats and
  triggers `MatchesClient` recovery calls after a disconnection.
- **Typed DTOs** – the `model` package contains Java classes for every feed
  payload.
- **HTTP clients** – `MatchesClient` exposes typed methods for recovery of
  markets and matches.
- **Extensive logging** – MDC tags and asynchronous appenders capture detailed
  context for each operation.

## Requirements

- Java **17** or higher.
- Use the provided Gradle wrapper (`./gradlew`) for all commands.
- Ensure `JAVA_HOME` points to a JDK if the wrapper cannot locate `java` on your
  `PATH`.

## Getting Started

1. Clone the repository and edit credentials in
   `src/main/java/com/pandascore/sdk/examples/FeedConsole.java`.
2. Run the example:

   ```bash
   ./gradlew clean run
   ```

3. Observe console logs for feed messages, heartbeats and automatic recovery
   after any disconnection.

## Configuration

All runtime settings are provided through `SDKOptions`. Use the builder to
construct an instance and initialise the SDK once at application start:

```java
SDKOptions opts = SDKOptions.builder()
        .apiToken("your-api-token")
        .companyId(123)
        .email("you@example.com")
        .password("secret")
        .queueBinding(SDKOptions.QueueBinding.builder()
                .queueName("my-queue")
                .routingKey("#")
                .build())
        .alwaysLogPayload(true)
        .americanOdds(true)
        .fractionalOdds(true)
        .build();
SDKConfig.setOptions(opts);
```

`SDKOptions` fields:

- `apiToken` – token for REST requests.
- `companyId` – numeric identifier of your PandaScore account.
- `email` / `password` – credentials used to connect to RabbitMQ.
- `feedHost` – RabbitMQ host.
- `apiBaseUrl` – base URL for the recovery API.
- `queueBindings` – list of queue/routing‑key pairs to declare.
- `alwaysLogPayload` – log payloads at INFO level when `true`.
- `americanOdds` / `fractionalOdds` – compute additional odds representations.

## Consuming the Feed

Create an `EventHandler` to track connectivity and a `RabbitMQFeed` to receive
messages:

```java
EventHandler handler = new EventHandler(evt -> {
    if ("disconnection".equals(evt)) {
        // notify your application
    } else if ("reconnection".equals(evt)) {
        // resume normal processing
    }
});

RabbitMQFeed feed = new RabbitMQFeed(handler);
feed.connect(json -> {
    // json is a Jackson JsonNode; convert to the desired DTO
});
```

`connect` starts consumers on all configured queues and automatically retries
with exponential back‑off if the connection drops. Incoming messages are passed
to the provided `Consumer<Object>` as Jackson `JsonNode` instances.

## HTTP Recovery

When a disconnection occurs, the SDK can recover missed data using the typed
methods in `MatchesClient`:

- `List<MarketsRecoveryMatch> recoverMarkets(String since)` – return markets
  updated after the given ISO‑8601 timestamp.
- `List<FixtureMatch> fetchMatchesRange(String start, String end)` – return
  matches modified between the two timestamps.

Example usage:

```java
List<MarketsRecoveryMatch> markets =
    MatchesClient.recoverMarkets("2025-05-22T14:00:00Z");
List<FixtureMatch> matches =
    MatchesClient.fetchMatchesRange("2025-05-22T14:00:00Z", "2025-05-22T15:00:00Z");
```

## Logging

`logback.xml` defines asynchronous file appenders:
`sdk-debug.log`, `sdk-info.log` and `sdk-warn.log`. Each file only accepts
entries at its log level so information is not duplicated. Console and file
appenders are wrapped in `AsyncAppender` so feed threads are never blocked.

Log entries include MDC tags controlled by the `sdk.mdc.pattern` system
property. The default pattern is:

```text
[session=%X{session}] [customerId=%X{customerId}] [feed=%X{feed}] [messageType=%X{messageType}] [operation=%X{operation}] [routingKey=%X{routingKey}]
```

Provide a custom value via `-Dsdk.mdc.pattern=...` to remove or reorder tags.

## Building and Javadoc

Compile and run the example:

```bash
./gradlew build
```

Generate API documentation:

```bash
./gradlew javadoc
```

HTML files are written to `build/docs/javadoc`. A Javadoc JAR can be produced
with:

```bash
./gradlew javadocJar
```

## Example

`FeedConsole` demonstrates end‑to‑end usage, from configuration to message
handling. Refer to the Javadoc for classes under `com.pandascore.sdk.model.feed`
for full details of each payload structure.

