// ----- Auth Service Types -----
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

// ----- Events Service Types -----
export interface Seat {
  id: number;
  seat_number: string;
  category: string; // e.g., "VIP", "Standard"
}

export interface Event {
  id: number;
  title: string;
  description: string;
  date: string; // ISO string; convert to Date if needed
  location: string;
  category: string;
  price: number;
  image_url: string;
  venue: string;
  capacity: number;
  is_featured: boolean;
  seats: Seat[]; // 🔹 Added to match backend schema
}

// ----- Tickets Service Types -----
export enum TicketStatus {
  AVAILABLE = "available",
  RESERVED = "reserved",
  SOLD = "sold",
}

export interface Ticket {
  id: number;
  event_id: number;
  seat_id: number;
  price: number;
  status: TicketStatus;
  created_at: string; // ISO string
  updated_at?: string;
}
