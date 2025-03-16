package ticketBookingSystem.dto.Payment;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PaymentRequestDTO {
    // private double amount;
    // private String currency;
    @JsonProperty("event_id")  // Matches FastAPI's expected field name
    private String eventId;
    
    @JsonProperty("seats")  // Matches FastAPI's expected field name
    private List<String> seatsId;
    
    @JsonProperty("payment_intent_id")  // Matches FastAPI's expected field name
    private String paymentIntentId;
    


}
