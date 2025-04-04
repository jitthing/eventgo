package com.example.ticketinventory.dto.MarkTransferring;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "Request object for marking ticket as transferring")
public class MarkTransferringRequest {
    @Schema(description = "ID of the ticket to mark as transferring", example = "123", required = true)
    private Long ticket_id;
}