package com.example.eventbus.service.impl;

import com.example.eventbus.domain.SystemWorker;
import com.example.eventbus.domain.repository.SystemWorkerRepository;
import com.example.eventbus.service.IEventConsumer;
import com.example.eventbus.service.IEventProducer;
import com.example.eventbus.service.IWorkerRegistry;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class WorkerRegistryImpl implements IWorkerRegistry {

    private static final Logger LOGGER = LoggerFactory.getLogger(WorkerRegistryImpl.class);

    private final Map<String, IEventProducer> producers = new ConcurrentHashMap<>();
    private final Map<String, List<IEventConsumer>> consumers = new ConcurrentHashMap<>();
    private final SystemWorkerRepository systemWorkerRepository;

    public WorkerRegistryImpl(SystemWorkerRepository systemWorkerRepository) {
        this.systemWorkerRepository = systemWorkerRepository;
    }

    @Override
    @Transactional
    public void registerProducer(IEventProducer producer) {
        SystemWorker worker = persistWorker(producer.getProducerWorker());
        producers.put(worker.getWorkerName(), producer);
        LOGGER.info("Registered producer {}", worker.getWorkerName());
    }

    @Override
    @Transactional
    public void registerConsumer(IEventConsumer consumer) {
        SystemWorker worker = persistWorker(consumer.getConsumerWorker());
        consumer.getSupportedEventTypes().forEach(eventType ->
            consumers.computeIfAbsent(eventType, key -> new ArrayList<>()).add(consumer)
        );
        LOGGER.info("Registered consumer {} for events {}", worker.getWorkerName(), consumer.getSupportedEventTypes());
    }

    @Override
    public IEventProducer getProducerByWorkerName(String workerName) {
        return producers.get(workerName);
    }

    @Override
    public List<IEventConsumer> getConsumersByEventType(String eventType) {
        return consumers.containsKey(eventType)
            ? Collections.unmodifiableList(consumers.get(eventType))
            : Collections.emptyList();
    }

    @Override
    public List<IEventConsumer> getAllConsumers() {
        return consumers.values().stream()
            .flatMap(List::stream)
            .distinct()
            .toList();
    }

    private SystemWorker persistWorker(SystemWorker worker) {
        if (worker.getId() != null) {
            return worker;
        }
        return systemWorkerRepository.findByWorkerName(worker.getWorkerName())
            .orElseGet(() -> systemWorkerRepository.save(worker));
    }
}
