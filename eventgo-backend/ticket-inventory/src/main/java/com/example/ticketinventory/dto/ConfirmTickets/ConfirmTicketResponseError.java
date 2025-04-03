package com.example.ticketinventory.dto.ConfirmTickets;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "Response object for ticket confirmation")
public class ConfirmTicketResponseError {
    @Schema(description = "Status of the operation", example = "error", required = true)
    private String status;

    @Schema(description = "Response message", example = "Invalid reservation ID or no reserved tickets found", required = true)
    private String message;
}