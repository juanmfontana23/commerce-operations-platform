package com.example.commerceoperations.questions.application;

import java.util.List;

import com.example.commerceoperations.questions.domain.QuestionPriority;

public record PriorityResult(int score, QuestionPriority priority, List<String> reasons) {
}
