package com.example.ticketinventory.dto.ConfirmTickets;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "Request object for confirming ticket purchase")
public class ConfirmTicketRequest {
    @Schema(description = "Reservation ID to confirm", example = "6240760612326", required = true)
    private Long reservationId;

    @Schema(description = "User ID making the purchase", example = "2001", required = true)
    private Long userId;

    @Schema(description = "Payment intent ID from Stripe", example = "pi_3Oy4Xd2eZvKYlo2C1gkTHPaK", required = true)
    private String paymentIntentId;
}