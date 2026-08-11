package com.example.commerceoperations.outbox.application;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.example.commerceoperations.outbox.domain.OutboxEvent;
import com.example.commerceoperations.outbox.infrastructure.OutboxDeliveryRepository;

@ExtendWith(MockitoExtension.class)
class OutboxDeliveryServiceTest {

    @Mock
    private OutboxDeliveryRepository deliveryRepository;

    @Mock
    private OrderEventHandler handler;

    @Test
    void duplicateDeliveryDoesNotInvokeHandlerAgain() {
        OutboxEvent event = new OutboxEvent("key", "ORDER_STATUS_CHANGED", "ORDER", 7L, "{}", LocalDateTime.now());
        when(deliveryRepository.existsByEventId(event.getEventId())).thenReturn(true);

        new OutboxDeliveryService(deliveryRepository, handler).deliverOnce(event);

        verify(handler, never()).handle(event);
    }
}
