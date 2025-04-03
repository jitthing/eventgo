package com.example.ticketinventory.dto.TicketAssociatedUser;

import java.util.List;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "Response object for ticket associated user")
public class TicketAssociatedUserResponse {
    @Schema(
        description = "List of ticket details",
        example = """
            [
                {
                    "eventId": 1,
                    "previousOwner": 1001,
                    "price": 100.0,
                    "currentOwner": 1002,
                    "category": "standard",
                    "ticketId": 1,
                    "seatNumber": "A1",
                    "status": "sold"
                }
            ]
            """
    )
    private List<TicketDetails> data;
    @Schema(description = "Status of the operation", example = "success")
    private String status;
}

class TicketDetails {
    @Schema(description = "Event ID", example = "1")
    private Long eventId;
    @Schema(description = "Previous owner", example = "1001")
    private Long previousOwner;
    @Schema(description = "Price", example = "100.0")
    private Double price;
    @Schema(description = "Current owner", example = "1002")
    private Long currentOwner;
    @Schema(description = "Category", example = "standard")
    private String category;
    @Schema(description = "Ticket ID", example = "1")
    private Long ticketId;
    @Schema(description = "Seat number", example = "A1")
    private String seatNumber;
    @Schema(description = "Status", example = "sold")
    private String status;
}
