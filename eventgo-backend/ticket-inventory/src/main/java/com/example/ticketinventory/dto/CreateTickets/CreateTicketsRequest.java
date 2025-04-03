package com.example.ticketinventory.dto.CreateTickets;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import java.util.List;

@Data
@Schema(description = "Request for creating tickets with events and category prices")
public class CreateTicketsRequest {
    private List<EventSeats> events;
    private List<CategoryPrice> categoryPrices;
} 