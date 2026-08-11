package com.example.commerceoperations.notifications.domain;

import java.time.LocalDateTime;

import com.example.commerceoperations.questions.domain.Question;

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
@Table(name = "notifications")
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "question_id", nullable = false)
    private Question question;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private NotificationChannelType channelType;
    @Column(nullable = false)
    private String recipient;
    @Column(nullable = false, length = 1_000)
    private String message;
    @Column(nullable = false)
    private LocalDateTime createdAt;

    protected Notification() {
    }

    public Notification(Question question, NotificationChannelType channelType, String recipient, String message, LocalDateTime createdAt) {
        this.question = question;
        this.channelType = channelType;
        this.recipient = recipient;
        this.message = message;
        this.createdAt = createdAt;
    }

    public Long getId() {
        return id;
    }

    public Question getQuestion() {
        return question;
    }

    public NotificationChannelType getChannelType() {
        return channelType;
    }

    public String getRecipient() {
        return recipient;
    }

    public String getMessage() {
        return message;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
