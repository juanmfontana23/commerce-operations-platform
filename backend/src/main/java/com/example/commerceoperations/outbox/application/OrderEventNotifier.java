package com.example.commerceoperations.outbox.application;

import com.example.commerceoperations.outbox.domain.OutboxEvent;

public interface OrderEventNotifier {
    void notify(OutboxEvent event);
}
