package com.event_go.notification_service.service.impl;

import com.event_go.notification_service.model.NotificationEvent;
import com.event_go.notification_service.service.NotificationService;
import jakarta.mail.*;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import java.util.Properties;

@Service
public class NotificationServiceImpl implements NotificationService {

    @Value("${smtp.host}")
    private String smtpHost;

    @Value("${smtp.port}")
    private int smtpPort;

    @Value("${smtp.username}")
    private String smtpUsername;

    @Value("${smtp.password}")
    private String smtpPassword;

    @Value("${email.from}")
    private String fromEmail;

    @Override
    public void sendEmailNotification(NotificationEvent notification) {
        sendEmail(notification.getRecipientEmailAddress(), notification.getMessage(), notification.getSubject());
    }

    private void sendEmail(String recipientEmailAddress, String messageBody, String subject) {
        try {
            Properties properties = new Properties();
            properties.put("mail.smtp.auth", "true");
            properties.put("mail.smtp.starttls.enable", "true");
            properties.put("mail.smtp.host", smtpHost);
            properties.put("mail.smtp.port", String.valueOf(smtpPort));

            Session session = Session.getInstance(properties, new Authenticator() {
                @Override
                protected PasswordAuthentication getPasswordAuthentication() {
                    return new PasswordAuthentication(smtpUsername, smtpPassword);
                }
            });

            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress(fromEmail));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(recipientEmailAddress));
            message.setSubject(subject);
            message.setText(messageBody);

            Transport.send(message);
            System.out.println("✅ Email sent successfully to: " + recipientEmailAddress);
        } catch (Exception e) {
            System.err.println("❌ Failed to send email: " + e.getMessage());
        }
    }
}
