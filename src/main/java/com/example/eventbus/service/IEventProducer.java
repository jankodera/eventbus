package com.example.eventbus.service;

import com.example.eventbus.domain.SystemWorker;

public interface IEventProducer {

    String publishEvent(String eventType, Object eventData);

    String publishEvent(String eventType, Object eventData, String correlationId);

    SystemWorker getProducerWorker();
}
