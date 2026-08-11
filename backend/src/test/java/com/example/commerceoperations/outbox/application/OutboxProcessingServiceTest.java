package com.example.commerceoperations.outbox.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.example.commerceoperations.outbox.domain.OutboxEvent;
import com.example.commerceoperations.outbox.domain.OutboxEventStatus;
import com.example.commerceoperations.outbox.infrastructure.OutboxEventRepository;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;

@ExtendWith(MockitoExtension.class)
class OutboxProcessingServiceTest {

    @Mock
    private OutboxEventRepository eventRepository;

    @Mock
    private OutboxDeliveryService deliveryService;

    @Test
    void successfulProcessingMarksEventSucceeded() {
        OutboxEvent event = new OutboxEvent("success-key", "ORDER_STATUS_CHANGED", "ORDER", 7L, "{}", LocalDateTime.now());
        when(eventRepository.findById(any())).thenReturn(Optional.of(event));

        OutboxProcessingService service = new OutboxProcessingService(eventRepository, deliveryService,
                new SimpleMeterRegistry(), 3, Duration.ofSeconds(30));
        service.process(event);

        assertThat(event.getStatus()).isEqualTo(OutboxEventStatus.SUCCEEDED);
        verify(deliveryService).deliverOnce(event);
        verify(eventRepository).save(event);
    }

    @Test
    void processingFailureSchedulesRetryAndIncrementsAttempt() {
        OutboxEvent event = new OutboxEvent("key", "ORDER_STATUS_CHANGED", "ORDER", 7L, "{}", LocalDateTime.now());
        when(eventRepository.findById(any())).thenReturn(Optional.of(event));
        doThrow(new IllegalStateException("temporary failure")).when(deliveryService).deliverOnce(event);

        OutboxProcessingService service = new OutboxProcessingService(eventRepository, deliveryService,
                new SimpleMeterRegistry(), 3, Duration.ofSeconds(30));
        service.process(event);

        assertThat(event.getAttemptCount()).isEqualTo(1);
        assertThat(event.getStatus()).isEqualTo(OutboxEventStatus.PENDING);
        verify(eventRepository).save(event);
    }

    @Test
    void terminalFailureCanBeRequeuedAndProcessedAgain() {
        OutboxEvent event = new OutboxEvent("key", "ORDER_STATUS_CHANGED", "ORDER", 7L, "{}", LocalDateTime.now());
        when(eventRepository.findById(any())).thenReturn(Optional.of(event));
        when(eventRepository.findByIdForUpdate(event.getId())).thenReturn(Optional.of(event));
        doThrow(new IllegalStateException("temporary failure"))
                .doNothing().when(deliveryService).deliverOnce(event);

        OutboxProcessingService service = new OutboxProcessingService(eventRepository, deliveryService,
                new SimpleMeterRegistry(), 1, Duration.ofSeconds(30));
        service.process(event);

        assertThat(event.getStatus()).isEqualTo(OutboxEventStatus.FAILED);
        assertThat(event.getLastError()).isEqualTo("temporary failure");

        service.requeueFailed(event.getId(), LocalDateTime.now());

        assertThat(event.getStatus()).isEqualTo(OutboxEventStatus.PENDING);
        assertThat(event.getAttemptCount()).isZero();
        assertThat(event.getLastError()).isEqualTo("temporary failure");
        verify(eventRepository).findByIdForUpdate(event.getId());

        service.process(event);

        assertThat(event.getStatus()).isEqualTo(OutboxEventStatus.SUCCEEDED);
    }
}
