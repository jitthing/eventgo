package ticketBookingSystem.controller;


import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;

import ticketBookingSystem.dto.Booking.ProcessBookingRequestDTO;
import ticketBookingSystem.dto.Booking.ProcessBookingResponseDTO;
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

    @PostMapping("/process-booking")
    public ResponseEntity<ProcessBookingResponseDTO> processBooking(
            @RequestBody ProcessBookingRequestDTO request
    ) {
        try {
            ProcessBookingResponseDTO processBookingResponseDTO = bookingService.processBooking(request);
            return ResponseEntity.ok(processBookingResponseDTO);
        } catch (Exception e) {
            ProcessBookingResponseDTO errorResponse = new ProcessBookingResponseDTO();
            errorResponse.setStatus("FAILED");
            errorResponse.setErrorMessage("Booking Failed: " + e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }

    @GetMapping("/test")
    public String test(){
        return bookingService.test();
    }

    @GetMapping("/status")
    public ResponseEntity<String> getStatus() {
        // You can add more logic here as needed.
        log.info("Status endpoint called.");
        return ResponseEntity.ok("Booking Service is running");
    }




}
