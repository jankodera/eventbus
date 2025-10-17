package com.example.eventbus.service.impl;

import com.example.eventbus.domain.EventConsumption;
import com.example.eventbus.domain.EventStatus;
import com.example.eventbus.domain.SystemWorker;
import com.example.eventbus.domain.WorkersEvent;
import com.example.eventbus.domain.repository.EventConsumptionRepository;
import com.example.eventbus.domain.repository.SystemWorkerRepository;
import com.example.eventbus.domain.repository.WorkersEventRepository;
import com.example.eventbus.service.IEventBusService;
import com.example.eventbus.service.IIdempotencyService;
import com.example.eventbus.service.IMetricsService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EventBusServiceImpl implements IEventBusService {

    private static final Logger LOGGER = LoggerFactory.getLogger(EventBusServiceImpl.class);

    private final WorkersEventRepository workersEventRepository;
    private final EventConsumptionRepository eventConsumptionRepository;
    private final SystemWorkerRepository systemWorkerRepository;
    private final IMetricsService metricsService;
    private final IIdempotencyService idempotencyService;
    private final ObjectMapper objectMapper;

    public EventBusServiceImpl(WorkersEventRepository workersEventRepository,
                               EventConsumptionRepository eventConsumptionRepository,
                               SystemWorkerRepository systemWorkerRepository,
                               IMetricsService metricsService,
                               IIdempotencyService idempotencyService,
                               ObjectMapper objectMapper) {
        this.workersEventRepository = workersEventRepository;
        this.eventConsumptionRepository = eventConsumptionRepository;
        this.systemWorkerRepository = systemWorkerRepository;
        this.metricsService = metricsService;
        this.idempotencyService = idempotencyService;
        this.objectMapper = objectMapper;
    }

    @Override
    @Transactional
    public String publishEvent(Long producerWorkerId, String eventType, Object eventData) {
        return publishEvent(producerWorkerId, eventType, eventData, null);
    }

    @Override
    @Transactional
    public String publishEvent(Long producerWorkerId, String eventType, Object eventData, String correlationId) {
        String eventUuid = UUID.randomUUID().toString();
        WorkersEvent event = new WorkersEvent();
        event.setEventUuid(eventUuid);
        event.setEventType(eventType);
        event.setEventVersion(1);
        event.setEventData(writeValue(eventData));
        event.setCorrelationId(correlationId);
        event.setStatus(EventStatus.PENDING);
        if (producerWorkerId != null) {
            SystemWorker worker = systemWorkerRepository.findById(producerWorkerId)
                .orElseThrow(() -> new IllegalArgumentException("Producer worker not found: " + producerWorkerId));
            event.setProducerWorker(worker);
        }
        workersEventRepository.save(event);
        return eventUuid;
    }

    @Override
    @Transactional(readOnly = true)
    public List<WorkersEvent> pollPendingEvents(String eventType, int limit) {
        List<WorkersEvent> events = workersEventRepository
            .findTop100ByEventTypeAndStatusOrderByCreatedAtAsc(eventType, EventStatus.PENDING);
        if (events.size() > limit) {
            return new ArrayList<>(events.subList(0, limit));
        }
        return events;
    }

    @Override
    @Transactional
    public boolean markEventProcessing(String eventUuid, Long consumerWorkerId) {
        Optional<WorkersEvent> optional = workersEventRepository.findByEventUuid(eventUuid);
        if (optional.isEmpty()) {
            LOGGER.warn("Event {} not found when attempting to mark processing", eventUuid);
            return false;
        }

        WorkersEvent event = optional.get();
        if (event.getStatus() != EventStatus.PENDING && event.getStatus() != EventStatus.FAILED_RETRYABLE) {
            return false;
        }

        event.setStatus(EventStatus.PROCESSING);
        event.setProcessingStartedAt(Instant.now());
        workersEventRepository.save(event);

        Optional<EventConsumption> existingConsumption = eventConsumptionRepository
            .findByEventUuidAndConsumerWorker_Id(eventUuid, consumerWorkerId);

        if (existingConsumption.isEmpty()) {
            EventConsumption consumption = new EventConsumption();
            consumption.setEventUuid(eventUuid);
            SystemWorker worker = systemWorkerRepository.findById(consumerWorkerId)
                .orElseThrow(() -> new IllegalArgumentException("Consumer worker not found: " + consumerWorkerId));
            consumption.setConsumerWorker(worker);
            consumption.setStatus(EventStatus.PROCESSING);
            consumption.setAttemptNumber(1);
            consumption.setProcessingStartedAt(Instant.now());
            consumption.setIdempotencyKey(idempotencyService.generateIdempotencyKey(eventUuid, consumerWorkerId));
            eventConsumptionRepository.save(consumption);
            return true;
        }

        EventConsumption consumption = existingConsumption.get();
        if (consumption.getStatus() == EventStatus.PROCESSING) {
            return false;
        }

        consumption.setStatus(EventStatus.PROCESSING);
        consumption.setProcessingStartedAt(Instant.now());
        consumption.setAttemptNumber(consumption.getAttemptNumber() == null ? 1 : consumption.getAttemptNumber() + 1);
        if (consumption.getIdempotencyKey() == null) {
            consumption.setIdempotencyKey(idempotencyService.generateIdempotencyKey(eventUuid, consumerWorkerId));
        }
        eventConsumptionRepository.save(consumption);
        return true;
    }

    @Override
    @Transactional
    public void markEventSuccess(String eventUuid, Long consumerWorkerId, String resultHash) {
        WorkersEvent event = workersEventRepository.findByEventUuid(eventUuid)
            .orElseThrow(() -> new IllegalArgumentException("Event not found: " + eventUuid));

        event.setStatus(EventStatus.SUCCESS);
        event.setProcessedAt(Instant.now());
        event.setRetryCount(0);
        workersEventRepository.save(event);

        EventConsumption consumption = eventConsumptionRepository
            .findByEventUuidAndConsumerWorker_Id(eventUuid, consumerWorkerId)
            .orElseThrow(() -> new IllegalStateException("Consumption record missing for event: " + eventUuid));
        consumption.setStatus(EventStatus.SUCCESS);
        consumption.setCompletedAt(Instant.now());
        consumption.setResultHash(resultHash);
        eventConsumptionRepository.save(consumption);

        metricsService.recordEventProcessed(event.getEventType(), consumerWorkerId, EventStatus.SUCCESS,
            computeProcessingDuration(consumption));
    }

    @Override
    @Transactional
    public void markEventFailed(String eventUuid, Long consumerWorkerId, Exception error, boolean retryable) {
        WorkersEvent event = workersEventRepository.findByEventUuid(eventUuid)
            .orElseThrow(() -> new IllegalArgumentException("Event not found: " + eventUuid));
        EventStatus status = retryable ? EventStatus.FAILED_RETRYABLE : EventStatus.FAILED_PERMANENT;
        event.setStatus(status);
        event.setFailedAt(Instant.now());
        event.setLastErrorMessage(error.getMessage());
        event.setLastErrorStackTrace(getStackTrace(error));
        if (retryable) {
            event.setRetryCount(event.getRetryCount() + 1);
        }
        workersEventRepository.save(event);

        EventConsumption consumption = eventConsumptionRepository
            .findByEventUuidAndConsumerWorker_Id(eventUuid, consumerWorkerId)
            .orElseThrow(() -> new IllegalStateException("Consumption record missing for event: " + eventUuid));
        consumption.setStatus(status);
        consumption.setFailedAt(Instant.now());
        consumption.setCompletedAt(Instant.now());
        consumption.setErrorMessage(error.getMessage());
        consumption.setErrorStackTrace(getStackTrace(error));
        eventConsumptionRepository.save(consumption);

        metricsService.recordEventProcessed(event.getEventType(), consumerWorkerId, status,
            computeProcessingDuration(consumption));
    }

    @Override
    @Transactional
    public void replayEvent(String eventUuid) {
        WorkersEvent event = workersEventRepository.findByEventUuid(eventUuid)
            .orElseThrow(() -> new IllegalArgumentException("Event not found: " + eventUuid));
        event.setStatus(EventStatus.PENDING);
        event.setRetryCount(0);
        event.setProcessingStartedAt(null);
        event.setProcessedAt(null);
        event.setFailedAt(null);
        event.setLastErrorMessage(null);
        event.setLastErrorStackTrace(null);
        workersEventRepository.save(event);
    }

    @Override
    @Transactional
    public int archiveOldEvents(int olderThanDays) {
        Instant threshold = LocalDate.now(ZoneOffset.UTC).minusDays(olderThanDays).atStartOfDay().toInstant(ZoneOffset.UTC);
        List<Long> ids = workersEventRepository.findByStatusAndCreatedAtBefore(EventStatus.SUCCESS, threshold).stream()
            .map(WorkersEvent::getId)
            .collect(Collectors.toCollection(ArrayList::new));
        ids.addAll(workersEventRepository.findByStatusAndCreatedAtBefore(EventStatus.FAILED_PERMANENT, threshold).stream()
            .map(WorkersEvent::getId)
            .toList());
        if (ids.isEmpty()) {
            return 0;
        }
        int updated = workersEventRepository.bulkUpdateStatus(EventStatus.ARCHIVED, Instant.now(), ids);
        LOGGER.info("Archived {} events older than {} days", updated, olderThanDays);
        return updated;
    }

    private String writeValue(Object eventData) {
        if (eventData == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(eventData);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Unable to serialize event data", e);
        }
    }

    private long computeProcessingDuration(EventConsumption consumption) {
        if (consumption.getProcessingStartedAt() == null || consumption.getCompletedAt() == null) {
            return 0L;
        }
        return consumption.getCompletedAt().toEpochMilli() - consumption.getProcessingStartedAt().toEpochMilli();
    }

    private String getStackTrace(Exception error) {
        StringBuilder builder = new StringBuilder();
        builder.append(error).append("\n");
        for (StackTraceElement element : error.getStackTrace()) {
            builder.append("\tat ").append(element).append('\n');
        }
        return builder.toString();
    }
}
