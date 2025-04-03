package com.example.ticketinventory.dto.CancelTickets;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "Information about each canceled ticket")
public class CancelledTicket {
    @Schema(description = "Status of ticket before cancellation", example = "sold", required = true)
    private String previous_status;

    @Schema(description = "Event ID", example = "1", required = true)
    private Long event_id;

    @Schema(description = "payment_intent_id", example = "pi_1234567890", required = true)
    private String payment_intent_id;

    @Schema(description = "ID of the canceled ticket", example = "101")
    private Long ticketId;

    @Schema(description = "Seat label", example = "A5")
    private String seat;

    @Schema(description = "User ID", example = "123")
    private Long user_id;

    @Schema(description = "Price of ticket", example = "100.0")
    private Double price;
}
