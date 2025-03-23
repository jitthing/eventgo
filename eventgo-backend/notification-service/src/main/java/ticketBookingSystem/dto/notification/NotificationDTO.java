package ticketBookingSystem.dto.notification;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.io.Serializable;
import java.util.Date;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class NotificationDTO implements Serializable {
    private static final long serialVersionUID = 1L;
    private UUID notificationId;
    private Date timestamp;
    private String message;
    private String subject;
    private String recipientEmailAddress;
    
    // Getters and setters
    public UUID getNotificationId() { return notificationId; }
    public void setNotificationId(UUID notificationId) { this.notificationId = notificationId; }
    
    public Date getTimestamp() { return timestamp; }
    public void setTimestamp(Date timestamp) { this.timestamp = timestamp; }
    
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    
    public String getSubject() { return subject; }
    public void setSubject(String subject) { this.subject = subject; }
    
    public String getRecipientEmailAddress() { return recipientEmailAddress; }
    public void setRecipientEmailAddress(String recipientEmailAddress) { this.recipientEmailAddress = recipientEmailAddress; }
    
    @Override
    public String toString() {
        return "NotificationDTO{" +
                "notificationId=" + notificationId +
                ", timestamp=" + timestamp +
                ", message='" + message + '\'' +
                ", subject='" + subject + '\'' +
                ", recipientEmailAddress='" + recipientEmailAddress + '\'' +
                '}';
    }
}