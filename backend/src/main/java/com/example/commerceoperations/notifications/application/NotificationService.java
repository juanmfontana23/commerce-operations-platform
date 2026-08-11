package com.example.commerceoperations.notifications.application;

import java.time.LocalDateTime;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.commerceoperations.notifications.domain.Notification;
import com.example.commerceoperations.notifications.domain.NotificationChannelType;
import com.example.commerceoperations.notifications.infrastructure.NotificationRepository;
import com.example.commerceoperations.questions.domain.Question;
import com.example.commerceoperations.questions.domain.QuestionPriority;

@Service
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final Map<NotificationChannelType, NotificationChannel> channels;

    public NotificationService(NotificationRepository notificationRepository, List<NotificationChannel> channels) {
        this.notificationRepository = notificationRepository;
        this.channels = new EnumMap<>(NotificationChannelType.class);
        channels.forEach(channel -> this.channels.put(channel.type(), channel));
    }

    @Transactional
    public void notifyIfImportant(Question question) {
        if (question.getPriority() != QuestionPriority.HIGH && question.getPriority() != QuestionPriority.CRITICAL) {
            return;
        }

        Notification notification = notificationRepository.save(new Notification(
                question,
                NotificationChannelType.EMAIL,
                question.getOrder().getSeller().getEmail(),
                "Question " + question.getId() + " needs attention with priority " + question.getPriority(),
                LocalDateTime.now()));
        channels.get(NotificationChannelType.EMAIL).send(notification);
    }
}
