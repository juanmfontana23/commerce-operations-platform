package com.example.commerceoperations.questions.application;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.commerceoperations.notifications.application.NotificationService;
import com.example.commerceoperations.questions.domain.Question;
import com.example.commerceoperations.questions.domain.QuestionStatus;
import com.example.commerceoperations.questions.infrastructure.QuestionRepository;
import com.example.commerceoperations.shared.exception.ResourceNotFoundException;

@Service
public class QuestionService {

    private final QuestionRepository questionRepository;
    private final QuestionPriorityCalculator priorityCalculator;
    private final NotificationService notificationService;

    public QuestionService(
            QuestionRepository questionRepository,
            QuestionPriorityCalculator priorityCalculator,
            NotificationService notificationService) {
        this.questionRepository = questionRepository;
        this.priorityCalculator = priorityCalculator;
        this.notificationService = notificationService;
    }

    @Transactional(readOnly = true)
    public List<QuestionWithPriority> findOrderQuestions(Long orderId) {
        return questionRepository.findByOrderId(orderId).stream()
                .map(this::withPriority)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<QuestionWithPriority> findUnresolvedSellerQuestions(Long sellerId) {
        return questionRepository.findByOrderSellerIdAndStatusNot(sellerId, QuestionStatus.RESOLVED).stream()
                .map(this::withPriority)
                .sorted(Comparator.comparingInt((QuestionWithPriority item) -> item.priority().score()).reversed()
                        .thenComparing(item -> item.question().getCreatedAt()))
                .toList();
    }

    @Transactional
    public QuestionWithPriority answerQuestion(Long questionId, String answer) {
        Question question = getQuestion(questionId);
        question.answer(answer, LocalDateTime.now());
        return refreshPriority(question, true);
    }

    @Transactional
    public QuestionWithPriority resolveQuestion(Long questionId) {
        Question question = getQuestion(questionId);
        question.resolve(LocalDateTime.now());
        return refreshPriority(question, false);
    }

    @Transactional
    public QuestionWithPriority refreshPriority(Question question) {
        return refreshPriority(question, false);
    }

    @Transactional
    public QuestionWithPriority refreshPriorityAndNotify(Question question) {
        return refreshPriority(question, true);
    }

    private QuestionWithPriority refreshPriority(Question question, boolean notify) {
        PriorityResult priority = priorityCalculator.calculate(question, LocalDateTime.now());
        question.updatePriority(priority.priority(), priority.score());
        Question savedQuestion = questionRepository.save(question);
        if (notify) {
            notificationService.notifyIfImportant(savedQuestion);
        }
        return new QuestionWithPriority(savedQuestion, priority);
    }

    public Question getQuestion(Long questionId) {
        return questionRepository.findWithDetailsById(questionId)
                .orElseThrow(() -> new ResourceNotFoundException("Question not found: " + questionId));
    }

    private QuestionWithPriority withPriority(Question question) {
        return new QuestionWithPriority(question, priorityCalculator.calculate(question, LocalDateTime.now()));
    }
}
