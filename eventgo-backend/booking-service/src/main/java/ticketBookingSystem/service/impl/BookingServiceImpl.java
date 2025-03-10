package ticketBookingSystem.service.impl;

import ticketBookingSystem.dto.Booking.BookingDetailsResponseDTO;
import ticketBookingSystem.dto.Booking.CancelBookingResponseDTO;
import ticketBookingSystem.dto.Booking.InitiateBookingRequestDTO;
import ticketBookingSystem.dto.Booking.InitiateBookingResponseDTO;
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

    public InitiateBookingResponseDTO initiateBooking(InitiateBookingRequestDTO request) {
        UUID bookingID = UUID.randomUUID();

        // TicketReserveRequestDTO ticketReserveRequestDTO = null;

        try {

            // Retrieve token from securityContext
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if(authentication == null || !authentication.isAuthenticated()) {
                String errorMessage = "User not authenticated.";
                log.error(errorMessage);
                return new InitiateBookingResponseDTO("FAILED", bookingID.toString(), errorMessage);
            }

            // Store token as credentials in auth object
            String token = (String) authentication.getCredentials();
            if(token == null || token.isEmpty()) {
                String errorMessage = "No valid token found.";
                log.error(errorMessage);
                return new InitiateBookingResponseDTO("FAILED", bookingID.toString(), errorMessage);
            }

            // Prepare common headers to propagate token
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Bearer " + token);



            // STEP 1: Check if ticket is available + Reserve ticket
            // TicketReserveRequestDTO ticketReserveRequestDTO = new TicketReserveRequestDTO(


            // ticketReserveRequestDTO = new TicketReserveRequestDTO(

                    // request.getEventId(),
                    // request.getSeatsId()
                    // ,request.getUserId(),
                    // bookingID.toString()
            // );
            
            String reserveTicketsServiceUrl = ticketsServiceUrl + "/tickets/reserve";

            
            // TicketReserveResponseDTO ticketReserveResponseDTO = restTemplate.postForObject(
            //         reserveTicketsServiceUrl, ticketReserveRequestDTO, TicketReserveResponseDTO.class
            // );


            // Directly send the list of seat IDs, not a wrapped object.
            HttpEntity<List<String>> reserveEntity = new HttpEntity<>(request.getSeatsId(), headers);
            ResponseEntity<TicketReserveResponseDTO> reserveResponse = restTemplate.exchange(
                reserveTicketsServiceUrl, HttpMethod.POST, reserveEntity, TicketReserveResponseDTO.class
            );
            TicketReserveResponseDTO ticketReserveResponseDTO = reserveResponse.getBody();


            if (ticketReserveResponseDTO == null) {
                String errorMessage = "No response from ticket inventory service. Failed to reserve ticket.";
                log.error(errorMessage);
                return new InitiateBookingResponseDTO("FAILED", bookingID.toString(), errorMessage);
            }

            // STEP 2: Initiate payment
            PaymentRequestDTO paymentRequestDTO = new PaymentRequestDTO(
                    ticketReserveResponseDTO.getAmount(),
                    ticketReserveResponseDTO.getCurrency(),
                    ticketReserveResponseDTO.getEventId(),
                    ticketReserveResponseDTO.getSeatsId()
            );
            String paymentStripeServiceUrl = stripeServiceUrl + "/create-payment-intent";
            PaymentResponseDTO paymentResponseDTO = restTemplate.postForObject(
                    paymentStripeServiceUrl, paymentRequestDTO, PaymentResponseDTO.class
            );
            if (paymentResponseDTO == null) {
                String errorMessage = "No response from payment service. Failed to pay.";
                log.error(errorMessage);
                return new InitiateBookingResponseDTO("FAILED", bookingID.toString(), errorMessage);
            }

            // STEP 3: Confirm ticket (from reserve)
            TicketConfirmRequestDTO ticketConfirmRequestDTO = new TicketConfirmRequestDTO(
                    request.getEventId(),
                    request.getSeatsId(),
                    request.getUserId(),
                    bookingID.toString()  // reusing the same booking ID for consistency
            );

            String confirmTicketsServiceUrl = ticketsServiceUrl + "/purchase";

            TicketConfirmResponseDTO ticketConfirmResponseDTO = restTemplate.postForObject(
                    confirmTicketsServiceUrl, ticketConfirmRequestDTO, TicketConfirmResponseDTO.class
            );
            if (ticketConfirmResponseDTO == null) {
                String errorMessage = "No response from ticket inventory service. Failed to confirm tickets.";
                log.error(errorMessage);
                return new InitiateBookingResponseDTO("FAILED", bookingID.toString(), errorMessage);
            }

            // All steps succeeded.
            return new InitiateBookingResponseDTO("CONFIRMED", bookingID.toString(), "");

        } catch (Exception e) {
            String errorMessage = e.getMessage();
            log.error(errorMessage, e);
            return new InitiateBookingResponseDTO("FAILED", bookingID.toString(), errorMessage);
            // return new InitiateBookingResponseDTO("FAILED", bookingID.toString(), "NOPE");

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

        // Send the notification to the RabbitMQ queue
        notificationProducer.sendNotification(event);

        return "WORKING?";
        
    }

    
}