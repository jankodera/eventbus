package com.example.eventbus.worker.consumer;

import com.example.eventbus.domain.SystemWorker;
import com.example.eventbus.domain.WorkersEvent;
import com.example.eventbus.dto.ConsumptionResult;
import com.example.eventbus.service.IEventBusService;
import com.example.eventbus.service.IIdempotencyService;
import com.example.eventbus.worker.BaseEventConsumer;
import com.example.eventbus.worker.producer.FtpDownloadProducer;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CsvProcessorConsumer extends BaseEventConsumer {

    private static final Logger LOGGER = LoggerFactory.getLogger(CsvProcessorConsumer.class);

    private final ObjectMapper objectMapper;

    public CsvProcessorConsumer(SystemWorker systemWorker,
                                IEventBusService eventBusService,
                                IIdempotencyService idempotencyService,
                                ObjectMapper objectMapper) {
        super(systemWorker, eventBusService, idempotencyService);
        this.objectMapper = objectMapper;
        registerEventType(FtpDownloadProducer.EVENT_TYPE, 1);
    }

    @Override
    protected ConsumptionResult processEvent(WorkersEvent event) {
        try {
            JsonNode node = objectMapper.readTree(event.getEventData());
            String csvContent = node.path("csvContent").asText("");
            List<Map<String, String>> records = parseCsv(csvContent);
            validateData(records);
            LOGGER.info("Processed {} records from event {}", records.size(), event.getEventUuid());
            Map<String, Object> result = new HashMap<>();
            result.put("recordsProcessed", records.size());
            result.put("fileName", node.path("fileName").asText("unknown"));
            return ConsumptionResult.success(result);
        } catch (JsonProcessingException e) {
            return ConsumptionResult.failure("Invalid event payload", false);
        } catch (IllegalArgumentException e) {
            return ConsumptionResult.failure(e.getMessage(), false);
        }
    }

    protected List<Map<String, String>> parseCsv(String data) {
        List<Map<String, String>> records = new ArrayList<>();
        if (data == null || data.isEmpty()) {
            return records;
        }
        String[] lines = data.split("\\n");
        if (lines.length < 2) {
            return records;
        }
        String[] headers = lines[0].split(",");
        for (int i = 1; i < lines.length; i++) {
            String[] values = lines[i].split(",");
            Map<String, String> record = new HashMap<>();
            for (int j = 0; j < headers.length && j < values.length; j++) {
                record.put(headers[j].trim(), values[j].trim());
            }
            records.add(record);
        }
        return records;
    }

    protected void validateData(List<Map<String, String>> records) {
        if (records.isEmpty()) {
            throw new IllegalArgumentException("No records found in CSV");
        }
        for (Map<String, String> record : records) {
            if (!record.containsKey("id")) {
                throw new IllegalArgumentException("Record missing id column");
            }
        }
    }
}
