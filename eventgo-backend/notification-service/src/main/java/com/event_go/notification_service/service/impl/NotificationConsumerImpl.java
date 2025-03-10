package com.event_go.notification_service.service.impl;

import com.event_go.notification_service.config.RabbitMQConfig;
import com.event_go.notification_service.model.NotificationEvent;
import com.event_go.notification_service.service.NotificationConsumer;
import com.event_go.notification_service.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class NotificationConsumerImpl implements NotificationConsumer {

    private final NotificationService notificationService;

    @RabbitListener(queues = RabbitMQConfig.NOTIFICATION_QUEUE)
    public void receiveNotification(NotificationEvent notification) {
        System.out.println("Received notification: " + notification);
        notificationService.sendNotification(notification);

    }
}
