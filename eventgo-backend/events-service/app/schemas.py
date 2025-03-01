from pydantic import BaseModel
from datetime import datetime
from typing import List
from .models import EventStatus
from typing import Optional




class EventBase(BaseModel):
    title: str
    description: str
    date: datetime
    location: str
    category: str
    price: float
    image_url: str
    venue: str
    capacity: int
    is_featured: bool = False
    status: EventStatus = EventStatus.TICKETS_AVAILABLE

class EventCreate(EventBase):
    pass


class SeatResponse(BaseModel):
    id: int
    event_id: int
    seat_number: str
    category: str

    class Config:
        from_attributes = True


class EventResponse(EventBase):
    id: int
    seats: List[SeatResponse]  # Include seat data in event response

    class Config:
        from_attributes = True



class EventUpdate(BaseModel):
    title: Optional[str] = None
    description: Optional[str] = None
    date: Optional[str] = None
    location: Optional[str] = None
    category: Optional[str] = None
    price: Optional[int] = None
    image_url: Optional[str] = None
    venue: Optional[str] = None
    capacity: Optional[int] = None
    is_featured: Optional[bool] = None
    status: Optional[EventStatus] = None

    class Config:
        from_attributes = True  # Ensures compatibility with ORM models
