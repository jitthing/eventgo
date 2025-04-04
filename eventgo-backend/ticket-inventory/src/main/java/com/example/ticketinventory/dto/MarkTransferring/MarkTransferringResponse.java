package com.example.ticketinventory.dto.MarkTransferring;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "Response object for marking ticket as transferring")
public class MarkTransferringResponse {
    @Schema(description = "Status of the operation", example = "success", required = true)
    private String status;

    @Schema(description = "Response message", example = "Ticket marked as transferring", required = true)
    private String message;

    @Schema(description = "ID of the ticket marked as transferring", example = "123", required = true)
    private Long ticket_id;
}