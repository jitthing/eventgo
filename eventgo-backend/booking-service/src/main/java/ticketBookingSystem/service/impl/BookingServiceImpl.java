package ticketBookingSystem.service.impl;

import ticketBookingSystem.dto.Booking.BookingDetailsResponseDTO;
import ticketBookingSystem.dto.Booking.CancelBookingResponseDTO;
import ticketBookingSystem.dto.Booking.ProcessBookingRequestDTO;
import ticketBookingSystem.dto.Booking.ProcessBookingResponseDTO;
import ticketBookingSystem.dto.Payment.PaymentRequestDTO;
import ticketBookingSystem.dto.Payment.PaymentResponseDTO;
import ticketBookingSystem.dto.TicketsService.TicketConfirmRequestDTO;
import ticketBookingSystem.dto.TicketsService.TicketConfirmResponseDTO;
import ticketBookingSystem.dto.TicketsService.TicketReserveRequestDTO;
import ticketBookingSystem.dto.TicketsService.TicketReserveResponseDTO;
import ticketBookingSystem.dto.notification.NotificationDTO;
import ticketBookingSystem.service.NotificationProducer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
public class BookingServiceImpl implements ticketBookingSystem.service.BookingService {

    @Value("${TICKETS_SERVICE_URL}")
    private String ticketsServiceUrl;

    @Value("${STRIPE_SERVICE_URL}")
    private String stripeServiceUrl;



    private final RestTemplate restTemplate;

    private final NotificationProducer notificationProducer;

//    private TicketReserveResponseDTO ticketReserveResponseDTO;
//    private PaymentResponseDTO paymentResponseDTO;


    public BookingServiceImpl(NotificationProducer notificationProducer) {
        this.notificationProducer = notificationProducer;
        this.restTemplate = new RestTemplate();
    }

    public ProcessBookingResponseDTO processBooking(ProcessBookingRequestDTO request) {
        // UUID bookingID = UUID.randomUUID();
        // TicketReserveRequestDTO ticketReserveRequestDTO = null;

        try {

            // STEP 0: Auth layer, would be moved into the API layer

            // Retrieve token from securityContext
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if(authentication == null || !authentication.isAuthenticated()) {
                String errorMessage = "User not authenticated.";
                log.error(errorMessage);
                return new ProcessBookingResponseDTO("FAILED", errorMessage);
            }

            // Store token as credentials in auth object
            String token = (String) authentication.getCredentials();
            if(token == null || token.isEmpty()) {
                String errorMessage = "No valid token found.";
                log.error(errorMessage);
                return new ProcessBookingResponseDTO("FAILED", errorMessage);
            }

            // Prepare common headers to propagate token
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Bearer " + token);



            // STEP 1: Validate payment status
            PaymentRequestDTO paymentRequestDTO = new PaymentRequestDTO(
                request.getEventId(),
                request.getSeatsId(),
                request.getPaymentIntentId()
            );
            String paymentStripeServiceUrl = stripeServiceUrl + "/validate-payment";
            PaymentResponseDTO paymentResponseDTO = restTemplate.postForObject(
                    paymentStripeServiceUrl, paymentRequestDTO, PaymentResponseDTO.class
            );
            if (paymentResponseDTO == null) {
                String errorMessage = "No response from payment service. Failed to validate payment.";
                log.error(errorMessage);
                return new ProcessBookingResponseDTO("FAILED",  errorMessage);
            }

            // STEP 2: Confirm ticket (from reserve)
            TicketConfirmRequestDTO ticketConfirmRequestDTO = new TicketConfirmRequestDTO(
                    request.getUserId(),
                    request.getReservationId(),
                    request.getPaymentIntentId()

            );

            String confirmTicketsServiceUrl = ticketsServiceUrl + "/confirm";

            TicketConfirmResponseDTO ticketConfirmResponseDTO = restTemplate.postForObject(
                    confirmTicketsServiceUrl, ticketConfirmRequestDTO, TicketConfirmResponseDTO.class
            );
            if (ticketConfirmResponseDTO == null) {
                String errorMessage = "No response from ticket inventory service. Failed to confirm tickets.";
                log.error(errorMessage);
                return new ProcessBookingResponseDTO("FAILED", errorMessage);
            }

            // All steps succeeded.
            return new ProcessBookingResponseDTO("CONFIRMED", "");

        } catch (Exception e) {
            String errorMessage = e.getMessage();
            log.error(errorMessage, e);
            return new ProcessBookingResponseDTO("FAILED", errorMessage);
        }
    }


    public BookingDetailsResponseDTO getBookingDetails(UUID bookingId){
        return null;
    }

    public CancelBookingResponseDTO cancelBooking(UUID bookingId){
        return null;
    }

    public String test(){
        // Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        // if(authentication == null || !authentication.isAuthenticated()) {
        //     String errorMessage = "User not authenticated.";
        //     log.error(errorMessage);
        // }

        // // Store token as credentials in auth object
        // String token = (String) authentication.getCredentials();
        // if(token == null || token.isEmpty()) {
        //     String errorMessage = "No valid token found.";
        //     log.error(errorMessage);
        // }

        // // Prepare common headers to propagate token
        // HttpHeaders headers = new HttpHeaders();
        // headers.set("Authorization", "Bearer " + token);
        
        // // Your booking logic here...
        System.out.println("Processing booking for: ");

        // Prepare a notification event, e.g., informing the user
        NotificationDTO event = new NotificationDTO();
        // event.setRecipient(booking.getUserEmail());
        event.setMessage("Your booking is confirmed!");
        event.setRecipientEmailAddress("taneeherng@gmail.com");
        event.setSubject("Test");
        // event.setRecipientPhoneNumber("+6596327542");

        // Send the notification to the RabbitMQ queue
        notificationProducer.sendNotification(event);

        return "WORKING?";
        
    }

    
}