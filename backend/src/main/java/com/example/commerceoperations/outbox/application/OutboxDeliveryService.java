package com.example.commerceoperations.outbox.application;

import java.time.LocalDateTime;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Propagation;

import com.example.commerceoperations.outbox.domain.OutboxEvent;
import com.example.commerceoperations.outbox.infrastructure.OutboxDeliveryRepository;

@Service
public class OutboxDeliveryService {

    private final OutboxDeliveryRepository deliveryRepository;
    private final OrderEventHandler handler;

    public OutboxDeliveryService(OutboxDeliveryRepository deliveryRepository, OrderEventHandler handler) {
        this.deliveryRepository = deliveryRepository;
        this.handler = handler;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void deliverOnce(OutboxEvent event) {
        if (deliveryRepository.existsByEventId(event.getEventId())) {
            return;
        }
        handler.handle(event);
        deliveryRepository.save(new OutboxDelivery(event, LocalDateTime.now()));
    }
}
