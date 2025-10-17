package com.example.eventbus.service.impl;

import com.example.eventbus.domain.EventMetrics;
import com.example.eventbus.domain.EventStatus;
import com.example.eventbus.domain.SystemWorker;
import com.example.eventbus.domain.repository.EventMetricsRepository;
import com.example.eventbus.domain.repository.SystemWorkerRepository;
import com.example.eventbus.service.IMetricsService;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MetricsServiceImpl implements IMetricsService {

    private final EventMetricsRepository eventMetricsRepository;
    private final SystemWorkerRepository systemWorkerRepository;

    public MetricsServiceImpl(EventMetricsRepository eventMetricsRepository,
                              SystemWorkerRepository systemWorkerRepository) {
        this.eventMetricsRepository = eventMetricsRepository;
        this.systemWorkerRepository = systemWorkerRepository;
    }

    @Override
    @Transactional
    public void recordEventProcessed(String eventType, Long consumerWorkerId, EventStatus status, long processingTimeMs) {
        LocalDate today = LocalDate.now(ZoneOffset.UTC);
        SystemWorker worker = null;
        if (consumerWorkerId != null) {
            worker = systemWorkerRepository.findById(consumerWorkerId).orElse(null);
        }

        EventMetrics metrics = eventMetricsRepository
            .findByMetricDateAndEventTypeAndConsumerWorker_IdAndStatus(today, eventType,
                worker != null ? worker.getId() : null, status)
            .orElseGet(() -> eventMetricsRepository.save(new EventMetrics(today, eventType, worker, status)));

        metrics.incrementEventCount();
        metrics.addProcessingTime(processingTimeMs);
        metrics.recalculateAverage();
        eventMetricsRepository.save(metrics);
    }

    @Override
    @Transactional(readOnly = true)
    public List<EventMetrics> getMetrics(String eventType, LocalDate fromDate, LocalDate toDate) {
        return eventMetricsRepository.findByEventTypeAndMetricDateBetween(eventType, fromDate, toDate);
    }

    @Override
    @Transactional(readOnly = true)
    public long getDailyEventCount(String eventType, LocalDate date) {
        return eventMetricsRepository.countByEventTypeAndMetricDateAndStatus(eventType, date, EventStatus.SUCCESS);
    }
}
