import { Event, Seat, TicketStatus } from "./interfaces";

const EVENTS_API_URL = process.env.NEXT_PUBLIC_EVENTS_API_URL || 'http://localhost:8002';
const TICKETS_API_URL = process.env.NEXT_PUBLIC_TICKETS_API_URL || 'http://localhost:8003';

export async function getEvent(id: number): Promise<Event> {
  const response = await fetch(`${EVENTS_API_URL}/events/${id}`);
  if (!response.ok) {
    throw new Error('Failed to fetch event');
  }
  const event: Event = await response.json();
  
  // Ensure image_url is a full URL
  if (event.image_url && !event.image_url.startsWith('http')) {
    event.image_url = `${EVENTS_API_URL}/events/${event.id}/image`;
  }
  
  return event;
}

export async function getTicketsForEvent(eventId: number) {
  const response = await fetch(`${TICKETS_API_URL}/events/${eventId}/tickets`);
  if (!response.ok) {
    throw new Error('Failed to fetch tickets');
  }
  return response.json();
}

export async function getFeaturedEvents(): Promise<Event[]> {
  const response = await fetch(`${EVENTS_API_URL}/events?featured=true`);
  if (!response.ok) {
    throw new Error('Failed to fetch featured events');
  }
  const events: Event[] = await response.json();
  
  // Transform relative image URLs to full URLs
  const transformedEvents = events.map((event) => {
    if (event.image_url && !event.image_url.startsWith('http')) {
      event.image_url = `${EVENTS_API_URL}/events/${event.id}/image`;
    }
    return event;
  });
  
  return transformedEvents;
}


export async function getAvailableSeats(eventId: number): Promise<Seat[]> {
  // 1) Call the TICKETS-SERVICE endpoint that already merges seat + status
  const response = await fetch(`${TICKETS_API_URL}/events/${eventId}/seats`);
  if (!response.ok) {
    throw new Error(`Failed to fetch seats for event ${eventId}`);
  }

  // 2) Parse JSON to an array of seats
  const seats: Seat[] = await response.json();

  // Each seat has "id", "seat_number", "category", and "status"
  // For safety, ensure status is one of our known TicketStatus values:
  const normalized = seats.map((seat) => {
    if (!seat.status) {
      seat.status = TicketStatus.AVAILABLE; 
    }
    return seat;
  });

  return normalized;
}