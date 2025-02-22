from pydantic import BaseModel
from datetime import datetime
from .models import TicketStatus


# -----------------------------
# Existing Ticket Schemas
# -----------------------------
class TicketBase(BaseModel):
    event_id: int
    price: float
    status: TicketStatus = TicketStatus.AVAILABLE


class TicketCreate(TicketBase):
    pass


class TicketResponse(TicketBase):
    id: int
    created_at: datetime
    updated_at: datetime | None = None

    class Config:
        from_attributes = True


# -----------------------------
# New Seat Schemas
# -----------------------------
class SeatBase(BaseModel):
    event_id: int
    seat_number: str  # e.g., "A12", "B5"
    category: str  # e.g., "VIP", "Standard"


class SeatCreate(SeatBase):
    pass


class SeatResponse(SeatBase):
    id: int
    ticket_id: int | None = None  # None means the seat is unassigned

    class Config:
        from_attributes = True
