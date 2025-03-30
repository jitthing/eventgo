package com.example.ticketinventory.dto;


import java.util.List;

public class CancelTicketRequest {
    List<Long> ticketList;

    public List<Long> getTicketList() {
        return ticketList;
    }

    public void setTicketList(List<Long> ticketList) {
        this.ticketList = ticketList;
    }

}
