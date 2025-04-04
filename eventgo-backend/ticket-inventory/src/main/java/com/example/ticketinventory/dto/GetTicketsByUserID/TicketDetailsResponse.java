package com.example.ticketinventory.dto.GetTicketsByUserID;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "Response object containing ticket details")
public class TicketDetailsResponse {
    
    @Schema(description = "List of ticket details")
    private List<TicketDetail> data;
    
    @Schema(description = "Status of the operation", example = "success")
    private String status;
}

@Data
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "Individual ticket detail")
class TicketDetail {
    @Schema(description = "Event identifier", example = "24")
    private Long eventId;
    
    @Schema(description = "Ticket price", example = "100.0")
    private Double price;
    
    @Schema(description = "Ticket category", example = "standard")
    private String category;
    
    @Schema(description = "Ticket identifier", example = "105")
    private Long ticketId;
    
    @Schema(description = "Seat number", example = "A3")
    private String seatNumber;
    
    @Schema(description = "Ticket status", example = "sold")
    private String status;
}