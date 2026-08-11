package com.example.commerceoperations.outbox.application;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.example.commerceoperations.outbox.domain.OutboxEvent;

@Component
public class LoggingOrderEventNotifier implements OrderEventNotifier {

    private static final Logger logger = LoggerFactory.getLogger(LoggingOrderEventNotifier.class);

    @Override
    public void notify(OutboxEvent event) {
        logger.info("Processed order event eventId={} type={} orderId={} payload={}",
                event.getEventId(), event.getEventType(), event.getAggregateId(), event.getPayload());
    }
}
