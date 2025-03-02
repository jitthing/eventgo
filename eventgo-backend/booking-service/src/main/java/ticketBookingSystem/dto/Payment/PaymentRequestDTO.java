package ticketBookingSystem.dto.Payment;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PaymentRequestDTO {
    private double amount;
    private String currency;
    private String eventId;
    private List<String> seatsId;
}
