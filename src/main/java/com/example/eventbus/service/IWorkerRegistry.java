package com.example.eventbus.service;

import java.util.List;

public interface IWorkerRegistry {

    void registerProducer(IEventProducer producer);

    void registerConsumer(IEventConsumer consumer);

    IEventProducer getProducerByWorkerName(String workerName);

    List<IEventConsumer> getConsumersByEventType(String eventType);

    List<IEventConsumer> getAllConsumers();
}
