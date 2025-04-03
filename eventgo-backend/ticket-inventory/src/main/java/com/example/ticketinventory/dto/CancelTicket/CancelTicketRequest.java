package com.example.ticketinventory.dto.CancelTicket;


import java.util.List;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Request for canceling tickets")
public class CancelTicketRequest {
    @Schema(description = "List of ticket IDs to cancel", example = "[1, 2, 3]")
    private List<Long> ticketList;

    public List<Long> getTicketList() {
        return ticketList;
    }

    public void setTicketList(List<Long> ticketList) {
        this.ticketList = ticketList;
    }

}
