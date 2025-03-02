package ticketBookingSystem.controller;

import ticketBookingSystem.dto.Booking.InitiateBookingRequestDTO;
import ticketBookingSystem.dto.Booking.InitiateBookingResponseDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import ticketBookingSystem.service.BookingService;

@Slf4j
@CrossOrigin(origins = "http://localhost:3000")
@Validated
@RestController
@RequestMapping("/bookings")
public class BookingController {

    private final ticketBookingSystem.service.BookingService bookingService;

    public BookingController(BookingService bookingService) {
        this.bookingService = bookingService;
    }

    @PostMapping("/initiateBooking")
    public ResponseEntity<InitiateBookingResponseDTO> initiateBooking(
            @RequestBody InitiateBookingRequestDTO request
    ) {
        try {
            InitiateBookingResponseDTO initiateBookingResponseDTO = bookingService.initiateBooking(request);
            return ResponseEntity.ok(initiateBookingResponseDTO);
        } catch (Exception e) {
            InitiateBookingResponseDTO errorResponse = new InitiateBookingResponseDTO();
            errorResponse.setStatus("FAILED");
            errorResponse.setErrorMessage("Booking Failed: " + e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }

    @GetMapping("/test")
    public String test(){
        return bookingService.test();
    }




}
