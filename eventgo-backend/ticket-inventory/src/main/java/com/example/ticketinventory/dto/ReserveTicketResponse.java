package com.example.ticketinventory.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Schema(description = "Response object for ticket reservation")
public class ReserveTicketResponse {
    @Schema(description = "Status of the reservation", example = "success")
    private String status;

    @Schema(description = "Unique reservation identifier", example = "6240760612326")
    private Long reservationId;

    @Schema(description = "When the reservation expires", example = "2024-03-21T15:30:00")
    private LocalDateTime expiresAt;

    @Schema(description = "List of reserved seat numbers")
    private List<String> seats;
}