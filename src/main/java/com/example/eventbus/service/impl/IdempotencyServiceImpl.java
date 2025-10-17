package com.example.eventbus.service.impl;

import com.example.eventbus.domain.EventConsumption;
import com.example.eventbus.domain.EventStatus;
import com.example.eventbus.domain.repository.EventConsumptionRepository;
import com.example.eventbus.service.IIdempotencyService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class IdempotencyServiceImpl implements IIdempotencyService {

    private static final Logger LOGGER = LoggerFactory.getLogger(IdempotencyServiceImpl.class);

    private final EventConsumptionRepository eventConsumptionRepository;
    private final ObjectMapper objectMapper;

    public IdempotencyServiceImpl(EventConsumptionRepository eventConsumptionRepository,
                                  ObjectMapper objectMapper) {
        this.eventConsumptionRepository = eventConsumptionRepository;
        this.objectMapper = objectMapper;
    }

    @Override
    public String generateIdempotencyKey(String eventUuid, Long consumerWorkerId) {
        String value = eventUuid + "-" + consumerWorkerId;
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("Unable to create SHA-256 digest", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isAlreadyProcessed(String idempotencyKey) {
        Optional<EventConsumption> optional = eventConsumptionRepository.findByIdempotencyKey(idempotencyKey);
        return optional.filter(consumption -> consumption.getStatus() == EventStatus.SUCCESS).isPresent();
    }

    @Override
    @Transactional
    public void recordProcessing(String idempotencyKey, String resultHash) {
        eventConsumptionRepository.findByIdempotencyKey(idempotencyKey).ifPresentOrElse(consumption -> {
            consumption.setResultHash(resultHash);
            eventConsumptionRepository.save(consumption);
        }, () -> LOGGER.warn("Consumption record not found when recording idempotent result for key {}", idempotencyKey));
    }

    @Override
    public String calculateResultHash(Object result) {
        if (result == null) {
            return null;
        }
        try {
            String json = objectMapper.writeValueAsString(result);
            return generateSha256(json);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Unable to serialize result for hashing", e);
        }
    }

    private String generateSha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("Unable to create SHA-256 digest", e);
        }
    }
}
