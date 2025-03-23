package com.event_go.notification_service.model;

import lombok.Getter;
import lombok.Setter;
import java.io.Serializable;

@Getter
@Setter
public class NotificationEvent implements Serializable {
    // private String toPhoneNumber;
    private String message;
    private String subject;
    private String recipientEmailAddress;

    // public NotificationEvent() {}

    // public NotificationEvent(String toPhoneNumber, String message) {
    //     this.toPhoneNumber = toPhoneNumber;
    //     this.message = message;
    // }

    // public String getToPhoneNumber() { return toPhoneNumber; }
    // public void setToPhoneNumber(String toPhoneNumber) { this.toPhoneNumber = toPhoneNumber; }

    // public String getMessage() { return message; }
    // public void setMessage(String message) { this.message = message; }

    // @Override
    // public String toString() {
    //     return "NotificationEvent{toPhoneNumber='" + toPhoneNumber + "', message='" + message + "'}";
    // }
}
