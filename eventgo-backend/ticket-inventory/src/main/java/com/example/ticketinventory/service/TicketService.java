package com.example.ticketinventory.service;

import com.example.ticketinventory.model.*;
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
            ticketMap.put("category", ticket.getCategory().toString());
            ticketMap.put("price", ticket.getPrice());
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
            ticket.setUserId(userId);
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
                ticket.setUserId(null);
            }
        }

        ticketRepository.saveAll(tickets);

        return "All seats released successfully";
    }

    @Transactional
    public String confirmSeat(Long reservationId, Long userId, String paymentIntentId) {
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
            ticket.setUserId(userId);
            ticket.setPaymentIntentId(paymentIntentId);

        }
        ticketRepository.saveAll(tickets);

        return "Seat purchase confirmed for user ID: " + userId;
    }

    @Transactional
    public Map<String, Object> createTickets(Long eventId, List<Map<String, Object>> seatsData) {
        List<Ticket> createdTickets = new ArrayList<>();
        
        for (Map<String, Object> seatData : seatsData) {
            String seatNumber = (String) seatData.get("seatNumber");
            String categoryStr = (String) seatData.get("category");
            String statusStr = seatData.containsKey("status") ? 
                (String) seatData.get("status") : "available";
            Double price = seatData.containsKey("price") ? 
                ((Number) seatData.get("price")).doubleValue() : 0.0;
            
            // Check if seat already exists for this event
            Optional<Ticket> existingTicket = checkSeatAvailability(eventId, seatNumber);
            if (existingTicket.isPresent()) {
                continue; // Skip this seat as it already exists
            }
            
            TicketCategory category;
            try {
                category = TicketCategory.valueOf(categoryStr.toLowerCase());
            } catch (IllegalArgumentException e) {
                category = TicketCategory.standard; // Default to standard if invalid category
            }
            
            TicketStatus status;
            try {
                status = TicketStatus.valueOf(statusStr.toLowerCase());
            } catch (IllegalArgumentException e) {
                status = TicketStatus.available; // Default to available if invalid status
            }
            
            Ticket ticket = new Ticket();
            ticket.setEventId(eventId);
            ticket.setSeatNumber(seatNumber);
            ticket.setCategory(category);
            ticket.setStatus(status); 
            ticket.setPrice(price);
            
            // NEW: Optionally set user_id if provided in the seatData
            if (seatData.containsKey("user_id")) {
                ticket.setUserId(((Number) seatData.get("user_id")).longValue());
            }
            
            createdTickets.add(ticket);
        }
        
        // Save all tickets at once
        List<Ticket> savedTickets = ticketRepository.saveAll(createdTickets);
        
        // Format response
        List<Map<String, Object>> ticketList = savedTickets.stream().map(ticket -> {
            Map<String, Object> ticketMap = new HashMap<>();
            ticketMap.put("ticketId", ticket.getTicketId());
            ticketMap.put("eventId", ticket.getEventId());
            ticketMap.put("seatNumber", ticket.getSeatNumber());
            ticketMap.put("status", ticket.getStatus().toString());
            ticketMap.put("category", ticket.getCategory().toString());
            ticketMap.put("price", ticket.getPrice());
            return ticketMap;
        }).collect(Collectors.toList());
        
        Map<String, Object> response = new HashMap<>();
        response.put("status", "success");
        response.put("message", "Created " + savedTickets.size() + " tickets for event ID: " + eventId);
        response.put("data", ticketList);
        
        return response;
    }
    

    @Transactional
    public Map<String, Object> cancelTicketsByEvent(Long eventId) {
        List<Ticket> tickets = ticketRepository.findByEventId(eventId);
        int canceledCount = 0;
        List<String> paymentIds = new ArrayList<>();

        for (Ticket ticket : tickets) {
            if (ticket.getStatus() == TicketStatus.sold) {
                canceledCount++;
                if (!paymentIds.contains(ticket.getPaymentIntentId())) {
                    paymentIds.add(ticket.getPaymentIntentId());
                }
            }
            ticket.setStatus(TicketStatus.cancelled);
            ticket.setReservationExpires(null);
            ticket.setReservationId(null);
            ticket.setUserId(null);

        }

        ticketRepository.saveAll(tickets);

        Map<String, Object> response = new HashMap<>();
        response.put("status", "success");
        response.put("message", String.format("Successfully canceled %d reserved tickets for event ID: %d", canceledCount, eventId));
        response.put("paymentIds", paymentIds);
        
        return response;
    }

    // get tickets by user id
    public Map<String, Object> getTicketsByUserId(Long userId) {
        List<Ticket> tickets = ticketRepository.findByUserId(userId);
        return getTicketList(tickets);
    }

    private Map<String, Object> getTicketList(List<Ticket> tickets) {
        List<Map<String, Object>> ticketList = tickets.stream().map(ticket -> {
            Map<String, Object> ticketMap = new HashMap<>();
            ticketMap.put("ticketId", ticket.getTicketId());
            ticketMap.put("eventId", ticket.getEventId());
            ticketMap.put("seatNumber", ticket.getSeatNumber());
            ticketMap.put("status", ticket.getStatus().toString());
            ticketMap.put("category", ticket.getCategory().toString());
            ticketMap.put("price", ticket.getPrice());
            return ticketMap;
        }).collect(Collectors.toList());

        Map<String, Object> response = new HashMap<>();
        response.put("status", "success");
        response.put("data", ticketList);
        return response;
    }

    @Transactional
    public Map<String, Object> transferTicket(Long ticketId, Long currentUserId, Long newUserId, String paymentIntentId) {
        Map<String, Object> response = new HashMap<>();
        
        Optional<Ticket> ticketOpt = ticketRepository.findById(ticketId);
        // if (ticketOpt.isEmpty()) {
        //     response.put("status", "error");
        //     response.put("message", "Ticket not found");
        //     return response;
        // }

        Ticket ticket = ticketOpt.get();

        // // Validate current ownership
        // if (ticket.getUserId() == null || !ticket.getUserId().equals(currentUserId)) {
        //     response.put("status", "error");
        //     response.put("message", "Invalid ticket ownership");
        //     return response;
        // }

        // Validate ticket is in sold status (can only transfer purchased tickets)
        // if (ticket.getStatus() != TicketStatus.sold) {
        //     response.put("status", "error");
        //     response.put("message", "Only purchased tickets can be transferred");
        //     return response;
        // }

        // Perform transfer
        ticket.setUserId(newUserId);
        ticket.setPaymentIntentId(paymentIntentId);
        ticketRepository.save(ticket);

        response.put("status", "success");
        response.put("message", "Ticket successfully transferred");
        response.put("ticket_id", ticketId);
        response.put("new_user_id", newUserId);
        
        return response;
    }

    public Map<String, Object> getTicketById(Long ticketId) {
        Map<String, Object> response = new HashMap<>();
        
        Optional<Ticket> ticketOpt = ticketRepository.findById(ticketId);
        if (ticketOpt.isEmpty()) {
            response.put("status", "error");
            response.put("message", "Ticket not found");
            return response;
        }
        
        Ticket ticket = ticketOpt.get();
        response.put("status", "success");
        response.put("ticket_id", ticket.getTicketId());
        response.put("user_id", ticket.getUserId());
        response.put("event_id", ticket.getEventId());
        response.put("status", ticket.getStatus().toString());
        response.put("payment_intent_id", ticket.getPaymentIntentId());
        response.put("price", ticket.getPrice());
        
        return response;
    }

    @Transactional
    public String confirmSeatSplitBooking(Long reservationId, Long userId, String paymentIntentId, Long ticketId) {
        List<Ticket> tickets = ticketRepository.findByReservationId(reservationId);

        if (tickets.isEmpty()) {
            return "Invalid reservation ID or no reserved tickets found";
        }

        Optional<Ticket> ticketToConfirm = tickets.stream()
            .filter(t -> t.getTicketId().equals(ticketId))
            .findFirst();

        if (ticketToConfirm.isEmpty()) {
            return "Ticket not found in this reservation";
        }

        Ticket ticket = ticketToConfirm.get();
        if (ticket.getStatus() != TicketStatus.reserved) {
            return "Ticket is not in a reserved state";
        }

        ticket.setStatus(TicketStatus.sold);
        ticket.setReservationExpires(null);
        ticket.setReservationId(null);
        ticket.setUserId(userId);
        ticket.setPaymentIntentId(paymentIntentId);

        ticketRepository.save(ticket);

        return "Ticket purchase confirmed for user ID: " + userId;
    }
}

