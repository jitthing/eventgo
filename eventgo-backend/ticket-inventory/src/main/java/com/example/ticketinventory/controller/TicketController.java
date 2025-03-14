package com.example.ticketinventory.controller;

import com.example.ticketinventory.service.TicketService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.List;
import java.util.HashMap;
import java.util.ArrayList;

@RestController
@RequestMapping("/inventory")
public class TicketController {

    @Autowired
    private TicketService ticketService;

    @GetMapping("/{event_id}")
    public ResponseEntity<Map<String, Object>> getTicketsByEvent(@PathVariable Long event_id) {
        return ResponseEntity.ok(ticketService.getTicketsByEvent(event_id));
    }

    // Reserve multiple seats under the same reservation ID
    @PostMapping("/reserve")
    public ResponseEntity<Map<String, Object>> reserveSeat(@RequestBody Map<String, Object> request) {
        Long eventId = ((Number) request.get("event_id")).longValue();
        Long userId = ((Number) request.get("user_id")).longValue();
        List<String> seats = (List<String>) request.get("seats");

        Map<String, Object> response = ticketService.reserveSeat(eventId, seats, userId);
        return ResponseEntity.ok(response);
    }

    // Release all seats under a reservation ID - todo: change to patch
    @PostMapping("/release")
    public ResponseEntity<Map<String, String>> releaseSeat(@RequestBody Map<String, Object> request) {
        Long reservationId = ((Number) request.get("reservation_id")).longValue();

        return ResponseEntity.ok(Map.of(
                "status", "success",
                "message", ticketService.releaseSeat(reservationId)
        ));
    }

    // Confirm all seats under a reservation ID and assign a booking ID
    @PostMapping("/confirm")
    public ResponseEntity<Map<String, String>> confirmSeat(@RequestBody Map<String, Object> request) {
        Long reservationId = ((Number) request.get("reservation_id")).longValue();
        Long userId = ((Number) request.get("user_id")).longValue();
        String paymentIntentId = (String) request.get("payment_intent_id");

        return ResponseEntity.ok(Map.of(
                "status", "success",
                "message", ticketService.confirmSeat(reservationId, userId, paymentIntentId)
        ));
    }

    // PATCH OR POST - validate ticket ownership, transfer tickets


    // PATCH - cancel all tickets


    //  GET - ticket id, return status


    // CREATE tickets
    @PostMapping("/create")
    public ResponseEntity<Map<String, Object>> createSeats(@RequestBody Map<String, Object> request) {
        List<Map<String, Object>> events = (List<Map<String, Object>>) request.get("events");
        List<Map<String, Object>> categoryPrices = (List<Map<String, Object>>) request.get("categoryPrices");
        
        // Create a map of category -> price for easier lookup
        Map<String, Double> priceMap = new HashMap<>();
        for (Map<String, Object> categoryPrice : categoryPrices) {
            String category = (String) categoryPrice.get("category");
            Double price = ((Number) categoryPrice.get("price")).doubleValue();
            priceMap.put(category, price);
        }
        
        // Process each event
        Map<String, Object> response = new HashMap<>();
        List<Map<String, Object>> allCreatedTickets = new ArrayList<>();
        
        for (Map<String, Object> eventData : events) {
            Long eventId = ((Number) eventData.get("eventId")).longValue();
            List<Map<String, Object>> seats = (List<Map<String, Object>>) eventData.get("seats");
            
            // Add price information to each seat based on its category
            for (Map<String, Object> seat : seats) {
                String category = (String) seat.get("category");
                if (priceMap.containsKey(category)) {
                    seat.put("price", priceMap.get(category));
                }
            }
            
            Map<String, Object> eventResponse = ticketService.createTickets(eventId, seats);
            allCreatedTickets.addAll((List<Map<String, Object>>) eventResponse.get("data"));
        }
        
        response.put("status", "success");
        response.put("message", "Created " + allCreatedTickets.size() + " tickets");
        response.put("data", allCreatedTickets);
        
        return ResponseEntity.ok(response);
    }

}
