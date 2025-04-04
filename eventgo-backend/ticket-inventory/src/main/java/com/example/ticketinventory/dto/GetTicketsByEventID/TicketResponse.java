package com.example.ticketinventory.dto.GetTicketsByEventID;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import java.util.List;
import java.util.Map;

@Data
@Schema(
    description = "Response object containing ticket information and statistics for an event",
    example = """
    {
        "eventId": 1775,
        "tickets": [
            {
                "ticket_id": 108,
                "seat_number": "A1",
                "status": "available",
                "category": "standard",
                "price": 210.0
            }
        ],
        "totalCount": 100,
        "statusCounts": {
            "available": 80,
            "sold": 15,
            "reserved": 5
        },
        "price": 210.0
    }
    """
)
public class TicketResponse {
    @Schema(
        description = "Event identifier",
        example = "1775"
    )
    private Long eventId;
    
    @Schema(
        description = "List of tickets with their details",
        example = """
        [
            {
                "ticket_id": 108,
                "seat_number": "A1",
                "status": "available",
                "category": "standard",
                "price": 210.0
            }
        ]
        """
    )
    private List<Map<String, Object>> tickets;
    
    @Schema(
        description = "Total number of tickets for this event",
        example = "100"
    )
    private Integer totalCount;
    
    @Schema(
        description = "Count of tickets grouped by their status",
        example = """
        {
            "available": 80,
            "sold": 15,
            "reserved": 5
        }
        """
    )
    private Map<String, Long> statusCounts;

    @Schema(
        description = "Base ticket price in SGD",
        example = "210.0"
    )
    private Double price;
}