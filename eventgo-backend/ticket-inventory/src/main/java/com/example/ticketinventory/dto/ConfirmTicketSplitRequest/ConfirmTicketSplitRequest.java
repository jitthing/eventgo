package com.example.ticketinventory.dto.ConfirmTicketSplitRequest;

import com.example.ticketinventory.dto.ConfirmTickets.ConfirmTicketRequest;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Schema(description = "Request object for confirming ticket split")
public class ConfirmTicketSplitRequest extends ConfirmTicketRequest {
    @Schema(description = "ID of the ticket to confirm", example = "1", required = true)
    private Long ticketId;
    @Schema(description = "ID of the reservation to confirm", example = "6240760612326", required = true)
    private Long reservationId;
    @Schema(description = "ID of the user to confirm", example = "1001", required = true)
    private Long userId;
    @Schema(description = "Payment intent ID", example = "pi_3Oy4Xd2eZvKYlo2C1gkTHPaK", required = true)
    private String paymentIntentId;
}
