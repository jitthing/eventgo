package ticketBookingSystem.dto.Booking;

import java.util.List;

import ticketBookingSystem.dto.Payment.PaymentRequestDTO;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
//@Getter
//@Setter
public class InitiateBookingRequestDTO {

    private String eventId;
    private List<String> seatsId;
    private String userId;

    private PaymentRequestDTO paymentDetailsDTO;


}
