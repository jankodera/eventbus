package com.example.eventbus.service;

import com.example.eventbus.domain.WorkersEvent;
import java.util.List;

public interface IEventBusService {

    String publishEvent(Long producerWorkerId, String eventType, Object eventData);

    String publishEvent(Long producerWorkerId, String eventType, Object eventData, String correlationId);

    List<WorkersEvent> pollPendingEvents(String eventType, int limit);

    boolean markEventProcessing(String eventUuid, Long consumerWorkerId);

    void markEventSuccess(String eventUuid, Long consumerWorkerId, String resultHash);

    void markEventFailed(String eventUuid, Long consumerWorkerId, Exception error, boolean retryable);

    void replayEvent(String eventUuid);

    int archiveOldEvents(int olderThanDays);
}
