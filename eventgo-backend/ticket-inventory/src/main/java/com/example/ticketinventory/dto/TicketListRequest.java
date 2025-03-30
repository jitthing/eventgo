package com.example.ticketinventory.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

public class TicketListRequest {
    private List<Long> ticketList;

    public List<Long> getTicketList() {
        return ticketList;
    }
    public void setTicketList(List<Long> ticketList) {
        this.ticketList = ticketList;
    }
}
