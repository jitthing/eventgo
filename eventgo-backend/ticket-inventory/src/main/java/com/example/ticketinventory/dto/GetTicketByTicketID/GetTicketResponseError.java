package com.example.ticketinventory.dto.GetTicketByTicketID;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Schema(description = "Response for single ticket lookup")
public class GetTicketResponseError {
    @Schema(description = "Status of the response", example = "error")
    private String status;
    @Schema(description = "Message of the response", example = "Ticket not found")
    private String message;

}
