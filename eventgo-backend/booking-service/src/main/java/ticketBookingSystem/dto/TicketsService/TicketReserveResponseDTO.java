package ticketBookingSystem.dto.TicketsService;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class TicketReserveResponseDTO {
    private Boolean status;
    private String bookingId;
    private String eventId;
    private List<String> seatsId;
    private Double amount;
    private String currency;



}
