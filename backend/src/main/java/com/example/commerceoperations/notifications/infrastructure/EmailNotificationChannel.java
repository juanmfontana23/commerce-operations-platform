package com.example.commerceoperations.notifications.infrastructure;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.example.commerceoperations.notifications.application.NotificationChannel;
import com.example.commerceoperations.notifications.domain.Notification;
import com.example.commerceoperations.notifications.domain.NotificationChannelType;

@Component
public class EmailNotificationChannel implements NotificationChannel {

    private static final Logger logger = LoggerFactory.getLogger(EmailNotificationChannel.class);

    @Override
    public NotificationChannelType type() {
        return NotificationChannelType.EMAIL;
    }

    @Override
    public void send(Notification notification) {
        logger.info("Email notification recipientId={} message={}", notification.getQuestion().getOrder().getSeller().getId(), notification.getMessage());
    }
}
