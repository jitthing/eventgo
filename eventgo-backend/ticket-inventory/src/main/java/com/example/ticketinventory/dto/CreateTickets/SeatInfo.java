package com.example.ticketinventory.dto.CreateTickets;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "Information about a specific seat")
public class SeatInfo {
    @Schema(description = "ID of the ticket", example = "406")
    private Long ticketId;
    
    @Schema(description = "Seat number", example = "A1")
    private String seatNumber;
    
    @Schema(description = "Category of the seat", example = "standard")
    private String category;
    
    @Schema(description = "Status of the seat", example = "available")
    private String status;
} 