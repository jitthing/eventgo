package com.event_go.notification_service.service.impl;

import com.twilio.rest.api.v2010.account.Message;
import com.event_go.notification_service.model.NotificationEvent;
import com.event_go.notification_service.service.NotificationService;
import com.twilio.type.PhoneNumber;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class NotificationServiceImpl implements NotificationService {

    @Value("${twilio.phone_number}")
    private String twilioPhoneNumber;

    @Override
    public void sendNotification(NotificationEvent notification) {
        sendSMS(notification.getRecipientPhoneNumber(), notification.getMessage());
    }

    private void sendSMS(String recipientPhoneNumber, String messageBody) {
        Message message = Message.creator(
                new PhoneNumber(recipientPhoneNumber),
                new PhoneNumber(twilioPhoneNumber),
                messageBody
        ).create();

        System.out.println("Message sent to " + recipientPhoneNumber);
    }
}
