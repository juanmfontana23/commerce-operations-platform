package com.example.commerceoperations.notifications.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;

import com.example.commerceoperations.notifications.domain.Notification;
import com.example.commerceoperations.orders.domain.Order;
import com.example.commerceoperations.orders.domain.Seller;
import com.example.commerceoperations.questions.domain.Question;

@ExtendWith(OutputCaptureExtension.class)
class EmailNotificationChannelTest {

    @Test
    void sendDoesNotLogRecipientEmail(CapturedOutput output) {
        String recipient = "private-recipient@example.com";
        Notification notification = mock(Notification.class);
        Question question = mock(Question.class);
        Order order = mock(Order.class);
        Seller seller = mock(Seller.class);
        when(notification.getRecipient()).thenReturn(recipient);
        when(notification.getMessage()).thenReturn("Urgent support request");
        when(notification.getQuestion()).thenReturn(question);
        when(question.getOrder()).thenReturn(order);
        when(order.getSeller()).thenReturn(seller);
        when(seller.getId()).thenReturn(1L);

        new EmailNotificationChannel().send(notification);

        assertThat(output).doesNotContain(recipient);
        assertThat(output).contains("recipientId=1");
    }
}
