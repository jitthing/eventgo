const API_BASE_URL = process.env.NEXT_PUBLIC_API_URL || 'http://localhost:8002';
const TICKETS_API_URL = process.env.NEXT_PUBLIC_TICKETS_API_URL || 'http://localhost:8003';

export class APIError extends Error {
  constructor(public status: number, message: string) {
    super(message);
  }
}

async function handleResponse<T>(response: Response): Promise<T> {
  if (!response.ok) {
    throw new APIError(response.status, await response.text());
  }
  return response.json();
}

export interface Event {
  id: number;
  title: string;
  description: string;
  date: string;
  location: string;
  category: string;
  price: number;
  image_url: string;
  venue: string;
  capacity: number;
  is_featured: boolean;
}

export interface Ticket {
  id: number;
  event_id: number;
  price: number;
  status: 'available' | 'reserved' | 'sold';
  created_at: string;
  updated_at: string | null;
}

export const eventsAPI = {
  getFeaturedEvents: async (): Promise<Event[]> => {
    try {
      const response = await fetch(`${API_BASE_URL}/events/featured`, {
        method: 'GET',
        headers: {
          'Content-Type': 'application/json',
        },
      });
      
      if (!response.ok) {
        console.error('Failed to fetch featured events:', await response.text());
        return [];
      }
      
      return handleResponse<Event[]>(response);
    } catch (error) {
      console.error('Error fetching featured events:', error);
      return [];
    }
  },

  getAllEvents: async (): Promise<Event[]> => {
    const response = await fetch(`${API_BASE_URL}/events`);
    return handleResponse<Event[]>(response);
  },

  getEvent: async (id: number): Promise<Event> => {
    const response = await fetch(`${API_BASE_URL}/events/${id}`);
    return handleResponse<Event>(response);
  },
};

export const ticketsAPI = {
  getTicketsForEvent: async (eventId: number): Promise<Ticket[]> => {
    try {
      const response = await fetch(`${TICKETS_API_URL}/tickets?event_id=${eventId}`);
      if (!response.ok) {
        console.error('Failed to fetch tickets:', await response.text());
        return [];
      }
      return handleResponse<Ticket[]>(response);
    } catch (error) {
      console.error('Error fetching tickets:', error);
      return [];
    }
  },

  purchaseTicket: async (ticketId: number): Promise<{ message: string }> => {
    try {
      const response = await fetch(`${TICKETS_API_URL}/tickets/${ticketId}/purchase`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
        },
      });
      return handleResponse<{ message: string }>(response);
    } catch (error) {
      console.error('Error purchasing ticket:', error);
      throw new Error('Failed to purchase ticket');
    }
  },

  createTicket: async (ticket: Omit<Ticket, 'id' | 'created_at' | 'updated_at'>): Promise<Ticket> => {
    const response = await fetch(`${TICKETS_API_URL}/tickets`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify(ticket),
    });
    return handleResponse<Ticket>(response);
  },
}; 