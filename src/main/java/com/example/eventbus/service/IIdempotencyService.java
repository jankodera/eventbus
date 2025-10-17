package com.example.eventbus.service;

public interface IIdempotencyService {

    String generateIdempotencyKey(String eventUuid, Long consumerWorkerId);

    boolean isAlreadyProcessed(String idempotencyKey);

    void recordProcessing(String idempotencyKey, String resultHash);

    String calculateResultHash(Object result);
}
