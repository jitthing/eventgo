# tickets-service/app/schemas.py

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
# Existing Seat Schemas
# -----------------------------
class SeatBase(BaseModel):
    event_id: int
    seat_number: str
    category: str  # e.g., "VIP", "Standard"


class SeatCreate(SeatBase):
    pass


class SeatResponse(SeatBase):
    id: int

    class Config:
        from_attributes = True


# -----------------------------
# NEW schema: SeatWithStatus
# -----------------------------
class SeatWithStatus(BaseModel):
    id: int
    event_id: int
    seat_number: str
    category: str
    status: str  # "AVAILABLE", "RESERVED", or "SOLD"

    class Config:
        from_attributes = True
