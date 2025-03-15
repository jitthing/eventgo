import { Event, Seat, TicketStatus } from "./interfaces";

const EVENTS_API_URL = process.env.NEXT_PUBLIC_EVENTS_API_URL || 'http://localhost:8002';
const TICKETS_API_URL = process.env.NEXT_PUBLIC_TICKETS_API_URL || 'http://localhost:8003';

export async function getEvent(event_id: number): Promise<Event> {
  const response = await fetch(`${EVENTS_API_URL}/events/${event_id}`);
  if (!response.ok) {
    throw new Error('Failed to fetch event');
  }
  const event: Event = await response.json();
  
  // Ensure image_url is a full URL
  if (event.image_url && !event.image_url.startsWith('http')) {
    event.image_url = `${EVENTS_API_URL}/events/${event.event_id}/image`;
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
      event.image_url = `${EVENTS_API_URL}/events/${event.event_id}/image`;
    }
    return event;
  });
  
  return transformedEvents;
}

export async function getAllEvents(): Promise<Event[]> {
  const response = await fetch(`${EVENTS_API_URL}/events`); // Fetch all events
  if (!response.ok) {
    throw new Error("Failed to fetch events");
  }
  const events: Event[] = await response.json();

  // Transform relative image URLs to full URLs
  return events.map((event) => {
    if (event.image_url && !event.image_url.startsWith("http")) {
      event.image_url = `${EVENTS_API_URL}/events/${event.event_id}/image`;
    }
    return event;
  });
}

// temp only
function generateMockSeats(count: number): Seat[] {
  const categories = ["VIP", "Regular", "Economy"];
  const statuses = [TicketStatus.AVAILABLE, TicketStatus.RESERVED, TicketStatus.SOLD];

  return Array.from({ length: count }, (_, index) => ({
    id: index + 1,
    seat_number: `${String.fromCharCode(65 + Math.floor(index / 10))}${(index % 10) + 1}`, // Fix: Removed "Row"
    category: categories[Math.floor(Math.random() * categories.length)],
    status: statuses[Math.floor(Math.random() * statuses.length)],
  }));
}


export async function getAvailableSeats(eventId: number): Promise<Seat[]> {
  const mockSeats = generateMockSeats(25); // Generate 10 mock seats

  await new Promise((resolve) => setTimeout(resolve, 500));

  return mockSeats;
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