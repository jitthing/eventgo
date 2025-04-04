package com.example.ticketinventory.dto.TransferTickets;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "Request object for transferring a ticket")
public class TransferTicketRequest {
    @Schema(description = "ID of the ticket to transfer", example = "1", required = true)
    private Long ticket_id;

    @Schema(description = "ID of the current ticket owner", example = "1001", required = true)
    private Long current_user_id;

    @Schema(description = "ID of the new ticket owner", example = "3001", required = true)
    private Long new_user_id;
}