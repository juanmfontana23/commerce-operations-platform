package com.example.commerceoperations.outbox.domain;

import java.time.LocalDateTime;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "outbox_events")
public class OutboxEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "event_id", nullable = false, unique = true, updatable = false)
    private UUID eventId;

    @Column(name = "idempotency_key", nullable = false, unique = true, updatable = false, length = 255)
    private String idempotencyKey;

    @Column(name = "event_type", nullable = false, length = 100)
    private String eventType;

    @Column(name = "aggregate_type", nullable = false, length = 100)
    private String aggregateType;

    @Column(name = "aggregate_id", nullable = false)
    private Long aggregateId;

    @Column(nullable = false, columnDefinition = "text")
    private String payload;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private OutboxEventStatus status;

    @Column(name = "attempt_count", nullable = false)
    private int attemptCount;

    @Column(name = "available_at", nullable = false)
    private LocalDateTime availableAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "last_error", length = 1_000)
    private String lastError;

    protected OutboxEvent() {
    }

    public OutboxEvent(String idempotencyKey, String eventType, String aggregateType, Long aggregateId,
            String payload, LocalDateTime now) {
        this.eventId = UUID.randomUUID();
        this.idempotencyKey = idempotencyKey;
        this.eventType = eventType;
        this.aggregateType = aggregateType;
        this.aggregateId = aggregateId;
        this.payload = payload;
        this.status = OutboxEventStatus.PENDING;
        this.availableAt = now;
        this.createdAt = now;
        this.updatedAt = now;
    }

    public void claim(LocalDateTime leaseUntil) {
        status = OutboxEventStatus.PROCESSING;
        availableAt = leaseUntil;
        updatedAt = LocalDateTime.now();
    }

    public void markSucceeded(LocalDateTime now) {
        status = OutboxEventStatus.SUCCEEDED;
        availableAt = now;
        updatedAt = now;
        lastError = null;
    }

    public void markFailed(LocalDateTime availableAt, String error, LocalDateTime now, boolean terminal) {
        status = terminal ? OutboxEventStatus.FAILED : OutboxEventStatus.PENDING;
        this.availableAt = availableAt;
        updatedAt = now;
        lastError = error == null ? null : error.substring(0, Math.min(error.length(), 1_000));
    }

    public void requeue(LocalDateTime now) {
        if (status != OutboxEventStatus.FAILED) {
            throw new IllegalStateException("Only FAILED outbox events can be requeued");
        }
        status = OutboxEventStatus.PENDING;
        attemptCount = 0;
        availableAt = now;
        updatedAt = now;
    }

    public void incrementAttempt() {
        attemptCount++;
    }

    public Long getId() { return id; }
    public UUID getEventId() { return eventId; }
    public String getIdempotencyKey() { return idempotencyKey; }
    public String getEventType() { return eventType; }
    public String getAggregateType() { return aggregateType; }
    public Long getAggregateId() { return aggregateId; }
    public String getPayload() { return payload; }
    public OutboxEventStatus getStatus() { return status; }
    public int getAttemptCount() { return attemptCount; }
    public LocalDateTime getAvailableAt() { return availableAt; }
    public String getLastError() { return lastError; }
}
