package ticketBookingSystem.dto.Booking;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Response payload for a processed booking request.")
public class ProcessBookingResponseDTO {

    @Schema(description = "Status of the booking process", example = "SUCCESS")
    private String status;

    @Schema(description = "Error message if booking failed", example = "Payment validation failed")
    private String errorMessage;

    @Schema(description = "Confirmation message if booking succeeded", example = "Booking confirmed for event EVT123 with payment ID pi_3JcXZa2eZvKYlo2C1TbC8")
    private String confirmationMessage;
}
