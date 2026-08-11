package com.example.commerceoperations.questions.api;

import java.util.List;

import com.example.commerceoperations.questions.domain.QuestionPriority;

public record PriorityResponse(int score, QuestionPriority priority, List<String> reasons) {
}
