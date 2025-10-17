package com.example.eventbus.worker;

import com.example.eventbus.domain.SystemWorker;
import com.example.eventbus.service.IEventBusService;
import com.example.eventbus.service.IIdempotencyService;
import java.util.function.Supplier;

public abstract class BaseEventWorker {

    private final Long workerId;
    private final String workerName;
    private final SystemWorker systemWorker;
    protected final IEventBusService eventBusService;
    protected final IIdempotencyService idempotencyService;

    protected BaseEventWorker(SystemWorker systemWorker,
                              IEventBusService eventBusService,
                              IIdempotencyService idempotencyService) {
        this.workerId = systemWorker.getId();
        this.workerName = systemWorker.getWorkerName();
        this.systemWorker = systemWorker;
        this.eventBusService = eventBusService;
        this.idempotencyService = idempotencyService;
    }

    public Long getWorkerId() {
        return workerId;
    }

    public String getWorkerName() {
        return workerName;
    }

    public SystemWorker getSystemWorker() {
        return systemWorker;
    }

    protected <T> T ensureIdempotent(Supplier<T> operation) {
        return operation.get();
    }
}
