package com.example.commerceoperations.questions.application;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.springframework.stereotype.Component;

import com.example.commerceoperations.questions.domain.Question;
import com.example.commerceoperations.questions.domain.QuestionPriority;

@Component
public class QuestionPriorityCalculator {

    public PriorityResult calculate(Question question, LocalDateTime now) {
        List<String> reasons = new ArrayList<>();
        int score = waitingTimeScore(question, now, reasons)
                + orderValueScore(question, reasons)
                + keywordScore(question, reasons)
                + productCategoryScore(question, reasons);
        return new PriorityResult(score, toPriority(score), reasons);
    }

    private int waitingTimeScore(Question question, LocalDateTime now, List<String> reasons) {
        long hours = Duration.between(question.getCreatedAt(), now).toHours();
        if (hours >= 48) {
            reasons.add("Waiting more than 48 hours");
            return 35;
        }
        if (hours >= 24) {
            reasons.add("Waiting more than 24 hours");
            return 25;
        }
        if (hours >= 8) {
            reasons.add("Waiting more than 8 hours");
            return 15;
        }
        reasons.add("Recently created");
        return 5;
    }

    private int orderValueScore(Question question, List<String> reasons) {
        int total = question.getOrder().getTotalAmount().intValue();
        if (total >= 500) {
            reasons.add("High-value order");
            return 25;
        }
        if (total >= 250) {
            reasons.add("Medium-value order");
            return 15;
        }
        if (total >= 100) {
            reasons.add("Standard-value order");
            return 8;
        }
        return 0;
    }

    private int keywordScore(Question question, List<String> reasons) {
        String message = question.getMessage().toLowerCase(Locale.ROOT);
        if (message.contains("urgent") || message.contains("refund") || message.contains("cancel")
                || message.contains("not received") || message.contains("broken")) {
            reasons.add("Message contains urgent support keywords");
            return 25;
        }
        if (message.contains("invoice") || message.contains("warranty")) {
            reasons.add("Message contains administrative support keywords");
            return 10;
        }
        return 0;
    }

    private int productCategoryScore(Question question, List<String> reasons) {
        if (question.getProduct() == null) {
            return 0;
        }
        String category = question.getProduct().getCategory().toLowerCase(Locale.ROOT);
        if (category.contains("electronics")) {
            reasons.add("Electronics product category");
            return 15;
        }
        if (category.contains("home")) {
            reasons.add("Home product category");
            return 8;
        }
        return 0;
    }

    private QuestionPriority toPriority(int score) {
        if (score >= 90) {
            return QuestionPriority.CRITICAL;
        }
        if (score >= 70) {
            return QuestionPriority.HIGH;
        }
        if (score >= 40) {
            return QuestionPriority.MEDIUM;
        }
        return QuestionPriority.LOW;
    }
}
