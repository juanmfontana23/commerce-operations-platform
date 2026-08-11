package com.example.commerceoperations.questions.application;

import com.example.commerceoperations.questions.domain.Question;

public record QuestionWithPriority(Question question, PriorityResult priority) {
}
