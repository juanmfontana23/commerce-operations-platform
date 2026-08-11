package com.example.commerceoperations.outbox.infrastructure;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.commerceoperations.outbox.application.OutboxDelivery;

public interface OutboxDeliveryRepository extends JpaRepository<OutboxDelivery, Long> {
    boolean existsByEventId(UUID eventId);
}
