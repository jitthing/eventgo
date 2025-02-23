from pydantic import BaseModel
from datetime import datetime
from .models import TicketStatus


# -----------------------------
# Ticket Schemas
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
# Seat Schemas
# -----------------------------
class SeatBase(BaseModel):
    event_id: int
    seat_number: str  # e.g., "A12", "B5"
    category: str  # e.g., "VIP", "Standard"


class SeatCreate(SeatBase):
    pass


class SeatResponse(SeatBase):
    id: int

    # Removed ticket_id because it's not a database column in the Seat model.
    class Config:
        from_attributes = True
