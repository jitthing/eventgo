package com.example.ticketinventory.service;

import com.example.ticketinventory.model.Ticket;
import com.example.ticketinventory.model.TicketStatus;
import com.example.ticketinventory.repository.TicketRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.Optional;
import java.util.ArrayList;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.stream.Collectors;
import java.time.*;
import java.time.format.DateTimeFormatter;

@Service
public class TicketService {

    @Autowired
    private TicketRepository ticketRepository;

    public Optional<Ticket> checkSeatAvailability(Long eventId, String seatNumber) {
        return ticketRepository.findByEventIdAndSeatNumber(eventId, seatNumber);
    }


    public Map<String, Object> getTicketsByEvent(Long eventId) {
        List<Ticket> tickets = ticketRepository.findByEventId(eventId);

        List<Map<String, Object>> ticketList = tickets.stream().map(ticket -> {
            Map<String, Object> ticketMap = new HashMap<>();
            ticketMap.put("ticket_id", ticket.getTicketId());
            ticketMap.put("event_id", ticket.getEventId());
            ticketMap.put("seat_number", ticket.getSeatNumber());
            ticketMap.put("status", ticket.getStatus().toString());
            return ticketMap;
        }).collect(Collectors.toList());

        Map<String, Object> response = new HashMap<>();
        response.put("status", "success");
        response.put("data", ticketList);

        return response;
    }

    @Transactional
    public Map<String, Object> reserveSeat(Long eventId, List<String> seatNumbers, Long userId) {
        List<Ticket> reservedTickets = new ArrayList<>();
        long reservationId = System.nanoTime(); // Generate reservation ID

        List<String> unavailableSeats = seatNumbers.stream()
                .filter(seatNumber -> {
                    Optional<Ticket> ticketOpt = ticketRepository.findByEventIdAndSeatNumber(eventId, seatNumber);
                    return ticketOpt.isEmpty() || ticketOpt.get().getStatus() != TicketStatus.available;
                })
                .collect(Collectors.toList());

        if (!unavailableSeats.isEmpty()) {
            return Map.of("status", "failure", "message", "Seats not available: " + String.join(", ", unavailableSeats));
        }

        LocalDateTime expirationTime = LocalDateTime.now(ZoneOffset.UTC).plus(10, ChronoUnit.MINUTES);

        for (String seatNumber : seatNumbers) {
            Ticket ticket = ticketRepository.findByEventIdAndSeatNumber(eventId, seatNumber).get();
            ticket.setStatus(TicketStatus.reserved);
            ticket.setReservationId(reservationId);
            ticket.setReservationExpires(expirationTime);
            reservedTickets.add(ticket);
        }

        ticketRepository.saveAll(reservedTickets);

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'")
                .withZone(ZoneOffset.UTC);
        String formattedExpiration = expirationTime.atOffset(ZoneOffset.UTC).format(formatter);

        return Map.of(
                "status", "success",
                "data", Map.of(
                        "reservation_id", reservationId,
                        "event_id", eventId,
                        "reserved_seats", seatNumbers,
                        "expires_at", formattedExpiration
                )
        );
    }

    @Transactional
    public String releaseSeat(Long reservationId) {
        List<Ticket> tickets = ticketRepository.findByReservationId(reservationId);

        if (tickets.isEmpty()) {
            return "Invalid reservation ID or no reserved tickets found";
        }

        for (Ticket ticket : tickets) {
            if (ticket.getStatus() == TicketStatus.reserved) {
                ticket.setStatus(TicketStatus.available);
                ticket.setReservationExpires(null);
                ticket.setReservationId(null);
            }
        }

        ticketRepository.saveAll(tickets);

        return "All seats released successfully";
    }

    @Transactional
    public String confirmSeat(Long reservationId, Long bookingId) {
        List<Ticket> tickets = ticketRepository.findByReservationId(reservationId);

        if (tickets.isEmpty()) {
            return "Invalid reservation ID or no reserved tickets found";
        }

        for (Ticket ticket : tickets) {
            if (ticket.getStatus() != TicketStatus.reserved) {
                return "Some seats are not in a reserved state";
            }
            ticket.setStatus(TicketStatus.sold);
            ticket.setReservationExpires(null);
            ticket.setReservationId(null);
            ticket.setBookingId(bookingId);
        }

        ticketRepository.saveAll(tickets);

        return "Seat purchase confirmed for booking ID: " + bookingId;
    }
}

