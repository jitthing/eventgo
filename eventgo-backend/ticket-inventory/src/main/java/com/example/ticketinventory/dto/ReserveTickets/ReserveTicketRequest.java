package com.example.ticketinventory.dto.ReserveTickets;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import java.util.List;

@Data
@Schema(description = "Request object for reserving tickets")
public class ReserveTicketRequest {
    @Schema(description = "ID of the event", example = "1001", required = true)
    private Long eventId;

    @Schema(description = "ID of the user making the reservation", example = "2001", required = true)
    private Long userId;

    @Schema(description = "List of seat numbers to reserve", 
           example = "['A1', 'A2', 'A3']", 
           required = true)
    private List<String> seats;
}
