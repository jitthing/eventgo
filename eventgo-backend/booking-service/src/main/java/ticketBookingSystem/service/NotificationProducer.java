package ticketBookingSystem.service;

import org.springframework.stereotype.Service;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import lombok.extern.slf4j.Slf4j;
import ticketBookingSystem.dto.notification.NotificationDTO;

@Slf4j
@Service
public class NotificationProducer {
    
    private final RabbitTemplate rabbitTemplate;
    public static final String NOTIFICATION_QUEUE = "notification.queue";

    public NotificationProducer(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    public void sendNotification(NotificationDTO notification) {
        try {
            log.info("Attempting to send notification to queue: {}", NOTIFICATION_QUEUE);
            log.debug("Notification details - Subject: {}, Recipient: {}", 
                    notification.getSubject(), 
                    notification.getRecipientEmailAddress());
            
            rabbitTemplate.convertAndSend(NOTIFICATION_QUEUE, notification);
            
            log.info("Successfully sent notification to queue: {}", NOTIFICATION_QUEUE);
        } catch (Exception e) {
            log.error("Failed to send notification to queue: {}. Error: ", NOTIFICATION_QUEUE, e);
            throw e; // Re-throw to be handled by caller
        }
    }
}
