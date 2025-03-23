package ticketBookingSystem.dto.notification;

import java.io.Serializable;
import java.util.Date;
import java.util.UUID;

public class NotificationDTO implements Serializable {
    private static final long serialVersionUID = 1L;
    private UUID notificationId;
    private String recipientPhoneNumber;
    private String recipientEmailAddress;
    private String subject;
    private String message;
    private Date timestamp;
    
    // Default constructor
    public NotificationDTO() {}
    
    // Getters and setters
    public UUID getNotificationId() { return notificationId; }
    public void setNotificationId(UUID notificationId) { this.notificationId = notificationId; }
    
    public String getRecipientPhoneNumber() { return recipientPhoneNumber; }
    public void setRecipientPhoneNumber(String recipientPhoneNumber) { this.recipientPhoneNumber = recipientPhoneNumber; }
    
    public String getRecipientEmailAddress() { return recipientEmailAddress; }
    public void setRecipientEmailAddress(String recipientEmailAddress) { this.recipientEmailAddress = recipientEmailAddress; }
    
    public String getSubject() { return subject; }
    public void setSubject(String subject) { this.subject = subject; }
    
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    
    public Date getTimestamp() { return timestamp; }
    public void setTimestamp(Date timestamp) { this.timestamp = timestamp; }
    
    @Override
    public String toString() {
        return "NotificationDTO{" +
                "notificationId=" + notificationId +
                ", recipientPhoneNumber='" + recipientPhoneNumber + '\'' +
                ", recipientEmailAddress='" + recipientEmailAddress + '\'' +
                ", subject='" + subject + '\'' +
                ", message='" + message + '\'' +
                ", timestamp=" + timestamp +
                '}';
    }
}