package com.example.ticketinventory.controller;

import com.example.ticketinventory.service.TicketService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import com.example.ticketinventory.dto.*;
import java.util.Map;
import java.util.List;
import java.util.HashMap;
import java.util.ArrayList;

@RestController
@RequestMapping("/tickets")
@Tag(name = "Ticket Management", description = "APIs for managing event tickets and reservations")
public class TicketController {

    @Autowired
    private TicketService ticketService;

    // GET TICKETS BY EVENT
    @Operation(
        summary = "Get tickets by event",
        description = "Retrieves all tickets and their current status for a specific event. " +
                     "Includes availability, pricing, and category information."
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "Successfully retrieved tickets",
            content = @Content(mediaType = "application/json", 
                schema = @Schema(implementation = TicketResponse.class))
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Event not found"
        )
    })
    @GetMapping("/{event_id}")
    public ResponseEntity<Map<String, Object>> getTicketsByEvent(
        @Parameter(description = "ID of the event to get tickets for", example = "1001")
        @PathVariable Long event_id
    ) {
        return ResponseEntity.ok(ticketService.getTicketsByEvent(event_id));
    }

    @Operation(
        summary = "Reserve seats",
        description = "Reserve multiple seats for an event. Seats will be held for 10 minutes. " +
                     "All seats must be available for the reservation to succeed."
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "Seats successfully reserved",
            content = @Content(schema = @Schema(implementation = ReserveTicketResponse.class))
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Invalid request or seats unavailable"
        )
    })

    // RESERVE SEATS
    @PostMapping("/reserve")
    public ResponseEntity<Map<String, Object>> reserveSeat(
        @io.swagger.v3.oas.annotations.parameters.RequestBody(
            description = "Reservation details",
            required = true,
            content = @Content(schema = @Schema(implementation = ReserveTicketRequest.class))
        )
        @RequestBody ReserveTicketRequest request
    ) {
        Map<String, Object> response = ticketService.reserveSeat(
            request.getEventId(), 
            request.getSeats(), 
            request.getUserId()
        );
        return ResponseEntity.ok(response);
    }

    // CONFIRM TICKET PURCHASE
    @Operation(
        summary = "Confirm ticket purchase",
        description = "Confirms a ticket reservation after successful payment. Changes ticket status from reserved to sold."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Purchase successfully confirmed"),
        @ApiResponse(responseCode = "400", description = "Invalid reservation or tickets no longer reserved"),
        @ApiResponse(responseCode = "404", description = "Reservation not found")
    })
    @PatchMapping("/confirm")
    public ResponseEntity<Map<String, String>> confirmSeat(
        @io.swagger.v3.oas.annotations.parameters.RequestBody(
            description = "Confirmation details",
            required = true,
            content = @Content(schema = @Schema(implementation = ConfirmTicketRequest.class))
        )
        @RequestBody ConfirmTicketRequest request
    ) {
        return ResponseEntity.ok(Map.of(
            "status", "success",
            "message", ticketService.confirmSeat(
                request.getReservationId(),
                request.getUserId(),
                request.getPaymentIntentId()
            )
        ));
    }

    // CONFIRM TICKET PURCHASE FOR SPLIT BOOKING
    @Operation(
        summary = "Confirm ticket purchase for split booking",
        description = "Confirms a specific ticket reservation after successful payment. Changes only the specified ticket status from reserved to sold."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Purchase successfully confirmed"),
        @ApiResponse(responseCode = "400", description = "Invalid reservation or ticket no longer reserved"),
        @ApiResponse(responseCode = "404", description = "Reservation or ticket not found")
    })
    @PatchMapping("/confirm-split")
    public ResponseEntity<Map<String, String>> confirmSeatSplitBooking(
        @io.swagger.v3.oas.annotations.parameters.RequestBody(
            description = "Confirmation details including specific ticket",
            required = true,
            content = @Content(schema = @Schema(implementation = ConfirmTicketSplitRequest.class))
        )
        @RequestBody ConfirmTicketSplitRequest request
    ) {
        return ResponseEntity.ok(Map.of(
            "status", "success",
            "message", ticketService.confirmSeatSplitBooking(
                request.getReservationId(),
                request.getUserId(),
                request.getPaymentIntentId(),
                request.getTicketId()
            )
        ));
    }

    // UPDATE SPLIT PREFERENCE
    @Operation(
            summary = "Update ticket preference for split booking",
            description = "Updates on user preference whether they wish to keep their ticket upon failure of full party payment. Changes only the specified ticket preference to either keep or refund."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Preference updated"),
            @ApiResponse(responseCode = "400", description = "Invalid ticket no longer reserved"),
            @ApiResponse(responseCode = "404", description = "Ticket not found")
    })
    @PatchMapping("/update-preference")
    public ResponseEntity<Map<String, String>> updatePreference(
        @io.swagger.v3.oas.annotations.parameters.RequestBody(
                description = "Release details",
                required = true
        )
        @RequestBody PreferenceRequest request
    ){
        return ResponseEntity.ok(Map.of(
                "status", "success",
                "message", ticketService.updatePreference(
                        request.getEventId(),
                        request.getSeatId(),
                        request.getPreference()
                )
        ));
    }


    
    // RELEASE RESERVED SEATS
    @Operation(
        summary = "Release reserved seats",
        description = "Release previously reserved seats back to available status"
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Seats successfully released"),
        @ApiResponse(responseCode = "400", description = "Invalid reservation ID")
    })
    @PatchMapping("/release")
    public ResponseEntity<Map<String, String>> releaseSeat(
        @io.swagger.v3.oas.annotations.parameters.RequestBody(
            description = "Release details",
            required = true
        )
        @RequestBody Map<String, Object> request
    ) {
        Long reservationId = ((Number) request.get("reservation_id")).longValue();
        return ResponseEntity.ok(Map.of(
                "status", "success",
                "message", ticketService.releaseSeat(reservationId)
        ));
    }

    // TRANSFER TICKET OWNERSHIP AFTER VERIFICATION
    @Operation(
        summary = "Transfer ticket ownership",
        description = "Transfer a purchased ticket to another user. Only the current ticket owner can initiate the transfer."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Ticket successfully transferred"),
        @ApiResponse(responseCode = "400", description = "Invalid ownership or ticket not transferable"),
        @ApiResponse(responseCode = "404", description = "Ticket not found")
    })
    @PatchMapping("/transfer")
    public ResponseEntity<Map<String, Object>> transferTicket(
        @io.swagger.v3.oas.annotations.parameters.RequestBody(
            description = "Transfer details including current and new owner information"
        )
        @RequestBody Map<String, Object> request
    ) {
        Long ticketId = ((Number) request.get("ticket_id")).longValue();
        Long currentUserId = ((Number) request.get("current_user_id")).longValue();
        Long newUserId = ((Number) request.get("new_user_id")).longValue();
        String paymentIntentId = (String) request.get("payment_intent_id");
        return ResponseEntity.ok(ticketService.transferTicket(ticketId, currentUserId, newUserId, paymentIntentId));
    }
    
    // CANCEL EVENT TICKETS
    @Operation(
        summary = "Cancel event tickets",
        description = "Cancel all reserved (unpaid) tickets for an event. Returns list of affected payment intents."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Successfully canceled tickets"),
        @ApiResponse(responseCode = "404", description = "Event not found")
    })
    @PatchMapping("/{event_id}/cancel")
    public ResponseEntity<Map<String, Object>> cancelTicketsByEvent(
        @Parameter(description = "ID of the event to cancel tickets for")
        @PathVariable Long event_id
    ) {
        return ResponseEntity.ok(ticketService.cancelTicketsByEvent(event_id));
    }

    // GET TICKETS BY USER ID
    @GetMapping("/user/{user_id}")
    public ResponseEntity<Map<String, Object>> getTicketsByUserId(@PathVariable Long user_id) {
        return ResponseEntity.ok(ticketService.getTicketsByUserId(user_id));
    }

    // CREATE TICKETS
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
        
        // Process each event. Note: Each seat may now optionally include a "user_id"
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
                // Optional: "user_id" can be provided here
            }
            
            Map<String, Object> eventResponse = ticketService.createTickets(eventId, seats);
            allCreatedTickets.addAll((List<Map<String, Object>>) eventResponse.get("data"));
        }
        
        response.put("status", "success");
        response.put("message", "Created " + allCreatedTickets.size() + " tickets");
        response.put("data", allCreatedTickets);
        
        return ResponseEntity.ok(response);
    }

    @GetMapping("/id/{ticket_id}")
    @Operation(
        summary = "Get ticket details",
        description = "Retrieve detailed information about a specific ticket, including payment data"
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Ticket found"),
        @ApiResponse(responseCode = "404", description = "Ticket not found")
    })
    public ResponseEntity<Map<String, Object>> getTicketById(
        @PathVariable Long ticket_id
    ) {
        return ResponseEntity.ok(ticketService.getTicketById(ticket_id));
    }

}
