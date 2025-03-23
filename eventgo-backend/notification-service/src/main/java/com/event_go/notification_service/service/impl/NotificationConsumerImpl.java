package com.event_go.notification_service.service.impl;

import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

import com.event_go.notification_service.model.NotificationEvent;
import ticketBookingSystem.dto.notification.NotificationDTO;

@Component
@Service
public class NotificationConsumerImpl {

    private final NotificationServiceImpl notificationService;

    public NotificationConsumerImpl(NotificationServiceImpl notificationService) {
        this.notificationService = notificationService;
    }

    @RabbitListener(queues = "notification.queue")
    public void receiveEmailNotification(NotificationDTO dto) {
        try {
            System.out.println("Received notification: " + dto);
            
            // Create a notification event from the DTO
            NotificationEvent notification = new NotificationEvent();
            notification.setMessage(dto.getMessage());
            notification.setSubject(dto.getSubject());
            notification.setRecipientEmailAddress(dto.getRecipientEmailAddress());
            
            // Process the notification
            if (notification.getRecipientEmailAddress() != null && notification.getMessage() != null) {
                notificationService.sendEmailNotification(notification);
                System.out.println("Email notification sent to " + notification.getRecipientEmailAddress());
            } else {
                System.out.println("Skipping notification - missing required fields");
            }
        } catch (Exception e) {
            System.err.println("Error processing notification: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
