package ticketBookingSystem.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import ticketBookingSystem.dto.Booking.ProcessBookingRequestDTO;
import ticketBookingSystem.dto.Booking.ProcessBookingResponseDTO;
import ticketBookingSystem.service.BookingService;

@RestController
@RequestMapping("/bookings")
public class BookingController {
    private final ticketBookingSystem.service.BookingService bookingService;

    public BookingController(BookingService bookingService) {
        this.bookingService = bookingService;
    }

    @Operation(summary="Initiates booking process", description="Validates payment, Confirms seats and Publish message to notification service to be sent when booking successful")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Booking successful"),
        @ApiResponse(responseCode = "400", description = "Payment validation failed"),
        @ApiResponse(responseCode = "409", description = "Seat confirmation failed"),
        @ApiResponse(responseCode = "500", description = "Unexpected server error")
    })
    @PostMapping("/process-booking")
    public ResponseEntity<ProcessBookingResponseDTO> processBooking(
            @RequestBody ProcessBookingRequestDTO request
    ) {
        // try {
            ProcessBookingResponseDTO response = bookingService.processBooking(request);
            switch (response.getStatus()) {
                case "SUCCESS":
                    return ResponseEntity.ok(response);
                case "FAILED":
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response); // 400
                case "ERROR":
                    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response); // 500
                default:
                    return ResponseEntity.status(HttpStatus.CONFLICT).body(response); // fallback or custom mapping
            }
            // return ResponseEntity.ok(processBookingResponseDTO);
        // } catch (Exception e) {
        //     ProcessBookingResponseDTO errorResponse = new ProcessBookingResponseDTO();
        //     errorResponse.setStatus("FAILED");
        //     errorResponse.setErrorMessage("Booking Failed: " + e.getMessage());
        //     return ResponseEntity.badRequest().body(errorResponse);
        // }
    }

    // @GetMapping("/health")
    // public String health() {
    //     return "{\"Status\": \"healthy\"}";
    // }
}
