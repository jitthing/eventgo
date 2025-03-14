package ticketBookingSystem.dto.Booking;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class ProcessBookingResponseDTO {
    private String status;
    private String errorMessage;
}
