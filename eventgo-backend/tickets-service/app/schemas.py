from pydantic import BaseModel
from datetime import datetime
from .models import TicketStatus

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