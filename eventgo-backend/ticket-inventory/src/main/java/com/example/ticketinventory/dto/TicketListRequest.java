package com.example.ticketinventory.dto;

import lombok.Getter;
import lombok.Setter;
import lombok.Builder;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;


import java.util.List;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class TicketListRequest {
    private List<Long> ticketList;

    public List<Long> getTicketList() {
        return ticketList;
    }
    public void setTicketList(List<Long> ticketList) {
        this.ticketList = ticketList;
    }
}
