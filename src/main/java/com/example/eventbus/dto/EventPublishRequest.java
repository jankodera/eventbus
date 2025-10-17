package com.example.eventbus.dto;

public class EventPublishRequest {

    private Long producerWorkerId;
    private String eventType;
    private Integer eventVersion = 1;
    private Object eventData;
    private String correlationId;
    private Integer partNumber;
    private Integer partsCount;

    public Long getProducerWorkerId() {
        return producerWorkerId;
    }

    public void setProducerWorkerId(Long producerWorkerId) {
        this.producerWorkerId = producerWorkerId;
    }

    public String getEventType() {
        return eventType;
    }

    public void setEventType(String eventType) {
        this.eventType = eventType;
    }

    public Integer getEventVersion() {
        return eventVersion;
    }

    public void setEventVersion(Integer eventVersion) {
        this.eventVersion = eventVersion;
    }

    public Object getEventData() {
        return eventData;
    }

    public void setEventData(Object eventData) {
        this.eventData = eventData;
    }

    public String getCorrelationId() {
        return correlationId;
    }

    public void setCorrelationId(String correlationId) {
        this.correlationId = correlationId;
    }

    public Integer getPartNumber() {
        return partNumber;
    }

    public void setPartNumber(Integer partNumber) {
        this.partNumber = partNumber;
    }

    public Integer getPartsCount() {
        return partsCount;
    }

    public void setPartsCount(Integer partsCount) {
        this.partsCount = partsCount;
    }
}
