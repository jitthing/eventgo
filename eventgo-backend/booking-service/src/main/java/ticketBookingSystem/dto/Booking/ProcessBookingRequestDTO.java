package ticketBookingSystem.dto.Booking;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Schema(description = "Request payload for processing a booking, including seat selection, payment details, and user info.")
public class ProcessBookingRequestDTO {

    @Schema(description = "ID of the event being booked", example = "EVT123", required = true)
    private String eventId;

    @Schema(description = "List of seat IDs the user wants to reserve", example = "[\"A1\", \"A2\", \"A3\"]", required = true)
    private List<String> seats;

    @Schema(description = "Stripe Payment Intent ID", example = "pi_3JcXZa2eZvKYlo2C1TbC8", required = true)
    private String paymentIntentId;

    @Schema(description = "Reservation ID from ticket inventory", example = "RSV789", required = true)
    private String reservationId;

    @Schema(description = "User ID making the booking", example = "USR456", required = true)
    private String userId;

    @Schema(description = "User's email address to send booking confirmation", example = "jane.doe@example.com", required = true)
    private String userEmail;

    @Schema(description = "Total amount to charge the user (in USD)", example = "149.99", required = true)
    private float totalAmount;
}
