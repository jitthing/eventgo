package com.example.ticketinventory.dto.CreateTickets;
    
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import java.util.List;

@Data
@Schema(description = "Response object for ticket creation")
public class CreateTicketsResponse {
    @Schema(description = "Status of the creation operation", example = "success", required = true)
    private String status;

    @Schema(description = "Response message", example = "Created 3 tickets", required = true)
    private String message;

    @Schema(description = "List of created tickets", required = true)
    private List<TicketData> data;
}

@Data
class TicketData {
    @Schema(description = "Event ID", example = "7")
    private Long eventId;

    @Schema(description = "Ticket price", example = "50.0")
    private Double price;

    @Schema(description = "Ticket category", example = "standard")
    private String category;

    @Schema(description = "Ticket ID", example = "119")
    private Long ticketId;

    @Schema(description = "Seat number", example = "A1")
    private String seatNumber;

    @Schema(description = "Ticket status", example = "available")
    private String status;
}