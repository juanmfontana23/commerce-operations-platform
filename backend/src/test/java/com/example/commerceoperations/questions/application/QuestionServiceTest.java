package com.example.commerceoperations.questions.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.example.commerceoperations.notifications.application.NotificationService;
import com.example.commerceoperations.orders.domain.Buyer;
import com.example.commerceoperations.orders.domain.Order;
import com.example.commerceoperations.orders.domain.OrderStatus;
import com.example.commerceoperations.orders.domain.Product;
import com.example.commerceoperations.orders.domain.Seller;
import com.example.commerceoperations.questions.domain.Question;
import com.example.commerceoperations.questions.domain.QuestionPriority;
import com.example.commerceoperations.questions.domain.QuestionStatus;
import com.example.commerceoperations.questions.infrastructure.QuestionRepository;

@ExtendWith(MockitoExtension.class)
class QuestionServiceTest {

    @Mock
    private QuestionRepository questionRepository;

    @Mock
    private QuestionPriorityCalculator priorityCalculator;

    @Mock
    private NotificationService notificationService;

    private QuestionService questionService;

    private Seller seller;
    private Buyer buyer;
    private Product product;
    private Order order;

    @BeforeEach
    void setUp() {
        questionService = new QuestionService(questionRepository, priorityCalculator, notificationService);

        seller = new Seller("Test Seller", "seller@test.com");
        buyer = new Buyer("Test Buyer", "buyer@test.com");
        product = new Product(seller, "Test Product", "Electronics", new BigDecimal("100.00"));
        order = new Order(seller, buyer, OrderStatus.PAID, LocalDateTime.now().minusDays(1));
        order.addItem(product, 1);
    }

    @Test
    void answerQuestion_changesStatusToAnsweredAndSetsAnsweredAt() {
        Question question = new Question(order, buyer, product, "Where is my order?", LocalDateTime.now().minusHours(5));
        Long questionId = 1L;
        String answerText = "Your order is on the way.";
        LocalDateTime answeredAt = LocalDateTime.now();

        when(questionRepository.findWithDetailsById(questionId)).thenReturn(Optional.of(question));
        when(priorityCalculator.calculate(any(Question.class), any(LocalDateTime.class)))
                .thenReturn(new PriorityResult(30, QuestionPriority.LOW, List.of()));
        when(questionRepository.save(any(Question.class))).thenAnswer(inv -> inv.getArgument(0));

        QuestionWithPriority result = questionService.answerQuestion(questionId, answerText);

        assertThat(result.question().getStatus()).isEqualTo(QuestionStatus.ANSWERED);
        assertThat(result.question().getAnswer()).isEqualTo(answerText);
        assertThat(result.question().getAnsweredAt()).isNotNull();
        verify(questionRepository).save(question);
        verify(notificationService).notifyIfImportant(question);
    }

    @Test
    void resolveQuestion_changesStatusToResolvedAndSetsResolvedAt() {
        Question question = new Question(order, buyer, product, "How does this work?", LocalDateTime.now().minusHours(10));
        question.answer("Like this.", LocalDateTime.now().minusHours(8));
        Long questionId = 2L;

        when(questionRepository.findWithDetailsById(questionId)).thenReturn(Optional.of(question));
        when(priorityCalculator.calculate(any(Question.class), any(LocalDateTime.class)))
                .thenReturn(new PriorityResult(15, QuestionPriority.LOW, List.of()));
        when(questionRepository.save(any(Question.class))).thenAnswer(inv -> inv.getArgument(0));

        QuestionWithPriority result = questionService.resolveQuestion(questionId);

        assertThat(result.question().getStatus()).isEqualTo(QuestionStatus.RESOLVED);
        assertThat(result.question().getResolvedAt()).isNotNull();
        verify(questionRepository).save(question);
        verify(notificationService, never()).notifyIfImportant(any());
    }

    @Test
    void findUnresolvedSellerQuestions_returnsOnlyOpenAndAnsweredOrderedByPriority() {
        Question highPriority = new Question(order, buyer, product, "Urgent broken", LocalDateTime.now().minusHours(50));
        highPriority.updatePriority(QuestionPriority.HIGH, 80);

        Question lowPriority = new Question(order, buyer, product, "Hello", LocalDateTime.now().minusHours(2));
        lowPriority.updatePriority(QuestionPriority.LOW, 10);

        Question resolved = new Question(order, buyer, product, "Done", LocalDateTime.now().minusHours(20));
        resolved.resolve(LocalDateTime.now().minusHours(1));

        List<Question> unresolved = List.of(highPriority, lowPriority);

        when(questionRepository.findByOrderSellerIdAndStatusNot(1L, QuestionStatus.RESOLVED))
                .thenReturn(unresolved);

        PriorityResult highResult = new PriorityResult(80, QuestionPriority.HIGH, List.of());
        PriorityResult lowResult = new PriorityResult(10, QuestionPriority.LOW, List.of());

        when(priorityCalculator.calculate(eq(highPriority), any(LocalDateTime.class))).thenReturn(highResult);
        when(priorityCalculator.calculate(eq(lowPriority), any(LocalDateTime.class))).thenReturn(lowResult);

        List<QuestionWithPriority> results = questionService.findUnresolvedSellerQuestions(1L);

        assertThat(results).hasSize(2);
        assertThat(results.get(0).priority().score()).isGreaterThanOrEqualTo(results.get(1).priority().score());
        assertThat(results.get(0).question()).isEqualTo(highPriority);
        assertThat(results.get(1).question()).isEqualTo(lowPriority);
    }
}
