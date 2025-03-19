import { 
  EventResponse, Event, Ticket, TicketResponse, TicketStatus, ReserveTicketRequest, ReserveTicketResponse, 
  ConfirmTicketRequest, ConfirmTicketResponse, TransferTicketRequest, TransferTicketResponse 
} from "./interfaces";

const EVENTS_API_URL = process.env.NEXT_PUBLIC_EVENTS_API_URL || 'https://personal-vyyhsf3d.outsystemscloud.com/EventsOutsystem/rest/EventsAPI';
const TICKETS_API_URL = process.env.NEXT_PUBLIC_TICKETS_API_URL || 'http://localhost:8005';

/**
 * Fetch an event by event ID.
 */
export async function getEvent(event_id: number): Promise<Event> {
  const response = await fetch(`${EVENTS_API_URL}/events/${event_id}`);
  if (!response.ok) {
    throw new Error('Failed to fetch event');
  }
  const eventResponse: EventResponse = await response.json();
  const event = eventResponse.EventAPI;

  // Ensure image_url is a full URL
  if (event.image_url && !event.image_url.startsWith('http')) {
    event.image_url = `${EVENTS_API_URL}/events/${event.event_id}/image`;
  }
  
  return event;
}

/**
 * Fetch all events.
 */
export async function getAllEvents(): Promise<Event[]> {
  const response = await fetch(`${EVENTS_API_URL}/events`);
  if (!response.ok) {
    throw new Error("Failed to fetch events");
  }
  const events: Event[] = await response.json();

  return events.map((event) => {
    if (event.image_url && !event.image_url.startsWith("http")) {
      event.image_url = `${EVENTS_API_URL}/events/${event.event_id}/image`;
    }
    return event;
  });
}

/**
 * Fetch featured events.
 */
export async function getFeaturedEvents(): Promise<Event[]> {
  const response = await fetch(`${EVENTS_API_URL}/events?featured=true`);
  if (!response.ok) {
    throw new Error("Failed to fetch featured events");
  }

  const data = await response.json();

  // Ensure the API response structure is valid
  if (!data || !Array.isArray(data.EventAPI)) {
    throw new Error("Unexpected API response structure");
  }

  return data.EventAPI.map((event: Event) => ({
    ...event,
    // Ensure image_url is a full URL
    image_url: event.image_url.startsWith("http")
      ? event.image_url
      : `${EVENTS_API_URL}/events/${event.event_id}/image`,
  }));
}

/**
 * Fetch all tickets for a given event.
 */
export async function getTicketsForEvent(event_id: number): Promise<Ticket[]> {
  const response = await fetch(`${TICKETS_API_URL}/tickets/${event_id}`);
  if (!response.ok) {
    throw new Error("Failed to fetch tickets");
  }

  const ticketResponse: TicketResponse = await response.json();

  // Ensure tickets exist inside `data`
  if (!Array.isArray(ticketResponse.data)) {
    throw new Error("Unexpected ticket response structure");
  }

  return ticketResponse.data;
}

/**
 * Reserve seats for an event.
 */
export async function reserveSeats(event_id: number, user_id: number, seats: string[]): Promise<ReserveTicketResponse> {
  const payload: ReserveTicketRequest = { event_id, user_id, seats };
  
  const response = await fetch(`${TICKETS_API_URL}/tickets/reserve`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(payload),
  });
  if (!response.ok) {
    throw new Error('Failed to reserve seats');
  }
  return response.json();
}

/**
 * Confirm ticket purchase after payment.
 */
export async function confirmPurchase(payment_intent_id: string, reservation_id: number, user_id: number): Promise<ConfirmTicketResponse> {
  const payload: ConfirmTicketRequest = { payment_intent_id, reservation_id, user_id };

  const response = await fetch(`${TICKETS_API_URL}/tickets/confirm`, {
    method: 'PATCH',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(payload),
  });
  if (!response.ok) {
    throw new Error('Failed to confirm ticket purchase');
  }
  return response.json();
}

/**
 * Cancel all reserved (unpaid) tickets for an event.
 */
export async function cancelTickets(event_id: number): Promise<{ status: string; message: string }> {
  const response = await fetch(`${TICKETS_API_URL}/tickets/${event_id}/cancel`, {
    method: 'PATCH',
  });
  if (!response.ok) {
    throw new Error('Failed to cancel tickets');
  }
  return response.json();
}

/**
 * Cancel an event.
 */
export async function cancelEvent(event_id: number): Promise<{ status: string; message: string }> {
  const response = await fetch(`${EVENTS_API_URL}/events/${event_id}/cancel`, {
    method: 'PATCH',
  });
  if (!response.ok) {
    throw new Error('Failed to cancel event');
  }
  return response.json();
}

/**
 * Transfer ticket ownership to another user.
 */
export async function transferTicket(current_user_id: number, new_user_id: number, ticket_id: number): Promise<TransferTicketResponse> {
  const payload: TransferTicketRequest = { current_user_id, new_user_id, ticket_id };

  const response = await fetch(`${TICKETS_API_URL}/tickets/transfer`, {
    method: 'PATCH',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(payload),
  });
  if (!response.ok) {
    throw new Error('Failed to transfer ticket');
  }
  return response.json();
}

/**
 * Release previously reserved seats back to available status.
 */
export async function releaseSeats(reservation_id: number): Promise<{ status: string; message: string }> {
  const response = await fetch(`${TICKETS_API_URL}/tickets/release`, {
    method: 'PATCH',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ reservation_id }),
  });
  if (!response.ok) {
    throw new Error('Failed to release seats');
  }
  return response.json();
}

/**
 * Get all tickets for a specific user.
 */
export async function getUserTickets(user_id: number): Promise<Ticket[]> {
  const response = await fetch(`${TICKETS_API_URL}/tickets/user/${user_id}`);
  if (!response.ok) {
    throw new Error('Failed to fetch user tickets');
  }
  return response.json();
}
