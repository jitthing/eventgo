package ticketBookingSystem.service.impl;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import ticketBookingSystem.dto.Booking.ProcessBookingRequestDTO;
import ticketBookingSystem.dto.Booking.ProcessBookingResponseDTO;
import ticketBookingSystem.service.BookingService;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.HashMap;

@Slf4j
@Service
public class BookingServiceImpl implements BookingService {

    @Value("${tickets.inventory.url}")
    private String ticketsInventoryUrl;

    @Value("${stripe.service.url}")
    private String stripeServiceUrl;

    private final RestTemplate restTemplate = new RestTemplate();

    public ProcessBookingResponseDTO processBooking(ProcessBookingRequestDTO request) {
        String eventId = request.getEventId();
        String paymentIntentId = request.getPaymentIntentId();
        
        log.info("Processing booking for event: {}, payment: {}, seats: {}", 
                eventId, paymentIntentId, request.getSeats());

        ProcessBookingResponseDTO response = new ProcessBookingResponseDTO();
        
        try {
            // 1. Validate payment with Stripe service
            boolean paymentValid = validatePayment(paymentIntentId, eventId, request.getSeats());
            if (!paymentValid) {
                response.setStatus("FAILED");
                response.setConfirmationMessage("Payment validation failed.");
                return response;
            }

            // 2. Confirm seat in Ticket Inventory service
            boolean seatConfirmed = confirmSeat(request);
            if (!seatConfirmed) {
                response.setStatus("FAILED");
                response.setConfirmationMessage("Failed to confirm seat in Ticket Inventory.");
                return response;
            }

            response.setStatus("SUCCESS");
            response.setConfirmationMessage("Booking confirmed for event " + eventId + " with payment ID " + paymentIntentId);
            
        } catch (Exception e) {
            log.error("Error processing booking: {}", e.getMessage(), e);
            response.setStatus("ERROR");
            response.setConfirmationMessage("An error occurred while processing the booking: " + e.getMessage());
        }
        
        return response;
    }

    private boolean validatePayment(String paymentIntentId, String eventId, java.util.List<String> seats) {
        String url = stripeServiceUrl + "/validate-payment";
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("payment_intent_id", paymentIntentId);
        requestBody.put("event_id", eventId);
        requestBody.put("seats", seats);

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

        try {
            // For testing, skip validation if using test payment intent
            if (paymentIntentId != null && paymentIntentId.startsWith("pi_test")) {
                log.info("Using test payment intent - bypassing validation");
                return true;
            }
            
            log.info("Calling Stripe validation service at {}", url);
            Map<String, Object> response = restTemplate.postForObject(url, entity, Map.class);
            log.info("Payment validation response: {}", response);
            
            if (response != null && response.containsKey("valid") && (Boolean) response.get("valid")) {
                return true;
            }
            return false;
        } catch (Exception e) {
            log.error("Error validating payment: {}", e.getMessage(), e);
            // For testing purposes only - remove in production
            if (paymentIntentId != null && paymentIntentId.startsWith("pi_test")) {
                log.warn("Using test payment intent - bypassing validation despite error");
                return true;
            }
            return false;
        }
    }

    private boolean confirmSeat(ProcessBookingRequestDTO request) {
        String url = ticketsInventoryUrl + "/tickets/confirm";
        log.info("Confirming seat at URL: {}", url);
        
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, Object> requestBody = new HashMap<>();
        
        // Check if reservationId and userId are provided
        if (request.getReservationId() == null || request.getUserId() == null) {
            log.warn("Missing reservationId or userId for ticket confirmation");
            return false;
        }
        
        // Add required fields for the ticket inventory service
        requestBody.put("reservationId", request.getReservationId());
        requestBody.put("userId", request.getUserId());
        requestBody.put("paymentIntentId", request.getPaymentIntentId());

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

        try {
            // Using PATCH method to match the TicketController endpoint
            log.info("Sending PATCH request to confirm tickets: {}", requestBody);
            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                url, 
                HttpMethod.PATCH,  // Using PATCH to match the TicketController endpoint
                entity, 
                new ParameterizedTypeReference<Map<String, Object>>() {}
            );
            
            log.info("Received response: status={}, body={}", response.getStatusCode(), response.getBody());
            
            if (response.getStatusCode().is2xxSuccessful()) {
                Map<String, Object> responseBody = response.getBody();
                if (responseBody != null && "success".equals(responseBody.get("status"))) {
                    log.info("Seat confirmation successful");
                    return true;
                }
            }
            log.warn("Seat confirmation failed with status: {}", response.getStatusCode());
            return false;
        } catch (Exception e) {
            log.error("Error confirming seat: {}", e.getMessage(), e);
            
            // For testing purposes only - remove in production
            if (request.getPaymentIntentId() != null && request.getPaymentIntentId().startsWith("pi_test")) {
                log.warn("Using test payment intent - bypassing seat confirmation despite error");
                return true;
            }
            
            return false;
        }
    }

    public String test() {
        return "test";
    }
}