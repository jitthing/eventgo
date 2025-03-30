// ----- AUTH SERVICE TYPES -----
export interface User {
  id: number;
  email: string;
  is_active: boolean;
}

export interface Token {
  access_token: string;
  token_type: string;
}

export interface TokenData {
  email?: string;
}

// ----- EVENTS SERVICE TYPES -----
export enum EventStatus {
  UPCOMING = "upcoming",
  ONGOING = "ongoing",
  COMPLETED = "completed",
  CANCELLED = "cancelled",
}

export interface Event {
  event_id: number;
  title: string;
  description: string;
  date: string; // ISO 8601 (e.g., "2014-12-31T23:59:59.938Z")
  category: string;
  image_url: string;
  venue: string;
  is_featured: boolean;
  status: EventStatus | string; // Enum used when possible, fallback to string
}

// API response wrapper for events
export interface EventResponse {
  EventAPI: Event;
  Result: {
    Success: boolean;
    ErrorMessage: string;
    event_id: number;
  };
}

// ----- TICKET SERVICE TYPES -----
export enum TicketCategory {
  STANDARD = "standard",
  VIP = "VIP",
}

export enum TicketStatus {
  AVAILABLE = "available",
  RESERVED = "reserved",
  SOLD = "sold",
  CANCELLED = "cancelled",
}

export interface Ticket {
  ticket_id: number;
  eventId: number;
  seat_number: string;
  category: TicketCategory;
  price: number;
  status: TicketStatus;
  reservation_id?: number | null;
  user_id?: number | null;
  reservation_expires?: string | null; // ISO 8601 timestamp
  payment_intent_id?: string | null;
}

// API response for fetching tickets of an event
export interface TicketResponse {
  data: Ticket[];  // This correctly maps to the API response structure
  status: string;
}


// API response for reserving a ticket
export interface ReserveTicketRequest {
  event_id: number;
  user_id: number;
  seats: string[]; // List of seat numbers
}

export interface ReserveTicketResponse {
  status: string;
  reservation_id: number;
  expires_at: string; // ISO 8601 timestamp
  seats: string[];
}

// API response for confirming a ticket purchase
export interface ConfirmTicketRequest {
  reservation_id: number;
  user_id: number;
  payment_intent_id: string;
}

export interface ConfirmTicketResponse {
  status: string;
  message: string;
}

// API response for ticket transfer
export interface TransferTicketRequest {
  ticket_id: number;
  current_user_id: number;
  new_user_id: number;
}

export interface TransferTicketResponse {
  status: string;
  message: string;
  ticket_id: number;
  new_user_id: number;
}


// New schema to avoid breaking old code.
export interface AssociatedTicket extends Ticket {
  previousOwner?: number | null;
}

export interface AssociatedTicketResponse {
  status: string;
  data: AssociatedTicket[];
}
