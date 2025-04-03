package com.example.ticketinventory.dto.CreateTickets;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "Price information for a ticket category")
public class CategoryPrice {
    @Schema(description = "Category name", example = "VIP")
    private String category;
    
    @Schema(description = "Price for the category", example = "100.00")
    private Double price;
} 