package com.example.eventbus;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.eventbus.domain.EventConsumption;
import com.example.eventbus.domain.EventStatus;
import com.example.eventbus.domain.WorkersEvent;
import com.example.eventbus.domain.repository.EventConsumptionRepository;
import com.example.eventbus.domain.repository.WorkersEventRepository;
import com.example.eventbus.dto.ConsumptionResult;
import com.example.eventbus.service.IEventBusService;
import com.example.eventbus.service.IIdempotencyService;
import com.example.eventbus.worker.consumer.CsvProcessorConsumer;
import com.example.eventbus.worker.producer.FtpDownloadProducer;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@TestPropertySource(properties = "eventbus.scheduler.enabled=false")
class EventBusIntegrationTest {

    @Autowired
    private FtpDownloadProducer ftpDownloadProducer;

    @Autowired
    private CsvProcessorConsumer csvProcessorConsumer;

    @Autowired
    private IEventBusService eventBusService;

    @Autowired
    private IIdempotencyService idempotencyService;

    @Autowired
    private WorkersEventRepository workersEventRepository;

    @Autowired
    private EventConsumptionRepository eventConsumptionRepository;

    @Test
    @Transactional
    void eventLifecycleCompletesSuccessfully() {
        ftpDownloadProducer.triggerDirectoryScan();

        List<WorkersEvent> events = eventBusService.pollPendingEvents(FtpDownloadProducer.EVENT_TYPE, 10);
        assertThat(events).isNotEmpty();

        WorkersEvent event = events.get(0);
        Long consumerWorkerId = csvProcessorConsumer.getConsumerWorker().getId();

        boolean locked = eventBusService.markEventProcessing(event.getEventUuid(), consumerWorkerId);
        assertThat(locked).isTrue();

        ConsumptionResult result = csvProcessorConsumer.consume(event);
        assertThat(result.isSuccess()).isTrue();

        String resultHash = idempotencyService.calculateResultHash(result.getResultData());
        eventBusService.markEventSuccess(event.getEventUuid(), consumerWorkerId, resultHash);

        WorkersEvent updatedEvent = workersEventRepository.findByEventUuid(event.getEventUuid()).orElseThrow();
        assertThat(updatedEvent.getStatus()).isEqualTo(EventStatus.SUCCESS);

        EventConsumption consumption = eventConsumptionRepository
            .findByEventUuidAndConsumerWorker_Id(event.getEventUuid(), consumerWorkerId)
            .orElseThrow();
        assertThat(consumption.getStatus()).isEqualTo(EventStatus.SUCCESS);
        assertThat(consumption.getIdempotencyKey()).isNotBlank();
        assertThat(consumption.getResultHash()).isEqualTo(resultHash);
    }
}
