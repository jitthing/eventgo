package ticketBookingSystem.service;

import ticketBookingSystem.dto.Booking.BookingDetailsResponseDTO;
import ticketBookingSystem.dto.Booking.CancelBookingResponseDTO;
import ticketBookingSystem.dto.Booking.InitiateBookingRequestDTO;
import ticketBookingSystem.dto.Booking.InitiateBookingResponseDTO;
import org.springframework.stereotype.Service;

import java.util.UUID;


public interface BookingService {

    InitiateBookingResponseDTO initiateBooking(InitiateBookingRequestDTO request);

    BookingDetailsResponseDTO getBookingDetails(UUID bookingId);

    CancelBookingResponseDTO cancelBooking(UUID bookingId);

    String test();


}
