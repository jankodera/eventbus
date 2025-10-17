package com.example.eventbus.worker.producer;

import com.example.eventbus.domain.SystemWorker;
import com.example.eventbus.service.IEventBusService;
import com.example.eventbus.service.IIdempotencyService;
import com.example.eventbus.worker.BaseEventProducer;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FtpDownloadProducer extends BaseEventProducer {

    public static final String EVENT_TYPE = "ftp.file.discovered";

    private static final Logger LOGGER = LoggerFactory.getLogger(FtpDownloadProducer.class);

    public FtpDownloadProducer(SystemWorker systemWorker,
                               IEventBusService eventBusService,
                               IIdempotencyService idempotencyService) {
        super(systemWorker, eventBusService, idempotencyService);
    }

    public void triggerDirectoryScan() {
        List<Map<String, Object>> files = scanFtpDirectory();
        files.forEach(fileData -> {
            LOGGER.info("Publishing download event for {}", fileData.get("fileName"));
            publishEvent(EVENT_TYPE, fileData);
        });
    }

    protected List<Map<String, Object>> scanFtpDirectory() {
        return List.of(createDownloadEvent("invoices-" + Instant.now().toEpochMilli() + ".csv", "id,amount\n1,100.0"));
    }

    protected Map<String, Object> createDownloadEvent(String fileName, String csvContent) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("fileName", fileName);
        payload.put("csvContent", csvContent);
        payload.put("discoveredAt", Instant.now().toString());
        return payload;
    }
}
