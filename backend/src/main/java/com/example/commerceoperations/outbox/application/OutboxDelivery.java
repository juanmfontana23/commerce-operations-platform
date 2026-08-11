package com.example.commerceoperations.outbox.application;

import java.time.LocalDateTime;
import java.util.UUID;

import com.example.commerceoperations.outbox.domain.OutboxEvent;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "outbox_deliveries")
public class OutboxDelivery {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "event_id", nullable = false, unique = true, updatable = false)
    private UUID eventId;

    @Column(name = "delivered_at", nullable = false)
    private LocalDateTime deliveredAt;

    protected OutboxDelivery() {
    }

    public OutboxDelivery(OutboxEvent event, LocalDateTime deliveredAt) {
        this.eventId = event.getEventId();
        this.deliveredAt = deliveredAt;
    }
}
