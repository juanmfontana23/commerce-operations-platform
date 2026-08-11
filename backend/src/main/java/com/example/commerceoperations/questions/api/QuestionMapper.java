package com.example.commerceoperations.questions.api;

import com.example.commerceoperations.questions.application.PriorityResult;
import com.example.commerceoperations.questions.domain.Question;

final class QuestionMapper {

    private QuestionMapper() {
    }

    static QuestionResponse toResponse(Question question, PriorityResult priority) {
        return new QuestionResponse(
                question.getId(),
                question.getOrder().getId(),
                question.getBuyer().getId(),
                question.getBuyer().getName(),
                question.getProduct() == null ? null : question.getProduct().getId(),
                question.getProduct() == null ? null : question.getProduct().getTitle(),
                question.getMessage(),
                question.getAnswer(),
                question.getStatus(),
                new PriorityResponse(priority.score(), priority.priority(), priority.reasons()),
                question.getCreatedAt(),
                question.getAnsweredAt(),
                question.getResolvedAt());
    }
}
