package com.example.commerceoperations.questions.domain;

import java.time.LocalDateTime;

import com.example.commerceoperations.orders.domain.Buyer;
import com.example.commerceoperations.orders.domain.Order;
import com.example.commerceoperations.orders.domain.Product;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "questions")
public class Question {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "buyer_id", nullable = false)
    private Buyer buyer;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id")
    private Product product;
    @Column(nullable = false, length = 1_000)
    private String message;
    @Column(length = 1_000)
    private String answer;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private QuestionStatus status;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private QuestionPriority priority;
    @Column(nullable = false)
    private int priorityScore;
    @Column(nullable = false)
    private LocalDateTime createdAt;
    private LocalDateTime answeredAt;
    private LocalDateTime resolvedAt;

    protected Question() {
    }

    public Question(Order order, Buyer buyer, Product product, String message, LocalDateTime createdAt) {
        this.order = order;
        this.buyer = buyer;
        this.product = product;
        this.message = message;
        this.createdAt = createdAt;
        this.status = QuestionStatus.OPEN;
        this.priority = QuestionPriority.LOW;
        this.priorityScore = 0;
    }

    public void answer(String answer, LocalDateTime answeredAt) {
        this.answer = answer;
        this.answeredAt = answeredAt;
        if (status == QuestionStatus.OPEN) {
            status = QuestionStatus.ANSWERED;
        }
    }

    public void resolve(LocalDateTime resolvedAt) {
        this.resolvedAt = resolvedAt;
        this.status = QuestionStatus.RESOLVED;
    }

    public void updatePriority(QuestionPriority priority, int priorityScore) {
        this.priority = priority;
        this.priorityScore = priorityScore;
    }

    public Long getId() {
        return id;
    }

    public Order getOrder() {
        return order;
    }

    public Buyer getBuyer() {
        return buyer;
    }

    public Product getProduct() {
        return product;
    }

    public String getMessage() {
        return message;
    }

    public String getAnswer() {
        return answer;
    }

    public QuestionStatus getStatus() {
        return status;
    }

    public QuestionPriority getPriority() {
        return priority;
    }

    public int getPriorityScore() {
        return priorityScore;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getAnsweredAt() {
        return answeredAt;
    }

    public LocalDateTime getResolvedAt() {
        return resolvedAt;
    }
}
