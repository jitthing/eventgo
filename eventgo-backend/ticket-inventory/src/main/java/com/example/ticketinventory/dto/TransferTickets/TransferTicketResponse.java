package com.example.ticketinventory.dto.TransferTickets;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "Response object for ticket transfer")
public class TransferTicketResponse {
    @Schema(description = "Status of the operation", example = "success", required = true)
    private String status;

    @Schema(description = "Response message", example = "Ticket successfully transferred", required = true)
    private String message;

    @Schema(description = "ID of the transferred ticket", example = "1", required = true)
    private Long ticket_id;

    @Schema(description = "ID of the new ticket owner", example = "3001", required = true)
    private Long new_user_id;
}