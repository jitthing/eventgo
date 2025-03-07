package com.example.ticketinventory.controller;

import com.example.ticketinventory.service.TicketService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.List;

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

    // Release all seats under a reservation ID
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
        Long bookingId = ((Number) request.get("booking_id")).longValue();

        return ResponseEntity.ok(Map.of(
                "status", "success",
                "message", ticketService.confirmSeat(reservationId, bookingId)
        ));
    }
}
