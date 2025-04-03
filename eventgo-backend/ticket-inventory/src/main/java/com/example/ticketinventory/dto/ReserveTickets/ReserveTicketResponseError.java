package com.example.ticketinventory.dto.ReserveTickets;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;


@Data
@Schema(description = "Error Response object for ticket reservation")
public class ReserveTicketResponseError {
    @Schema(
        description = "Status of the operation",
        example = "failure",
        required = true
    )
    private String status;

    @Schema(
        description = "Detailed error message",
        example = "Seats not available: A1, A2, A3, A4",
        required = true
    )
    private String message;
}