package com.event_go.notification_service.model;


import lombok.Getter;
import lombok.Setter;

import java.util.Date;
import java.util.UUID;

@Getter
@Setter
public class NotificationEvent {
    private UUID notificationId;
    private String recipientPhoneNumber;
    private String message;
    private Date timestamp;
}
