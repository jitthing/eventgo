package com.example.ticketinventory.dto.CreateTickets;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import java.util.List;

@Data
@Schema(description = "Error Response object for ticket creation")
public class CreateTicketsResponseError {
    @Schema(description = "Status of the operation", example = "success", required = true)
    private String status;

    @Schema(description = "Error message", example = "Created 0 tickets", required = true)
    private String message;

    @Schema(description = "Empty data array for consistency", required = true)
    private List<Object> data = List.of();
}