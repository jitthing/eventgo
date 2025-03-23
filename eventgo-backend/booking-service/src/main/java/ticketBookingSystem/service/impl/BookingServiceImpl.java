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
        
        log.info("Processing booking for event: {}, payment: {}, seats: {}, userEmail: {}", 
                eventId, paymentIntentId, request.getSeats(), request.getUserEmail());

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
            log.info("Starting to prepare email notification for booking: eventId={}, seats={}, userEmail={}", 
                    request.getEventId(), request.getSeats(), request.getUserEmail());

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
            
            // Get user email from request or use fallback
            String userEmail = request.getUserEmail();
            log.info("User email from request: {}", userEmail);
            
            if (userEmail == null || userEmail.isEmpty()) {
                // Fall back to getting email from auth service if not provided
                log.info("Email from request is null or empty, trying to get from auth service with userId: {}", request.getUserId());
                userEmail = getUserEmail(request.getUserId());
                log.info("Email returned from auth service: {}", userEmail);
                
                // If still null, use default for testing
                if (userEmail == null || userEmail.isEmpty()) {
                    userEmail = "taneeherng@gmail.com"; // Default for testing
                    log.warn("Using default email for testing: {}", userEmail);
                }
            }
            
            notification.setRecipientEmailAddress(userEmail);
            
            log.info("Sending booking confirmation email to: {}", userEmail);
            
            notificationProducer.sendNotification(notification);
            log.info("Successfully published notification to RabbitMQ queue");
            
        } catch (Exception e) {
            log.error("Failed to send booking confirmation email. Error details: ", e);
            // Don't throw the exception - we don't want to fail the booking if email fails
        }
    }

    private String getUserEmail(String userId) {
        try {
            if (userId == null || userId.isEmpty()) {
                log.warn("No userId provided for email lookup");
                return null;
            }
            
            log.info("Fetching user email for userId: {}", userId);
            
            // Call auth service to get user details
            Map response = webClient.get()
                .uri("http://auth-service:8001/users/" + userId)
                .retrieve()
                .bodyToMono(Map.class)
                .block();
            
            if (response != null && response.containsKey("email")) {
                String email = (String) response.get("email");
                log.info("Retrieved email {} for userId {}", email, userId);
                return email;
            }
            
            log.warn("Email not found for user: {}", userId);
            return null;
        } catch (Exception e) {
            log.error("Error fetching user email: {}", e.getMessage(), e);
            return null; // Return null instead of throwing exception
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