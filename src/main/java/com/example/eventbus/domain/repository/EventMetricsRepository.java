package com.example.eventbus.domain.repository;

import com.example.eventbus.domain.EventMetrics;
import com.example.eventbus.domain.EventStatus;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EventMetricsRepository extends JpaRepository<EventMetrics, Long> {

    Optional<EventMetrics> findByMetricDateAndEventTypeAndConsumerWorker_IdAndStatus(LocalDate metricDate,
                                                                                     String eventType,
                                                                                     Long consumerWorkerId,
                                                                                     EventStatus status);

    List<EventMetrics> findByEventTypeAndMetricDateBetween(String eventType, LocalDate from, LocalDate to);

    long countByEventTypeAndMetricDateAndStatus(String eventType, LocalDate metricDate, EventStatus status);
}
