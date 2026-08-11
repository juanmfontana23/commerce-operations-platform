package com.example.commerceoperations.notifications.infrastructure;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.commerceoperations.notifications.domain.Notification;

public interface NotificationRepository extends JpaRepository<Notification, Long> {
}
