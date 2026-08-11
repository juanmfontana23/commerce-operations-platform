package com.example.commerceoperations.outbox.application;

import java.time.LocalDateTime;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "outbox.worker.enabled", havingValue = "true", matchIfMissing = true)
public class OutboxWorker {

    private static final Logger logger = LoggerFactory.getLogger(OutboxWorker.class);
    private final OutboxProcessingService processingService;

    public OutboxWorker(OutboxProcessingService processingService) {
        this.processingService = processingService;
    }

    @Scheduled(fixedDelayString = "${outbox.worker.poll-delay:PT2S}")
    public void poll() {
        var events = processingService.claimDue(20, LocalDateTime.now());
        if (!events.isEmpty()) {
            logger.debug("Claimed {} due outbox events", events.size());
            events.forEach(processingService::process);
        }
    }
}
