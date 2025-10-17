package com.example.eventbus.service;

import com.example.eventbus.domain.EventMetrics;
import com.example.eventbus.domain.EventStatus;
import java.time.LocalDate;
import java.util.List;

public interface IMetricsService {

    void recordEventProcessed(String eventType, Long consumerWorkerId, EventStatus status, long processingTimeMs);

    List<EventMetrics> getMetrics(String eventType, LocalDate fromDate, LocalDate toDate);

    long getDailyEventCount(String eventType, LocalDate date);
}
