package com.event_go.notification_service.service;

import com.event_go.notification_service.model.NotificationEvent;

public interface NotificationConsumer {

    public void receiveNotification(NotificationEvent notification);
}
