# EventBus v2

EventBus v2 is a Spring Boot reference implementation of the PlantUML specification provided in the prompt. It showcases a
relational event bus that couples producers and consumers inside a single JVM while preserving full observability, idempotency, and
retry semantics.

## Features

- **Database schema** matching the provided diagram (`system_workers`, `workers_event`, `event_consumption`, `event_metrics`).
- **Pluggable workers** implemented in Java classes (`FtpDownloadProducer`, `CsvProcessorConsumer`) backed by the reusable
  `BaseEventProducer` and `BaseEventConsumer` abstractions.
- **Worker registry and scheduler** that automatically discover, register, and execute consumers.
- **Idempotency tracking** with deterministic hashes stored in the `event_consumption` table.
- **Metrics aggregation** with per-day statistics written to `event_metrics`.

## Project structure

```
src/main/java/com/example/eventbus
├── config
│   └── EventBusConfiguration.java     # Registers example workers
├── domain                             # JPA entities and enums
├── dto                                # Value objects shared by services and workers
├── scheduler                          # EventBusScheduler orchestrating consumption
├── service                            # Core service interfaces
├── service/impl                       # Concrete implementations (event bus, metrics, idempotency, registry)
└── worker                             # Base classes and example producer/consumer
```

## Getting started

### Prerequisites

- Java 17+
- Maven 3.9+
- MySQL 8.x (for production use)

### Database schema

The JPA entities mirror the schema described in the specification. DDL compatible with MySQL 8 is included below:

- [`SystemWorker`](src/main/java/com/example/eventbus/domain/SystemWorker.java)
- [`WorkersEvent`](src/main/java/com/example/eventbus/domain/WorkersEvent.java)
- [`EventConsumption`](src/main/java/com/example/eventbus/domain/EventConsumption.java)
- [`EventMetrics`](src/main/java/com/example/eventbus/domain/EventMetrics.java)

By default the project starts with an in-memory H2 database (configured in
[`application.properties`](src/main/resources/application.properties)) to simplify local experimentation. Override the datasource
properties to point at MySQL for real deployments.

```properties
spring.datasource.url=jdbc:mysql://localhost:3306/eventbus?useSSL=false&serverTimezone=UTC
spring.datasource.username=eventbus
spring.datasource.password=secret
spring.jpa.hibernate.ddl-auto=update
spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.MySQL8Dialect
```

### Running the application

```bash
mvn spring-boot:run
```

When the application starts it automatically registers the FTP producer and CSV consumer, then schedules consumption with the
`EventBusScheduler`. Toggle scheduling behaviour with the `eventbus.scheduler.*` properties.

### Example usage

```java
@Autowired
private FtpDownloadProducer ftpDownloadProducer;

@Autowired
private CsvProcessorConsumer csvProcessorConsumer;

@Autowired
private IEventBusService eventBusService;

@Autowired
private IIdempotencyService idempotencyService;

public void demo() {
    // Produce events
    ftpDownloadProducer.triggerDirectoryScan();

    // Poll + consume manually (scheduler does this automatically)
    List<WorkersEvent> events = eventBusService.pollPendingEvents(FtpDownloadProducer.EVENT_TYPE, 10);
    for (WorkersEvent event : events) {
        if (eventBusService.markEventProcessing(event.getEventUuid(), csvProcessorConsumer.getConsumerWorker().getId())) {
            ConsumptionResult result = csvProcessorConsumer.consume(event);
            if (result.isSuccess()) {
                String hash = idempotencyService.calculateResultHash(result.getResultData());
                eventBusService.markEventSuccess(event.getEventUuid(),
                        csvProcessorConsumer.getConsumerWorker().getId(), hash);
            }
        }
    }
}
```

The integration test [`EventBusIntegrationTest`](src/test/java/com/example/eventbus/EventBusIntegrationTest.java) demonstrates the
full lifecycle: publishing an event, locking it for consumption, processing it through the CSV consumer, and marking it successful.

### Testing

```bash
mvn test
```

> **Note:** The execution environment used for automated evaluation does not expose Maven Central, so the command above fails with a
> `403 Forbidden` while downloading dependencies. The build succeeds in a standard environment with network access.

## Configuration reference

| Property | Default | Description |
|----------|---------|-------------|
| `eventbus.scheduler.enabled` | `true` | Master switch for the scheduled poller |
| `eventbus.scheduler.fixed-delay` | `5000` | Delay (ms) between polling cycles |
| `eventbus.scheduler.poll-batch-size` | `50` | Maximum events fetched per cycle |
| `eventbus.scheduler.thread-pool-size` | `4` | Size of the executor used for consumption |

## Further work

- Implement additional consumers for more event types (the registry supports multiple per type).
- Add REST endpoints for manual replay/archival or to expose aggregated metrics.
- Swap H2 for MySQL in local development to validate production-like behaviour.
