package ticketBookingSystem.service;

import ticketBookingSystem.dto.Booking.BookingDetailsResponseDTO;
import ticketBookingSystem.dto.Booking.CancelBookingResponseDTO;
import ticketBookingSystem.dto.Booking.ProcessBookingRequestDTO;
import ticketBookingSystem.dto.Booking.ProcessBookingResponseDTO;

import org.springframework.stereotype.Service;

import java.util.UUID;


public interface BookingService {

    ProcessBookingResponseDTO processBooking(ProcessBookingRequestDTO request);

    BookingDetailsResponseDTO getBookingDetails(UUID bookingId);

    CancelBookingResponseDTO cancelBooking(UUID bookingId);

    String test();


}
