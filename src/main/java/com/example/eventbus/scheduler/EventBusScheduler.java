package com.example.eventbus.scheduler;

import com.example.eventbus.domain.WorkersEvent;
import com.example.eventbus.dto.ConsumptionResult;
import com.example.eventbus.service.IEventBusService;
import com.example.eventbus.service.IEventConsumer;
import com.example.eventbus.service.IIdempotencyService;
import com.example.eventbus.service.IWorkerRegistry;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import jakarta.annotation.PreDestroy;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class EventBusScheduler {

    private static final Logger LOGGER = LoggerFactory.getLogger(EventBusScheduler.class);

    private final IWorkerRegistry workerRegistry;
    private final IEventBusService eventBusService;
    private final IIdempotencyService idempotencyService;
    private final ExecutorService executorService;
    private final int pollBatchSize;
    private final boolean schedulerEnabled;

    public EventBusScheduler(IWorkerRegistry workerRegistry,
                             IEventBusService eventBusService,
                             IIdempotencyService idempotencyService,
                             @Value("${eventbus.scheduler.thread-pool-size:4}") int threadPoolSize,
                             @Value("${eventbus.scheduler.poll-batch-size:50}") int pollBatchSize,
                             @Value("${eventbus.scheduler.enabled:true}") boolean schedulerEnabled) {
        this.workerRegistry = workerRegistry;
        this.eventBusService = eventBusService;
        this.idempotencyService = idempotencyService;
        this.executorService = Executors.newFixedThreadPool(threadPoolSize);
        this.pollBatchSize = pollBatchSize;
        this.schedulerEnabled = schedulerEnabled;
    }

    @Scheduled(fixedDelayString = "${eventbus.scheduler.fixed-delay:5000}")
    public void processEvents() {
        if (!schedulerEnabled) {
            return;
        }
        List<IEventConsumer> consumers = workerRegistry.getAllConsumers();
        consumers.forEach(this::pollAndProcessEvents);
    }

    private void pollAndProcessEvents(IEventConsumer consumer) {
        consumer.getSupportedEventTypes().forEach(eventType -> {
            List<WorkersEvent> events = eventBusService.pollPendingEvents(eventType, pollBatchSize);
            events.forEach(event -> executorService.submit(() -> executeConsumption(event, consumer)));
        });
    }

    private void executeConsumption(WorkersEvent event, IEventConsumer consumer) {
        Long consumerWorkerId = consumer.getConsumerWorker().getId();
        if (!eventBusService.markEventProcessing(event.getEventUuid(), consumerWorkerId)) {
            return;
        }

        try {
            ConsumptionResult result = consumer.consume(event);
            if (result.isSuccess()) {
                String resultHash = idempotencyService.calculateResultHash(result.getResultData());
                eventBusService.markEventSuccess(event.getEventUuid(), consumerWorkerId, resultHash);
            } else {
                Exception error = new RuntimeException(result.getErrorMessage() != null
                    ? result.getErrorMessage()
                    : "Event consumption failed");
                eventBusService.markEventFailed(event.getEventUuid(), consumerWorkerId, error, result.isRetryable());
            }
        } catch (Exception ex) {
            LOGGER.error("Unexpected error consuming event {} by {}", event.getEventUuid(), consumer.getConsumerWorker().getWorkerName(), ex);
            eventBusService.markEventFailed(event.getEventUuid(), consumerWorkerId, ex, true);
        }
    }

    @PreDestroy
    public void shutdown() {
        executorService.shutdown();
        try {
            if (!executorService.awaitTermination(5, TimeUnit.SECONDS)) {
                executorService.shutdownNow();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            executorService.shutdownNow();
        }
    }
}
