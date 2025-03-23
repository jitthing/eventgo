package ticketBookingSystem.dto.notification;


import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.util.Date;
import java.util.UUID;

@Getter
@Setter
public class NotificationDTO implements Serializable {
    private static final long serialVersionUID = 1L;
    private UUID notificationId;
    private String recipientPhoneNumber;
    private String recipientEmailAddress;
    private String subject;
    private String message;
    private Date timestamp;
}
