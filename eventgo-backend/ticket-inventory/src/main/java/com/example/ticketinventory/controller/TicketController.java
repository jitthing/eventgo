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
// import com.example.ticketinventory.dto.*;
import com.example.ticketinventory.dto.CancelTicket.*;
import com.example.ticketinventory.dto.ReserveTickets.*;
import com.example.ticketinventory.dto.ReleaseTickets.*;
import com.example.ticketinventory.dto.CreateTickets.*;
import com.example.ticketinventory.dto.GetTicketsByEventID.*;
import com.example.ticketinventory.dto.CancelTickets.*;
import com.example.ticketinventory.dto.ConfirmTicketSplitRequest.*;
import com.example.ticketinventory.dto.TransferTickets.*;
import com.example.ticketinventory.dto.UpdatePreference.*;
import com.example.ticketinventory.dto.MarkTransferring.*;
import com.example.ticketinventory.dto.ConfirmTickets.*;
import com.example.ticketinventory.dto.GetTicketsByUserID.*;
import com.example.ticketinventory.dto.TicketAssociatedUser.*;
import com.example.ticketinventory.dto.TicketListRequest.TicketListRequest;
import com.example.ticketinventory.dto.GetTicketByTicketID.*;
import com.example.ticketinventory.dto.TicketListRequest.*;
import org.springframework.http.HttpStatus;
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

    // -------------------------------
    // GET TICKETS FOR ASSOCIATED USER
    // -------------------------------
    @Operation(summary = "Get all tickets associated with a user", description = "Retrieves all tickets where the given user is either the current owner or the previous owner.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Successfully retrieved tickets", content = @Content(schema = @Schema(implementation = TicketAssociatedUserResponse.class))),
            // @ApiResponse(responseCode = "404", description = "No tickets found for the user")
    })
    @GetMapping("/user/associated/{user_id}")
    public ResponseEntity<Map<String, Object>> getTicketsForUser(
            @Parameter(description = "User ID to retrieve associated tickets", example = "2001") @PathVariable("user_id") Long userId) {
        Map<String, Object> response = ticketService.getTicketsForUser(userId);
        return ResponseEntity.ok(response);
    }

    // -------------------------------
    // GET TICKETS BY EVENT ID (DONE)
    // -------------------------------
    @Operation(summary = "Get tickets by event", description = "Retrieves all tickets and their current status for a specific event. "
            +
            "Includes availability, pricing, and category information.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Successfully retrieved tickets", content = @Content(mediaType = "application/json", schema = @Schema(implementation = TicketResponse.class))),
            // @ApiResponse(responseCode = "404", description = "Event not found", content = @Content(mediaType = "application/json", schema = @Schema(implementation = TicketResponseError.class)))
    })
        @GetMapping("/{event_id}")
        public ResponseEntity<?> getTicketsByEvent(
                @Parameter(description = "ID of the event to get tickets for", example = "1001")
                @PathVariable Long event_id) {

        Map<String, Object> ticketData = ticketService.getTicketsByEvent(event_id);

        if (ticketData == null || ticketData.isEmpty()) {
                TicketResponseError error = new TicketResponseError();
                error.setData(List.of());
                error.setStatus("error");
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
        }

        return ResponseEntity.ok(ticketData);
        }

    // -------------------------------
    // RESERVE SEATS (DONE)
    // -------------------------------
    @Operation(summary = "Reserve seats", description = "Reserve multiple seats for an event. Seats will be held for 10 minutes. "
            +
            "All seats must be available for the reservation to succeed.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Seats successfully reserved", content = @Content(schema = @Schema(implementation = ReserveTicketResponse.class))),
            // @ApiResponse(responseCode = "400", description = "Invalid request or seats unavailable", content = @Content(schema = @Schema(implementation = ReserveTicketResponseError.class)))
    })
    @PostMapping("/reserve")
    public ResponseEntity<Map<String, Object>> reserveSeat(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Reservation details", required = true, content = @Content(schema = @Schema(implementation = ReserveTicketRequest.class))) @RequestBody ReserveTicketRequest request) {
        Map<String, Object> response = ticketService.reserveSeat(
                request.getEventId(),
                request.getSeats(),
                request.getUserId());
        return ResponseEntity.ok(response);
    }


    // -------------------------------
    // CONFIRM TICKET PURCHASE
    // -------------------------------
    @Operation(summary = "Confirm ticket purchase", description = "Confirms a ticket reservation after successful payment. Changes ticket status from reserved to sold.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Purchase successfully confirmed", content = @Content(schema = @Schema(implementation = ConfirmTicketResponse.class))      ),
            @ApiResponse(responseCode = "400", description = "Invalid reservation or tickets no longer reserved", content = @Content(schema = @Schema(implementation = ConfirmTicketResponseError.class))),
        //     @ApiResponse(responseCode = "404", description = "Reservation not found", content = @Content(schema = @Schema(implementation = ConfirmTicketResponseError.class)))
    })
    @PatchMapping("/confirm")
    public ResponseEntity<ConfirmTicketResponse> confirmSeat(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                description = "Confirmation details", required = true,
                content = @Content(schema = @Schema(implementation = ConfirmTicketRequest.class))
            ) @RequestBody ConfirmTicketRequest request) {
    
        String message = ticketService.confirmSeat(
                request.getReservationId(),
                request.getUserId(),
                request.getPaymentIntentId());
    
        if (message.contains("Invalid reservation ID") || message.contains("no reserved tickets")) {
            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body(new ConfirmTicketResponse("error", message));
        }
    
        return ResponseEntity.ok(new ConfirmTicketResponse("success", message));
    }
    

    // -------------------------------
    // CONFIRM TICKET PURCHASE FOR SPLIT BOOKING
    // -------------------------------
    @Operation(summary = "Confirm ticket purchase for split booking", description = "Confirms a specific ticket reservation after successful payment. Changes only the specified ticket status from reserved to sold.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Purchase successfully confirmed", content = @Content(schema = @Schema(implementation = ConfirmTicketSplitRequestResponse.class))),
            // @ApiResponse(responseCode = "400", description = "Invalid reservation or ticket no longer reserved"),
            // @ApiResponse(responseCode = "404", description = "Reservation or ticket not found")
    })
    @PatchMapping("/confirm-split")
    public ResponseEntity<Map<String, String>> confirmSeatSplitBooking(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Confirmation details including specific ticket", required = true, content = @Content(schema = @Schema(implementation = ConfirmTicketSplitRequest.class))) @RequestBody ConfirmTicketSplitRequest request) {
        return ResponseEntity.ok(Map.of(
                "status", "success",
                "message", ticketService.confirmSeatSplitBooking(
                        request.getReservationId(),
                        request.getUserId(),
                        request.getPaymentIntentId(),
                        request.getTicketId())));
    }

    // -------------------------------
    // UPDATE SPLIT PREFERENCE
    // -------------------------------
    @Operation(summary = "Update ticket preference for split booking", description = "Updates on user preference whether they wish to keep their ticket upon failure of full party payment. Changes only the specified ticket preference to either keep or refund.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Preference updated", content = @Content(schema = @Schema(implementation = UpdatePreferenceResponse.class))),
            // @ApiResponse(responseCode = "400", description = "Invalid ticket no longer reserved"),
            // @ApiResponse(responseCode = "404", description = "Ticket not found")
    })
    @PatchMapping("/update-preference")
    public ResponseEntity<Map<String, String>> updatePreference(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Release details", required = true) @RequestBody PreferenceRequest request) {
        return ResponseEntity.ok(Map.of(
                "status", "success",
                "message", ticketService.updatePreference(
                        request.getTicketId(),
                        request.getPreference()
                )
        ));
    }

    // -------------------------------
    // RELEASE RESERVED SEATS
    // -------------------------------
    @Operation(summary = "Release reserved seats", description = "Release previously reserved seats back to available status")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Seats successfully released", content = @Content(schema = @Schema(implementation = ReleaseTicketResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid reservation ID", content = @Content(schema = @Schema(implementation = ReleaseTicketResponseError.class)))
    })
    @PatchMapping("/release")
    public ResponseEntity<ReleaseTicketResponse> releaseSeat(@RequestBody ReleaseTicketRequest request) {
        ReleaseTicketResponse response = new ReleaseTicketResponse();
        try {
            String result = ticketService.releaseSeat(request.getReservation_id());
            if (result.contains("Invalid reservation ID")) {
                response.setStatus("failure");
                response.setMessage("Invalid reservation ID or no reserved tickets found");
                return ResponseEntity.badRequest().body(response);
            }
            response.setStatus("success");
            response.setMessage("All seats released successfully");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.setStatus("failure");
            response.setMessage(e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    // -------------------------------
    // TRANSFER TICKET OWNERSHIP AFTER VERIFICATION
    // -------------------------------
    @Operation(summary = "Transfer ticket ownership", description = "Transfer a purchased ticket to another user. Only the current ticket owner can initiate the transfer.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Ticket successfully transferred", content = @Content(schema = @Schema(implementation = TransferTicketResponse.class))),
            // @ApiResponse(responseCode = "400", description = "Invalid ownership or ticket not transferable"),
        //     @ApiResponse(responseCode = "404", description = "Ticket not found")
    })
    @PatchMapping("/transfer")
    public ResponseEntity<Map<String, Object>> transferTicket(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Transfer details including current and new owner information", required = true, content = @Content(schema = @Schema(implementation = TransferTicketRequest.class))) @RequestBody Map<String, Object> request) {
        Long ticketId = ((Number) request.get("ticket_id")).longValue();
        Long currentUserId = ((Number) request.get("current_user_id")).longValue();
        Long newUserId = ((Number) request.get("new_user_id")).longValue();
        String paymentIntentId = (String) request.get("payment_intent_id");
        return ResponseEntity.ok(ticketService.transferTicket(ticketId, currentUserId, newUserId, paymentIntentId));
    }

    // -------------------------------
    // MARK TICKET AS TRANSFERRING
    // -------------------------------
    @Operation(summary = "Mark ticket as transferring", description = "Mark a ticket as transferring")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Ticket marked as transferring", content = @Content(schema = @Schema(implementation = MarkTransferringResponse.class))),
    })
    @PatchMapping("/mark-transferring")
    public ResponseEntity<Map<String, Object>> markTicketTransferring(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Ticket identifier", required = true, content = @Content(schema = @Schema(implementation = MarkTransferringRequest.class))) @RequestBody Map<String, Object> request) {

        Long ticketId = ((Number) request.get("ticket_id")).longValue();
        Map<String, Object> response = ticketService.markTicketTransferring(ticketId);
        return ResponseEntity.ok(response);
    }

    // -------------------------------
    // CANCEL EVENT TICKETS
    // -------------------------------
    @Operation(summary = "Cancel event tickets", description = "Cancel all reserved (unpaid) tickets for an event. Returns list of affected payment intents.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Successfully canceled tickets", content = @Content(schema = @Schema(implementation = CancelTicketsResponse.class))),
            // @ApiResponse(responseCode = "404", description = "Event not found", content = @Content(schema = @Schema(implementation = CancelTicketsResponseError.class)))
    })
    @PatchMapping("/{event_id}/cancel")
    public ResponseEntity<Map<String, Object>> cancelTicketsByEvent(
            @Parameter(description = "ID of the event to cancel tickets for") @PathVariable Long event_id) {
        return ResponseEntity.ok(ticketService.cancelTicketsByEvent(event_id));
    }

    // -------------------------------
    // CANCEL SINGULAR TICKET ON REFUND
    // -------------------------------
    @Operation(
            summary = "Cancel singular ticket on refund",
            description = "To cancel ticket(s) after party booking failed for those who choose for refund."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Successfully cancelled tickets", content = @Content(schema = @Schema(implementation = CancelTicketResponse.class))),
            // @ApiResponse(responseCode = "404", description = "Tickets not found")
    })
    @PatchMapping("/cancel-ticket")
    public ResponseEntity<Map<String, Object>> cancelTicket(
            @RequestBody CancelTicketRequest request
    ){
        return ResponseEntity.ok(Map.of(
                "status", "success",
                "data", ticketService.cancelTickets(
                        request.getTicketList()
                )
        ));
    }

    // -------------------------------
    // GET TICKETS BY USER ID
    // -------------------------------
    @Operation(summary = "Get tickets by user ID", description = "Retrieve tickets associated with a specific user")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Successfully retrieved tickets", content = @Content(schema = @Schema(implementation = TicketDetailsResponse.class))),
        // @ApiResponse(responseCode = "404", description = "No tickets found for the user")
    })
    @GetMapping("/user/{user_id}")
    public ResponseEntity<Map<String, Object>> getTicketsByUserId(@PathVariable Long user_id) {
        return ResponseEntity.ok(ticketService.getTicketsByUserId(user_id));
    }

    // -------------------------------
    // CREATE TICKETS (DONE)
    // -------------------------------
    @Operation(summary = "Create tickets", description = "Create tickets for an event")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Tickets created successfully", content = @Content(schema = @Schema(implementation = CreateTicketsResponse.class))),
        // @ApiResponse(responseCode = "400", description = "Invalid request", content = @Content(schema = @Schema(implementation = CreateTicketsResponseError.class)))
    })
    @PostMapping("/create")
    public ResponseEntity<Map<String, Object>> createSeats(
        @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Event details", required = true, content = @Content(schema = @Schema(implementation = CreateTicketsRequest.class)))
        @RequestBody Map<String, Object> request) {

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

    // -------------------------------
    // GET TICKETS BY TICKET ID
    // -------------------------------
    @GetMapping("/id/{ticket_id}")
    @Operation(
        summary = "Get ticket details",
        description = "Retrieve detailed information about a specific ticket, including payment data"
    )
    @ApiResponses({ 
        @ApiResponse(responseCode = "200", description = "Ticket found", content = @Content(schema = @Schema(implementation = GetTicketResponse.class))),
        // @ApiResponse(responseCode = "404", description = "Ticket not found", content = @Content(schema = @Schema(implementation = GetTicketResponseError.class)))
    })
    public ResponseEntity<Map<String, Object>> getTicketsById(
        @Parameter(description = "ID of the ticket to retrieve", example = "137")
        @PathVariable("ticket_id") Long ticketId
    ) {
        return ResponseEntity.ok(ticketService.getTicketById(ticketId));
    }

    // -------------------------------
    // GET TICKETS BY TICKET ID LIST
    // -------------------------------
    @GetMapping("/tickets-by-ids")
    @Operation(
        summary = "Get tickets by ticket IDs",
        description = "Retrieve tickets by reservation ID"
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Ticket found", content = @Content(schema = @Schema(implementation = TicketListResponse.class))),
        // @ApiResponse(responseCode = "404", description = "Ticket not found")
    })
    public ResponseEntity<Map<String, Object>> getTicketsByIdList(
        @RequestBody TicketListRequest ticketListRequest
    ) {
        return ResponseEntity.ok(ticketService.getTicketsById(ticketListRequest.getTicketList()));
    }

}
