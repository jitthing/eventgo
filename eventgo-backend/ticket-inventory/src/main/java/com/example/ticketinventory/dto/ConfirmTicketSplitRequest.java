package com.example.ticketinventory.dto;

public class ConfirmTicketSplitRequest extends ConfirmTicketRequest {   
    private Long ticketId;

    public Long getTicketId() {
        return ticketId;
    }

    public void setTicketId(Long ticketId) {
        this.ticketId = ticketId;
    }
}
