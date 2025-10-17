package com.example.eventbus.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "event_metrics")
public class EventMetrics {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "metric_date", nullable = false)
    private LocalDate metricDate;

    @Column(name = "event_type", nullable = false)
    private String eventType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "consumer_worker_id")
    private SystemWorker consumerWorker;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private EventStatus status;

    @Column(name = "event_count", nullable = false)
    private Long eventCount = 0L;

    @Column(name = "total_processing_time_ms", nullable = false)
    private Long totalProcessingTimeMs = 0L;

    @Column(name = "avg_processing_time_ms")
    private Long avgProcessingTimeMs;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public EventMetrics() {
    }

    public EventMetrics(LocalDate metricDate, String eventType, SystemWorker consumerWorker, EventStatus status) {
        this.metricDate = metricDate;
        this.eventType = eventType;
        this.consumerWorker = consumerWorker;
        this.status = status;
    }

    @PrePersist
    public void prePersist() {
        Instant now = Instant.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = Instant.now();
    }

    public void incrementEventCount() {
        this.eventCount = this.eventCount + 1;
    }

    public void addProcessingTime(long processingTimeMs) {
        this.totalProcessingTimeMs = this.totalProcessingTimeMs + processingTimeMs;
    }

    public void recalculateAverage() {
        if (this.eventCount > 0) {
            this.avgProcessingTimeMs = this.totalProcessingTimeMs / this.eventCount;
        }
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public LocalDate getMetricDate() {
        return metricDate;
    }

    public void setMetricDate(LocalDate metricDate) {
        this.metricDate = metricDate;
    }

    public String getEventType() {
        return eventType;
    }

    public void setEventType(String eventType) {
        this.eventType = eventType;
    }

    public SystemWorker getConsumerWorker() {
        return consumerWorker;
    }

    public void setConsumerWorker(SystemWorker consumerWorker) {
        this.consumerWorker = consumerWorker;
    }

    public EventStatus getStatus() {
        return status;
    }

    public void setStatus(EventStatus status) {
        this.status = status;
    }

    public Long getEventCount() {
        return eventCount;
    }

    public void setEventCount(Long eventCount) {
        this.eventCount = eventCount;
    }

    public Long getTotalProcessingTimeMs() {
        return totalProcessingTimeMs;
    }

    public void setTotalProcessingTimeMs(Long totalProcessingTimeMs) {
        this.totalProcessingTimeMs = totalProcessingTimeMs;
    }

    public Long getAvgProcessingTimeMs() {
        return avgProcessingTimeMs;
    }

    public void setAvgProcessingTimeMs(Long avgProcessingTimeMs) {
        this.avgProcessingTimeMs = avgProcessingTimeMs;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}
