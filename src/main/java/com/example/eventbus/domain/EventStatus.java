package com.example.eventbus.domain;

public enum EventStatus {
    PENDING,
    PROCESSING,
    SUCCESS,
    FAILED_RETRYABLE,
    FAILED_PERMANENT,
    ARCHIVED
}
