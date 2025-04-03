package com.example.ticketinventory.dto.ReleaseTickets;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "Response object for ticket release operation")
public class ReleaseTicketResponseError {
    @Schema(
        description = "Status of the operation",
        example = "failure",
        required = true
    )
    private String status;

    @Schema(
        description = "Detailed error message",
        example = "Invalid reservation ID or no reserved tickets found",
        required = true
    )
    private String message;
}