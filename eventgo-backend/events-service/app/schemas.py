from pydantic import BaseModel
from datetime import datetime

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

class EventResponse(EventBase):
    id: int

    class Config:
        from_attributes = True 