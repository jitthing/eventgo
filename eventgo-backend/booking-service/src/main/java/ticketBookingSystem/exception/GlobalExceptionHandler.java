package ticketBookingSystem.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import ticketBookingSystem.dto.Booking.ProcessBookingResponseDTO;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(PaymentValidationException.class)
    public ResponseEntity<ProcessBookingResponseDTO> handlePaymentError(PaymentValidationException ex) {
        ProcessBookingResponseDTO response = new ProcessBookingResponseDTO();
        response.setStatus("FAILED");
        response.setErrorMessage(ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @ExceptionHandler(SeatConfirmationException.class)
    public ResponseEntity<ProcessBookingResponseDTO> handleSeatError(SeatConfirmationException ex) {
        ProcessBookingResponseDTO response = new ProcessBookingResponseDTO();
        response.setStatus("FAILED");
        response.setErrorMessage(ex.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT).body(response); // e.g. 409 Conflict
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ProcessBookingResponseDTO> handleGenericError(Exception ex) {
        ProcessBookingResponseDTO response = new ProcessBookingResponseDTO();
        response.setStatus("ERROR");
        response.setErrorMessage("An unexpected error occurred: " + ex.getMessage());
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }
}

