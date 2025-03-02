package ticketBookingSystem.dto.TicketsService;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TicketConfirmRequestDTO {
    private String eventId;
    private List<String> seatsId;
    private String userId;
    private String bookingId;
}
