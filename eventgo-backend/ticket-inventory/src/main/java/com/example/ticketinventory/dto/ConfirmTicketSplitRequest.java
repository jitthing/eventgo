package com.example.ticketinventory.dto;

public class ConfirmTicketSplitRequest extends ConfirmTicketRequest {
    private Long ticketId;
    private Long reservationId;
    private Long userId;
    private String paymentIntentId;

    public Long getTicketId() {
        return ticketId;
    }

    public void setTicketId(Long ticketId) {
        this.ticketId = ticketId;
    }

    public Long getReservationId(){
        return reservationId;
    }
    public void setReservationId(Long reservationId){
        this.reservationId = reservationId;
    }

    public Long getUserId() {
        return userId;
    }
    public void setUserId(Long userId) {
        this.userId = userId;
    }
    public String getPaymentIntentId() {
        return paymentIntentId;
    }
    public void setPaymentIntentId(String paymentIntentId) {
        this.paymentIntentId = paymentIntentId;
    }

}
