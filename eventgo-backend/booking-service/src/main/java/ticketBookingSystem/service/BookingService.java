package ticketBookingSystem.service;

import ticketBookingSystem.dto.Booking.ProcessBookingRequestDTO;
import ticketBookingSystem.dto.Booking.ProcessBookingResponseDTO;

public interface BookingService {
    ProcessBookingResponseDTO processBooking(ProcessBookingRequestDTO request);
}
