package ticketBookingSystem.service.impl;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import ticketBookingSystem.dto.Booking.ProcessBookingRequestDTO;
import ticketBookingSystem.dto.Booking.ProcessBookingResponseDTO;
import ticketBookingSystem.dto.notification.NotificationDTO;
import ticketBookingSystem.service.BookingService;
import ticketBookingSystem.service.NotificationProducer;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.HashMap;
import java.util.Date;
import java.util.UUID;

@Slf4j
@Service
public class BookingServiceImpl implements BookingService {

    @Value("${tickets.inventory.url}")
    private String ticketsInventoryUrl;

    @Value("${stripe.service.url}")
    private String stripeServiceUrl;

    private final WebClient webClient;
    private final NotificationProducer notificationProducer;

    public BookingServiceImpl(NotificationProducer notificationProducer) {
        this.webClient = WebClient.builder().build();
        this.notificationProducer = notificationProducer;
    }

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

            // 3. Send confirmation email
            sendBookingConfirmationEmail(request);

            response.setStatus("SUCCESS");
            response.setConfirmationMessage("Booking confirmed for event " + eventId + " with payment ID " + paymentIntentId);
            
        } catch (Exception e) {
            log.error("Error processing booking: {}", e.getMessage(), e);
            response.setStatus("ERROR");
            response.setConfirmationMessage("An error occurred while processing the booking: " + e.getMessage());
        }
        
        return response;
    }

    private void sendBookingConfirmationEmail(ProcessBookingRequestDTO request) {
        try {
            log.info("Starting to prepare email notification for booking: eventId={}, seats={}", 
                    request.getEventId(), request.getSeats());

            NotificationDTO notification = new NotificationDTO();
            notification.setNotificationId(UUID.randomUUID());
            notification.setTimestamp(new Date());
            notification.setSubject("Booking Confirmation - Event " + request.getEventId());
            
            // Create a detailed message
            String message = String.format(
                "Thank you for your booking!\n\n" +
                "Booking Details:\n" +
                "Event ID: %s\n" +
                "Seats: %s\n" +
                "Payment ID: %s\n\n" +
                "Your tickets have been confirmed. Enjoy the event!",
                request.getEventId(),
                String.join(", ", request.getSeats()),
                request.getPaymentIntentId()
            );
            
            notification.setMessage(message);
            
            // For testing, using a hardcoded email
            String userEmail = "taneeherng@gmail.com";
            notification.setRecipientEmailAddress(userEmail);
            
            log.info("Attempting to send notification through RabbitMQ. Email: {}, Subject: {}", 
                    userEmail, notification.getSubject());
            
            notificationProducer.sendNotification(notification);
            log.info("Successfully published notification to RabbitMQ queue");
            
        } catch (Exception e) {
            log.error("Failed to send booking confirmation email. Error details: ", e);
            // Don't throw the exception - we don't want to fail the booking if email fails
        }
    }

    private String getUserEmail(String userId) {
        try {
            // Call auth service to get user details
            Map response = webClient.get()
                .uri("http://auth-service:8001/users/" + userId)
                .retrieve()
                .bodyToMono(Map.class)
                .block();
            
            if (response != null && response.containsKey("email")) {
                return (String) response.get("email");
            }
            throw new RuntimeException("Email not found for user: " + userId);
        } catch (Exception e) {
            log.error("Error fetching user email: {}", e.getMessage());
            throw new RuntimeException("Failed to get user email: " + e.getMessage());
        }
    }

    private boolean validatePayment(String paymentIntentId, String eventId, java.util.List<String> seats) {
        String url = stripeServiceUrl + "/validate-payment";

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("payment_intent_id", paymentIntentId);
        requestBody.put("event_id", eventId);
        requestBody.put("seats", seats);

        try {
            // For testing, skip validation if using test payment intent
            if (paymentIntentId != null && paymentIntentId.startsWith("pi_test")) {
                log.info("Using test payment intent - bypassing validation");
                return true;
            }
            
            log.info("Calling Stripe validation service at {}", url);
            Map response = webClient.post()
                .uri(url)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(requestBody)
                .retrieve()
                .bodyToMono(Map.class)
                .block();
                
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

        try {
            log.info("Sending PATCH request to confirm tickets: {}", requestBody);
            Map response = webClient.patch()
                .uri(url)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(requestBody)
                .retrieve()
                .bodyToMono(Map.class)
                .block();
            
            log.info("Received response: body={}", response);
            
            if (response != null && "success".equals(response.get("status"))) {
                log.info("Seat confirmation successful");
                return true;
            }
            log.warn("Seat confirmation failed");
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