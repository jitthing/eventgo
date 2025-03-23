package ticketBookingSystem.dto.Booking;

import java.util.List;

import ticketBookingSystem.dto.Payment.PaymentRequestDTO;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class ProcessBookingRequestDTO {

    // private String eventId;
    // private List<String> seatsId;
    // private String userId;
    // private String paymentIntentId;
    // private String reservationId;
    private String eventId;
    private List<String> seats; // Make sure this line exists
    private String paymentIntentId;
    private String reservationId; // Add this to match the ticket inventory service
    private String userId;        // Add this to match the ticket inventory service

}
