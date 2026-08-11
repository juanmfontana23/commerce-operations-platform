package com.example.commerceoperations.questions.api;

import java.time.LocalDateTime;

import com.example.commerceoperations.questions.domain.QuestionStatus;

public record QuestionResponse(
        Long id,
        Long orderId,
        Long buyerId,
        String buyerName,
        Long productId,
        String productTitle,
        String message,
        String answer,
        QuestionStatus status,
        PriorityResponse priority,
        LocalDateTime createdAt,
        LocalDateTime answeredAt,
        LocalDateTime resolvedAt) {
}
