package com.example.ticketinventory.dto.TicketListRequest;

import java.util.List;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import io.swagger.v3.oas.annotations.media.Schema;
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Ticket List Response")
public class TicketListResponse {
    @Schema(description = "Ticket List")
    private List<TicketData> data;
    @Schema(description = "Status")
    private String status;
}

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Ticket Data")
class TicketData {
    @Schema(description = "Ticket ID", example = "1")
    private Long ticketId;
    @Schema(description = "Event ID", example = "1")
    private Long eventId;
    @Schema(description = "Seat Number", example = "A1")
    private String seatNumber;
    @Schema(description = "Status", example = "available")
    private String status;
    @Schema(description = "Category", example = "VIP")
    private String category;
    @Schema(description = "Reservation Expires", example = "null")
    private String reservationExpires; 
    @Schema(description = "Reservation ID", example = "null")
    private String reservationId;
    @Schema(description = "User ID", example = "0")
    private Long userId;
    @Schema(description = "Price", example = "100.00")
    private Double price;
    @Schema(description = "Payment Intent ID", example = "string")
    private String paymentIntentId;
    @Schema(description = "Preference", example = "keep")
    private String preference;
    @Schema(description = "Previous Owner User ID", example = "1")
    private Long previousOwnerUserId;
}