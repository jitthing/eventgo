package ticketBookingSystem.dto.TicketsService;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TicketConfirmResponseDTO {
    private Boolean status;
    private String bookingId;
    private String eventId;
    private List<String> seatsId;
}
