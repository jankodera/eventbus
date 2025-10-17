package com.example.eventbus.domain.repository;

import com.example.eventbus.domain.EventConsumption;
import com.example.eventbus.domain.EventStatus;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;

public interface EventConsumptionRepository extends JpaRepository<EventConsumption, Long> {

    Optional<EventConsumption> findByEventUuidAndConsumerWorker_Id(String eventUuid, Long consumerWorkerId);

    Optional<EventConsumption> findByIdempotencyKey(String idempotencyKey);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from EventConsumption c where c.eventUuid = :eventUuid and c.consumerWorker.id = :consumerWorkerId")
    Optional<EventConsumption> lockByEventUuidAndConsumerWorker(@Param("eventUuid") String eventUuid,
                                                                @Param("consumerWorkerId") Long consumerWorkerId);

    long countByConsumerWorker_IdAndStatus(Long consumerWorkerId, EventStatus status);
}
