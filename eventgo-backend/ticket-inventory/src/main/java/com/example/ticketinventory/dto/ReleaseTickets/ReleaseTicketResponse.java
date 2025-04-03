package com.example.ticketinventory.dto.ReleaseTickets;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "Response object for ticket release operation")
public class ReleaseTicketResponse {
    @Schema(description = "Status of the release operation", example = "success", required = true,
            allowableValues = {"success", "failure"})
    private String status;

    @Schema(description = "Message describing the result of the operation", 
            example = "All seats released successfully", required = true)
    private String message;

    // Getters and setters
    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}