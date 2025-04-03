package com.example.ticketinventory.dto.CreateTickets;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import java.util.List;

@Data
@Schema(description = "Event with its associated seats")
public class EventSeats {
    @Schema(description = "ID of the event", example = "7")
    private Long eventId;
    
    @Schema(description = "List of seats for the event")
    private List<SeatInfo> seats;
} 