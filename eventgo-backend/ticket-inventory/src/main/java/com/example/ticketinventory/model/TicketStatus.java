package com.example.ticketinventory.model;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Status of a ticket")
public enum TicketStatus {
    @Schema(description = "Ticket is available for purchase")
    available,

    @Schema(description = "Ticket is temporarily reserved (10-minute hold)")
    reserved,

    @Schema(description = "Ticket has been purchased")
    sold,

    @Schema(description = "Ticket has been cancelled")
    cancelled,

    @Schema(description = "Ticket is in the process of being transferred")
    transferring
}
