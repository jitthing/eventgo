package ticketBookingSystem.dto.Booking;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class InitiateBookingResponseDTO {
    private String status;
    private String bookingId;
    private String errorMessage;
}
