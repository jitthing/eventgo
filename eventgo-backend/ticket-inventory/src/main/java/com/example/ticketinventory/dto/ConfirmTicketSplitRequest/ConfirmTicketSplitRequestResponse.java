package com.example.ticketinventory.dto.ConfirmTicketSplitRequest;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Schema(description = "Response object for confirming ticket split")
public class ConfirmTicketSplitRequestResponse {
    @Schema(description = "Status of the operation", example = "success", required = true)
    private String status;
    @Schema(description = "Response message", example = "Ticket purchase confirmed for user ID: 1001", required = true)
    private String message;

}
