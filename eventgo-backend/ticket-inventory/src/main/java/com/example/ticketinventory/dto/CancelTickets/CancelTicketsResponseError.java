package com.example.ticketinventory.dto.CancelTickets;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import java.util.List;

@Data
@Schema(description = "Response object for ticket cancellation")
public class CancelTicketsResponseError {
    @Schema(description = "Status of the operation", example = "success", required = true)
    private String status;

    @Schema(description = "Response message", example = "Successfully canceled 0 sold tickets for event ID: 8", required = true)
    private String message;

    @Schema(description = "List of canceled ticket information", required = true)
    private List<Object> cancellations = List.of();
}
