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
import java.time.temporal.ChronoUnit;
import java.util.stream.Collectors;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.*;

import java.util.logging.Logger;

@Service
public class TicketService {

    @Autowired
    private TicketRepository ticketRepository;

    // Inject the scheduler bean from your configuration
    @Autowired
    private ScheduledExecutorService scheduler;

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

    private static final Logger logger = Logger.getLogger(TicketService.class.getName());

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
            return Map.of("status", "failure", "message",
                    "Seats not available: " + String.join(", ", unavailableSeats));
        }

        LocalDateTime expirationTime = LocalDateTime.now(ZoneOffset.UTC).plus(10, ChronoUnit.MINUTES);

        logger.info("Test log abracadabra");

        for (String seatNumber : seatNumbers) {
            Ticket ticket = ticketRepository.findByEventIdAndSeatNumber(eventId, seatNumber).get();
            ticket.setStatus(TicketStatus.reserved);
            ticket.setReservationId(reservationId);
            ticket.setReservationExpires(expirationTime);
            ticket.setUserId(userId);
            reservedTickets.add(ticket);
            logger.info("Inside for loop");
            Runnable task = () -> checkTicketConfirmation(eventId, seatNumber);

            // Schedule the task to run after a 60 seconds delay
            scheduler.schedule(task, 10, TimeUnit.MINUTES);
            logger.info("after scheduler starts - 10mins");
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
                        "expires_at", formattedExpiration));
    }

    // Mark ticket as transferring in progress
    @Transactional
    public Map<String, Object> markTicketTransferring(Long ticketId) {
        Optional<Ticket> ticketOpt = ticketRepository.findById(ticketId);
        if (ticketOpt.isEmpty()) {
            return Map.of("status", "error", "message", "Ticket not found");
        }
        Ticket ticket = ticketOpt.get();
        ticket.setStatus(TicketStatus.transferring);
        ticketRepository.save(ticket);

        return Map.of(
                "status", "success",
                "message", "Ticket marked as transferring",
                "ticket_id", ticket.getTicketId());
    }

    // Check if ticket is confirmed, else release
    @Transactional
    private void checkTicketConfirmation(Long eventId, String seatNumber) {

        Ticket ticket = ticketRepository.findByEventIdAndSeatNumber(eventId, seatNumber).get();
        if (ticket.getStatus() == TicketStatus.reserved) {
            ticket.setStatus(TicketStatus.available);
            ticket.setReservationExpires(null);
            ticket.setReservationId(null);
            ticket.setUserId(null);
        }

        ticketRepository.save(ticket);
        logger.info("released tickets");
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
    public String updatePreference(Long ticketId, String preference) {
        Ticket ticket = ticketRepository.findByTicketId(ticketId);
//        if (ticketOpt.isEmpty()){
//            return "Invalid event ID or no reserved tickets found";
//        }
//        Ticket ticket = ticketOpt.get();
        ticket.setPreference(preference);
        ticketRepository.save(ticket);

        return "Preference updated successfully";
    };

    @Transactional
    public Map<String, Object> getTicketsById(List<Long> ticketList){
        List<Ticket> tickets = new ArrayList<>();
        for( Long id : ticketList){
            Ticket ticket = ticketRepository.findByTicketId(id);
            tickets.add(ticket);
        }
        Map<String, Object> response = new HashMap<>();
        response.put("status", "success");
        response.put("data", tickets);

        return response;
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
            String statusStr = seatData.containsKey("status") ? (String) seatData.get("status") : "available";
            Double price = seatData.containsKey("price") ? ((Number) seatData.get("price")).doubleValue() : 0.0;

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
    public List<Ticket> cancelTickets(List<Long> ticketList){
        List<Ticket> res = new ArrayList<>();
        for (Long id : ticketList) {
            Ticket ticket = ticketRepository.findByTicketId(id);
            ticket.setPaymentIntentId(null);
            ticket.setUserId(null);
            ticket.setPreference(null);
            ticket.setStatus(TicketStatus.available);
            res.add(ticket);
        }
        return res;
    }
    

    @Transactional
    public Map<String, Object> cancelTicketsByEvent(Long eventId) {
        List<Ticket> tickets = ticketRepository.findByEventId(eventId);
        int canceledCount = 0;
        List<Map<String, Object>> cancellations = new ArrayList<>();

        for (Ticket ticket : tickets) {
            if (ticket.getStatus() == TicketStatus.sold) {
                canceledCount++;
                Map<String, Object> record = new HashMap<>();
                record.put("ticket_id", ticket.getTicketId());
                record.put("event_id", ticket.getEventId());
                record.put("seat_number", ticket.getSeatNumber());
                record.put("user_id", ticket.getUserId());
                record.put("payment_intent_id", ticket.getPaymentIntentId());
                record.put("previous_status", ticket.getStatus().toString());
                record.put("price", ticket.getPrice());
                cancellations.add(record);
            }
            ticket.setStatus(TicketStatus.cancelled);
            ticket.setReservationExpires(null);
            ticket.setReservationId(null);
        }

        ticketRepository.saveAll(tickets);

        Map<String, Object> response = new HashMap<>();
        response.put("status", "success");
        response.put("message", String.format(
                "Successfully canceled %d sold tickets for event ID: %d", canceledCount, eventId));
        response.put("cancellations", cancellations);

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

    public Map<String, Object> getTicketsForUser(Long userId) {
        // Retrieve tickets where the current owner or previous owner equals the
        // provided userId
        List<Ticket> tickets = ticketRepository.findByUserIdOrPreviousOwnerUserId(userId, userId);

        List<Map<String, Object>> ticketList = tickets.stream().map(ticket -> {
            Map<String, Object> ticketMap = new HashMap<>();
            ticketMap.put("ticketId", ticket.getTicketId());
            ticketMap.put("eventId", ticket.getEventId());
            ticketMap.put("seatNumber", ticket.getSeatNumber());
            ticketMap.put("status", ticket.getStatus().toString());
            ticketMap.put("category", ticket.getCategory().toString());
            ticketMap.put("price", ticket.getPrice());
            ticketMap.put("currentOwner", ticket.getUserId());
            ticketMap.put("previousOwner", ticket.getPreviousOwnerUserId());
            return ticketMap;
        }).collect(Collectors.toList());

        Map<String, Object> response = new HashMap<>();
        response.put("status", "success");
        response.put("data", ticketList);
        return response;
    }

    @Transactional
    public Map<String, Object> transferTicket(Long ticketId, Long currentUserId, Long newUserId,
            String paymentIntentId) {
        Map<String, Object> response = new HashMap<>();

        Optional<Ticket> ticketOpt = ticketRepository.findById(ticketId);
        if (ticketOpt.isEmpty()) {
            response.put("status", "error");
            response.put("message", "Ticket not found");
            return response;
        }

        Ticket ticket = ticketOpt.get();

        // (Optional) Validate current ownership if needed:
        // if (ticket.getUserId() == null || !ticket.getUserId().equals(currentUserId))
        // {
        // response.put("status", "error");
        // response.put("message", "Invalid ticket ownership");
        // return response;
        // }

        // (Optional) Validate ticket is in sold status if needed:
        // if (ticket.getStatus() != TicketStatus.sold) {
        // response.put("status", "error");
        // response.put("message", "Only purchased tickets can be transferred");
        // return response;
        // }

        // Set previous owner before transfer
        ticket.setPreviousOwnerUserId(ticket.getUserId());

        // Perform transfer: update owner and status, and set payment intent
        ticket.setUserId(newUserId);
        ticket.setPaymentIntentId(paymentIntentId);
        ticket.setStatus(TicketStatus.sold); // Ensuring the status remains sold after transfer

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
        Map<String, Object> ticketData = new HashMap<>();
        ticketData.put("ticketId", ticket.getTicketId());
        ticketData.put("eventId", ticket.getEventId());
        ticketData.put("seatNumber", ticket.getSeatNumber());
        ticketData.put("status", ticket.getStatus().toString());
        ticketData.put("category", ticket.getCategory().toString());
        ticketData.put("reservationExpires", ticket.getReservationExpires());
        ticketData.put("reservationId", ticket.getReservationId());
        ticketData.put("userId", ticket.getUserId());
        ticketData.put("price", ticket.getPrice());
        ticketData.put("paymentIntentId", ticket.getPaymentIntentId());
        ticketData.put("preference", ticket.getPreference());
        ticketData.put("previousOwnerUserId", ticket.getPreviousOwnerUserId());
    
        // Wrap in `data` array and add a success status
        response.put("data", List.of(ticketData));
        response.put("status", "success");
        
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