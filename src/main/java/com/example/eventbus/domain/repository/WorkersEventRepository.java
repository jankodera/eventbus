package com.example.eventbus.domain.repository;

import com.example.eventbus.domain.EventStatus;
import com.example.eventbus.domain.WorkersEvent;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface WorkersEventRepository extends JpaRepository<WorkersEvent, Long> {

    Optional<WorkersEvent> findByEventUuid(String eventUuid);

    List<WorkersEvent> findTop100ByEventTypeAndStatusOrderByCreatedAtAsc(String eventType, EventStatus status);

    List<WorkersEvent> findByStatusAndCreatedAtBefore(EventStatus status, Instant createdAt);

    @Modifying
    @Query("update WorkersEvent e set e.status = :status, e.archivedAt = :archivedAt where e.id in :ids")
    int bulkUpdateStatus(@Param("status") EventStatus status, @Param("archivedAt") Instant archivedAt, @Param("ids") List<Long> ids);
}
