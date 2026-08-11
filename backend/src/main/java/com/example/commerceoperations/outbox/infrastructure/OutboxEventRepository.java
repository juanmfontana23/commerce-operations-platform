package com.example.commerceoperations.outbox.infrastructure;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.example.commerceoperations.outbox.domain.OutboxEvent;
import com.example.commerceoperations.outbox.domain.OutboxEventStatus;

import jakarta.persistence.LockModeType;

public interface OutboxEventRepository extends JpaRepository<OutboxEvent, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select e from OutboxEvent e where e.availableAt <= :now and e.status in :statuses order by e.id")
    List<OutboxEvent> findDueForUpdate(LocalDateTime now, List<OutboxEventStatus> statuses, Pageable pageable);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select e from OutboxEvent e where e.id = :id")
    java.util.Optional<OutboxEvent> findByIdForUpdate(@Param("id") Long id);
}
