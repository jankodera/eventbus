package com.example.eventbus.service;

import com.example.eventbus.domain.SystemWorker;
import com.example.eventbus.domain.WorkersEvent;
import com.example.eventbus.dto.ConsumptionResult;
import java.util.List;

public interface IEventConsumer {

    boolean canConsume(String eventType, Integer eventVersion);

    ConsumptionResult consume(WorkersEvent event);

    SystemWorker getConsumerWorker();

    List<String> getSupportedEventTypes();
}
