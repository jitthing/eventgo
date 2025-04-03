package com.example.ticketinventory.dto.ReleaseTickets;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "Request object for releasing reserved tickets")
public class ReleaseTicketRequest {
    @Schema(description = "Reservation ID to release", example = "6240760612326", required = true)
    private Long reservation_id;  // Using snake_case to match the API contract

    // Getters and setters
    public Long getReservation_id() {
        return reservation_id;
    }

    public void setReservation_id(Long reservation_id) {
        this.reservation_id = reservation_id;
    }
}