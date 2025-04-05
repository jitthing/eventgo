package com.example.ticketinventory.dto.TicketListRequest;

import lombok.Getter;
import lombok.Setter;
import lombok.Builder;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "Ticket List Request")
public class TicketListRequest {
    @Schema(description = "Ticket List", required = true, example = "[1, 2, 3]")
    private List<Long> ticketList;

    public List<Long> getTicketList() {
        return ticketList;
    }
    public void setTicketList(List<Long> ticketList) {
        this.ticketList = ticketList;
    }
}
