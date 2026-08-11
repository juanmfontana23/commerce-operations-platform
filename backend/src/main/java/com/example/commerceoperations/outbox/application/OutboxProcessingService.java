package com.example.commerceoperations.outbox.application;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.commerceoperations.outbox.domain.OutboxEvent;
import com.example.commerceoperations.outbox.domain.OutboxEventStatus;
import com.example.commerceoperations.outbox.infrastructure.OutboxEventRepository;
import com.example.commerceoperations.shared.exception.ResourceNotFoundException;

import io.micrometer.core.instrument.MeterRegistry;

@Service
public class OutboxProcessingService {

    private static final Logger logger = LoggerFactory.getLogger(OutboxProcessingService.class);
    private final OutboxEventRepository eventRepository;
    private final OutboxDeliveryService deliveryService;
    private final MeterRegistry meterRegistry;
    private final int maxAttempts;
    private final Duration claimLease;

    public OutboxProcessingService(OutboxEventRepository eventRepository, OutboxDeliveryService deliveryService,
            MeterRegistry meterRegistry,
            @Value("${outbox.worker.max-attempts:5}") int maxAttempts,
            @Value("${outbox.worker.claim-lease:PT30S}") Duration claimLease) {
        this.eventRepository = eventRepository;
        this.deliveryService = deliveryService;
        this.meterRegistry = meterRegistry;
        this.maxAttempts = maxAttempts;
        this.claimLease = claimLease;
    }

    @Transactional
    public List<OutboxEvent> claimDue(int batchSize, LocalDateTime now) {
        List<OutboxEvent> events = eventRepository.findDueForUpdate(now,
                List.of(OutboxEventStatus.PENDING, OutboxEventStatus.PROCESSING), PageRequest.of(0, batchSize));
        events.forEach(event -> event.claim(now.plus(claimLease)));
        return events;
    }

    @Transactional
    public void process(OutboxEvent claimedEvent) {
        try {
            OutboxEvent event = eventRepository.findById(claimedEvent.getId()).orElseThrow();
            deliveryService.deliverOnce(event);
            event.markSucceeded(LocalDateTime.now());
            eventRepository.save(event);
            meterRegistry.counter("outbox.events.processed", "result", "success").increment();
        } catch (Exception exception) {
            OutboxEvent event = eventRepository.findById(claimedEvent.getId()).orElseThrow();
            event.incrementAttempt();
            boolean terminal = event.getAttemptCount() >= maxAttempts;
            long backoffSeconds = Math.min(300, 1L << Math.min(event.getAttemptCount(), 8));
            event.markFailed(LocalDateTime.now().plusSeconds(backoffSeconds), exception.getMessage(), LocalDateTime.now(), terminal);
            eventRepository.save(event);
            meterRegistry.counter("outbox.events.processed", "result", "failure").increment();
            meterRegistry.counter("outbox.events.retries", "result", terminal ? "dead_letter" : "scheduled").increment();
            logger.warn("Outbox event processing failed eventId={} attempt={}/{}", event.getEventId(),
                    event.getAttemptCount(), maxAttempts, exception);
        }
    }

    @Transactional
    public OutboxEvent requeueFailed(Long eventId, LocalDateTime now) {
        OutboxEvent event = eventRepository.findByIdForUpdate(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Outbox event not found: " + eventId));
        event.requeue(now);
        return eventRepository.save(event);
    }
}
