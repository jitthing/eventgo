package com.example.ticketinventory.dto;

public class PreferenceRequest {
    private Long eventId;
    private String seatId;
    private String preference;
    
    // Getters and setters
    public Long getEventId() { return eventId; }
    public void setEventId(Long eventId) { this.eventId = eventId; }
    
    public String getSeatId() { return seatId; }
    public void setSeatId(String seatId) { this.seatId = seatId; }
    
    public String getPreference() { return preference; }
    public void setPreference(String preference) { this.preference = preference; }
}