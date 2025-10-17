package com.example.eventbus.domain.repository;

import com.example.eventbus.domain.SystemWorker;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SystemWorkerRepository extends JpaRepository<SystemWorker, Long> {

    Optional<SystemWorker> findByWorkerName(String workerName);
}
