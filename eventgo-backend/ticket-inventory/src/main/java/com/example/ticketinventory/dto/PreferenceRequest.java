package com.example.ticketinventory.dto;

public class PreferenceRequest {
    private Long ticketId;
    private String preference;
    
    // Getters and setters
    public Long getTicketId() { return ticketId; }
    public void setSeatId(Long seatId) { this.ticketId = ticketId; }
    
    public String getPreference() { return preference; }
    public void setPreference(String preference) { this.preference = preference; }
}