package com.example.commerceoperations.questions.api;

import jakarta.validation.constraints.NotBlank;

public record AnswerQuestionRequest(@NotBlank String answer) {
}
