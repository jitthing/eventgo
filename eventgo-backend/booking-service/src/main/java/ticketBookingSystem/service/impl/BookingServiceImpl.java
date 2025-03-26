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

    @Value("${events.api.url}")
    private String eventsApiUrl;

    @Value("${auth.service.url}")
    private String authServiceUrl;

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
    
            // 1. Fetch Event Details from the Event Service
            Map<String, Object> eventDetails = webClient.get()
                    .uri(eventsApiUrl + "/events/" + request.getEventId())
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();
            log.info("Fetched event details: {}", eventDetails);

            Map<String, Object> eventData = eventDetails != null ? (Map<String, Object>) eventDetails.get("EventAPI") : null;

            String eventTitle = eventData != null && eventData.get("title") != null 
                    ? eventData.get("title").toString() 
                    : "NBA Finals Game";
            String eventDateRaw = eventData != null && eventData.get("date") != null 
                    ? eventData.get("date").toString() 
                    : "2025-09-20T13:00:00Z";
            String venue = eventData != null && eventData.get("venue") != null 
                    ? eventData.get("venue").toString() 
                    : "Madison Square Garden";

            String formattedDate = "September 20, 2025 (9:00 PM to 12:00 AM)";
            if (eventDateRaw != null) {
                try {
                    java.time.ZonedDateTime zdt = java.time.ZonedDateTime.parse(eventDateRaw);
                    formattedDate = zdt.format(java.time.format.DateTimeFormatter.ofPattern("MMMM dd, yyyy 'at' hh:mm a"));
                } catch (Exception ex) {
                    log.warn("Failed to parse event date: {}", eventDateRaw);
                }
            }

            log.info("Event details processed: title={}, date={}, venue={}", eventTitle, formattedDate, venue);
    
            // 2. Fetch User Details from the Auth Service to get full name (if not already provided)
            Map<String, Object> userDetails = webClient.get()
                    .uri(authServiceUrl + "/users/" + request.getUserId())
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();
            log.info("Fetched user details: {}", userDetails);
    
            String fullName = userDetails != null && userDetails.get("full_name") != null
                    ? userDetails.get("full_name").toString()
                    : request.getUserEmail(); // Fallback to email if full name isn’t available
            log.info("Using full name: {}", fullName);
    
            // 3. Use the total amount provided by the front end
            double amount = request.getTotalAmount();
            log.info("Total amount received: {}", amount);
    
            // 4. Compose the comprehensive confirmation message
            String seats = String.join(", ", request.getSeats());
            String message = String.format(
                "Hello %s,%n%n" +
                "Thank you for booking with EventGo! Your reservation has been confirmed.%n%n" +
                "Booking Details%n" +
                "📅 %s%n" +
                "📍 %s%n" +
                "🎟 Seats: %s%n" +
                "💳 Payment ID: %s%n%n" +
                "Total Amount Paid: $%.2f%n%n" +
                "Your tickets are now ready — no further action is needed. We look forward to seeing you at the event!%n%n" +
                "If you have any questions or need assistance, visit our Help Center at https://help.eventgo.com or reply directly to this email.%n%n" +
                "Sincerely,%n" +
                "EventGo Customer Support",
                fullName, formattedDate, venue, seats, request.getPaymentIntentId(), amount
            );
            log.info("Composed email message: {}", message);
    
            // 5. Prepare and send the notification
            NotificationDTO notification = new NotificationDTO();
            notification.setNotificationId(UUID.randomUUID());
            notification.setTimestamp(new Date());
            notification.setSubject("Booking Confirmation - " + eventTitle);
            notification.setMessage(message);
    
            // Ensure recipient email is valid: if not, try to get it via another lookup
            String recipientEmail = request.getUserEmail();
            if (recipientEmail == null || recipientEmail.isEmpty()) {
                recipientEmail = getUserEmail(request.getUserId());
                log.warn("User email not provided in request, fallback email: {}", recipientEmail);
            } else {
                log.info("Using recipient email from request: {}", recipientEmail);
            }
            notification.setRecipientEmailAddress(recipientEmail);
    
            notificationProducer.sendNotification(notification);
            log.info("Successfully published comprehensive booking confirmation.");
        } catch (Exception e) {
            log.error("Failed to send booking confirmation email. Error: ", e);
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