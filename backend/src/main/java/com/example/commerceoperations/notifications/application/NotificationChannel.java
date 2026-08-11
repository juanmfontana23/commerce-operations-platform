package com.example.commerceoperations.notifications.application;

import com.example.commerceoperations.notifications.domain.Notification;
import com.example.commerceoperations.notifications.domain.NotificationChannelType;

public interface NotificationChannel {

    NotificationChannelType type();

    void send(Notification notification);
}
