package com.example.commerceoperations.outbox.application;

import org.springframework.stereotype.Component;

import com.example.commerceoperations.outbox.domain.OutboxEvent;

@Component
public class OrderEventHandler {

    public static final String ORDER_STATUS_CHANGED = "ORDER_STATUS_CHANGED";

    private final OrderEventNotifier notifier;

    public OrderEventHandler(OrderEventNotifier notifier) {
        this.notifier = notifier;
    }

    public void handle(OutboxEvent event) {
        if (!ORDER_STATUS_CHANGED.equals(event.getEventType())) {
            throw new IllegalArgumentException("Unsupported outbox event type: " + event.getEventType());
        }
        notifier.notify(event);
    }
}
