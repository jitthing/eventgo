package com.example.ticketinventory.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import java.util.List;
import java.util.Map;

@Data
@Schema(description = "Response object for ticket information")
public class TicketResponse {
    @Schema(description = "Event ID", example = "1001")
    private Long eventId;
    
    @Schema(description = "List of tickets")
    private List<Map<String, Object>> tickets;
    
    @Schema(description = "Total number of tickets", example = "100")
    private Integer totalCount;
    
    @Schema(description = "Count of tickets by status")
    private Map<String, Long> statusCounts;
}