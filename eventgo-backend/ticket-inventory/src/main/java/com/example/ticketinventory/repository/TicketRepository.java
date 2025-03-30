package com.example.ticketinventory.repository;

import com.example.ticketinventory.model.Ticket;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TicketRepository extends JpaRepository<Ticket, Long> {
    Optional<Ticket> findByEventIdAndSeatNumber(Long eventId, String seatNumber);

    List<Ticket> findByReservationId(Long reservationId); // Fix: return list instead of Optional

    List<Ticket> findByUserId(Long userId); // Fetch confirmed tickets

    List<Ticket> findByEventId(Long eventId); // Fetch all tickets for an event

    // New method to find tickets by current owner or previous owner
    List<Ticket> findByUserIdOrPreviousOwnerUserId(Long userId, Long previousOwnerUserId);

}
