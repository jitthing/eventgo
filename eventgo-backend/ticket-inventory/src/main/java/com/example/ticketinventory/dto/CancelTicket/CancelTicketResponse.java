package com.example.ticketinventory.dto.CancelTicket;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Response for single ticket lookup")
public class CancelTicketResponse {
    @Schema(
        description = "List containing single ticket details",
        example = """
            [
                {
                    "ticketId": 137,
                    "eventId": 24,
                    "seatNumber": "A2",
                    "status": "sold",
                    "category": "standard",
                    "reservationExpires": null,
                    "reservationId": null,
                    "userId": 0,
                    "price": 50.0,
                    "paymentIntentId": "string",
                    "preference": null,
                    "previousOwnerUserId": null
                }
            ]
            """
    )
    private List<TicketDetail> data;

    @Schema(description = "Status of the operation", example = "success")
    private String status;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TicketDetail {
        @Schema(description = "Unique identifier for the ticket", example = "137")
        private Long ticketId;

        @Schema(description = "ID of the event this ticket is for", example = "24")
        private Long eventId;

        @Schema(description = "Seat number", example = "A2")
        private String seatNumber;

        @Schema(description = "Current status of the ticket", example = "sold")
        private String status;

        @Schema(description = "Ticket category", example = "standard")
        private String category;

        @Schema(description = "Expiration time of reservation", example = "null")
        private String reservationExpires;

        @Schema(description = "Reservation identifier", example = "null")
        private String reservationId;

        @Schema(description = "Current user ID", example = "0")
        private Long userId;

        @Schema(description = "Ticket price", example = "50.0")
        private Double price;

        @Schema(description = "Payment intent identifier", example = "string")
        private String paymentIntentId;

        @Schema(description = "User's preference for split booking", example = "null")
        private String preference;

        @Schema(description = "Previous owner's user ID", example = "null")
        private Long previousOwnerUserId;
    }
}