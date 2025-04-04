package com.example.ticketinventory.dto.ConfirmTickets;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "Response object for ticket confirmation")
public class ConfirmTicketResponse {
    @Schema(description = "Status of the operation", example = "success")
    private String status;

    @Schema(description = "Response message", example = "Purchase successfully confirmed")
    private String message;
}
