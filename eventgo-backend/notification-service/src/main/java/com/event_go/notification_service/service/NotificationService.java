package com.event_go.notification_service.service;

import com.event_go.notification_service.model.NotificationEvent;
import org.springframework.stereotype.Service;

@Service
public interface NotificationService {

    void sendNotification(NotificationEvent notification);
}
