package com.example.ticketinventory.dto.GetTicketsByEventID;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Schema(
    description = "Response object for when no tickets are found or an error occurs",
    example = """
    {
        "data": [],
        "status": "error"
    }
    """
)
public class TicketResponseError {

    @Schema(
        description = "Empty list indicating no tickets were found",
        example = "[]"
    )
    private List<?> data = List.of();  // Ensure it's never null

    @Schema(
        description = "Status of the operation",
        example = "error",
        allowableValues = {"success", "error"}
    )
    private String status = "error";
}
