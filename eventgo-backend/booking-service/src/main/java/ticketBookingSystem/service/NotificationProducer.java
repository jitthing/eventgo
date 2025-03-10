package ticketBookingSystem.service;

import org.springframework.stereotype.Service;

import ticketBookingSystem.dto.notification.NotificationDTO;

import org.springframework.amqp.rabbit.core.RabbitTemplate;


@Service
public class NotificationProducer {
    
    private final RabbitTemplate rabbitTemplate;
    public static final String NOTIFICATION_QUEUE = "notification.queue";

    public NotificationProducer(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    public void sendNotification(NotificationDTO notification) {
        rabbitTemplate.convertAndSend(NOTIFICATION_QUEUE, notification);
        System.out.println("Sent notification: " + notification);
    }
}
