from pydantic import BaseModel
from datetime import datetime
from typing import List


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


class EventCreate(EventBase):
    pass


class SeatResponse(BaseModel):
    id: int
    seat_number: str
    category: str

    class Config:
        from_attributes = True


class EventResponse(EventBase):
    id: int
    seats: List[SeatResponse]  # Include seat data in event response

    class Config:
        from_attributes = True
