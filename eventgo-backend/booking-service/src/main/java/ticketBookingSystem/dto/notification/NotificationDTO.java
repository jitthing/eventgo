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
    private UUID notificationId;
    private Date timestamp;
    private String message;
    private String subject;
    private String recipientEmailAddress;
}
