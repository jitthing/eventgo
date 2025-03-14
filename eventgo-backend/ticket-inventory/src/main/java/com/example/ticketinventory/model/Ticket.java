package com.example.ticketinventory.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "tickets")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Ticket {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ticket_id")
    private Long ticketId;

    @Column(name = "event_id", nullable = false)
    private Long eventId;

    @Column(name = "seat_number", length = 10, nullable = false)
    private String seatNumber;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private TicketStatus status; // @ Ticket Status

    @Enumerated(EnumType.STRING)
    @Column(name = "category", nullable = false)
    private TicketCategory category; // @ Ticket Category

    @Column(name = "reservation_expires")
    private LocalDateTime reservationExpires; // logic 10 minutes

    @Column(name = "reservation_id", nullable = true)
    private Long reservationId;

//    @Column(name = "booking_id", nullable = true)
//    private Long bookingId;

    @Column(name = "user_id", nullable = true)
    private Long userId;

    @Column(name = "price", nullable = false)
    private Double price;

    @Column(name = "payment_intent_id", nullable = true, length = 255)
    private String paymentIntentId;

}
