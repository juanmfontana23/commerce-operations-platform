package com.example.commerceoperations.outbox.domain;

public enum OutboxEventStatus {
    PENDING,
    PROCESSING,
    SUCCEEDED,
    FAILED
}
