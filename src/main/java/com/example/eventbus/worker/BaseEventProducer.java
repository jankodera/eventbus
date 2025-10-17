package com.example.eventbus.worker;

import com.example.eventbus.domain.SystemWorker;
import com.example.eventbus.service.IEventBusService;
import com.example.eventbus.service.IEventProducer;
import com.example.eventbus.service.IIdempotencyService;

public abstract class BaseEventProducer extends BaseEventWorker implements IEventProducer {

    protected BaseEventProducer(SystemWorker systemWorker,
                                IEventBusService eventBusService,
                                IIdempotencyService idempotencyService) {
        super(systemWorker, eventBusService, idempotencyService);
    }

    @Override
    public String publishEvent(String eventType, Object eventData) {
        return publishEvent(eventType, eventData, null);
    }

    @Override
    public String publishEvent(String eventType, Object eventData, String correlationId) {
        validateEventData(eventData);
        Object enrichedData = enrichEventData(eventData);
        return eventBusService.publishEvent(getWorkerId(), eventType, enrichedData, correlationId);
    }

    @Override
    public SystemWorker getProducerWorker() {
        return getSystemWorker();
    }

    protected void validateEventData(Object eventData) {
    }

    protected Object enrichEventData(Object eventData) {
        return eventData;
    }
}
