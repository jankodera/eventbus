package com.example.eventbus.worker;

import com.example.eventbus.domain.SystemWorker;
import com.example.eventbus.domain.WorkersEvent;
import com.example.eventbus.dto.ConsumptionResult;
import com.example.eventbus.service.IEventBusService;
import com.example.eventbus.service.IEventConsumer;
import com.example.eventbus.service.IIdempotencyService;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public abstract class BaseEventConsumer extends BaseEventWorker implements IEventConsumer {

    private final List<String> supportedEventTypes = new ArrayList<>();
    private final Map<String, Integer> eventVersions = new HashMap<>();

    protected BaseEventConsumer(SystemWorker systemWorker,
                                IEventBusService eventBusService,
                                IIdempotencyService idempotencyService) {
        super(systemWorker, eventBusService, idempotencyService);
    }

    protected void registerEventType(String eventType, int version) {
        supportedEventTypes.add(eventType);
        eventVersions.put(eventType, version);
    }

    @Override
    public boolean canConsume(String eventType, Integer eventVersion) {
        return supportedEventTypes.contains(eventType)
            && eventVersions.getOrDefault(eventType, 1) >= eventVersion;
    }

    @Override
    public ConsumptionResult consume(WorkersEvent event) {
        String idempotencyKey = idempotencyService.generateIdempotencyKey(event.getEventUuid(), getWorkerId());
        if (idempotencyService.isAlreadyProcessed(idempotencyKey)) {
            return ConsumptionResult.success("Already processed");
        }

        try {
            long start = System.currentTimeMillis();
            ConsumptionResult result = processEvent(event);
            long end = System.currentTimeMillis();
            long duration = end - start;

            if (result.isSuccess()) {
                String resultHash = idempotencyService.calculateResultHash(result.getResultData());
                idempotencyService.recordProcessing(idempotencyKey, resultHash);
                afterSuccessfulProcessing(event, duration);
            } else if (result.isRetryable()) {
                afterRetryableFailure(event, result.getErrorMessage());
            } else {
                afterPermanentFailure(event, result.getErrorMessage());
            }

            return result;
        } catch (Exception ex) {
            handleProcessingError(event, ex);
            boolean retryable = isRetryable(ex);
            return ConsumptionResult.failure(ex.getMessage(), retryable);
        }
    }

    @Override
    public SystemWorker getConsumerWorker() {
        return getSystemWorker();
    }

    @Override
    public List<String> getSupportedEventTypes() {
        return List.copyOf(supportedEventTypes);
    }

    protected void afterSuccessfulProcessing(WorkersEvent event, long processingTimeMs) {
    }

    protected void afterRetryableFailure(WorkersEvent event, String message) {
    }

    protected void afterPermanentFailure(WorkersEvent event, String message) {
    }

    protected abstract ConsumptionResult processEvent(WorkersEvent event);

    protected void handleProcessingError(WorkersEvent event, Exception error) {
    }

    protected boolean isRetryable(Exception error) {
        return true;
    }
}
