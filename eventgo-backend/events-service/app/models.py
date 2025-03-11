from sqlalchemy import Column, Integer, String, DateTime, Float, Boolean, ForeignKey, Enum
from sqlalchemy.orm import relationship
from .database import Base
import enum

class EventStatus(enum.Enum):
    SCHEDULED = "Scheduled"
    TICKETS_AVAILABLE = "Tickets Available"
    SOLD_OUT = "Sold Out"
    CANCELLED = "Cancelled"
    POSTPONED = "Postponed"

class Event(Base):
    __tablename__ = "events"

    id = Column(Integer, primary_key=True, index=True)
    title = Column(String, index=True)
    description = Column(String)
    date = Column(DateTime)
    location = Column(String)
    category = Column(String)
    price = Column(Float)
    image_url = Column(String)
    venue = Column(String)
    capacity = Column(Integer)
    is_featured = Column(Boolean, default=False)
    status = Column(Enum(EventStatus), default=EventStatus.TICKETS_AVAILABLE)

    #seats = relationship("Seat", back_populates="event", cascade="all, delete-orphan")

""""
class Seat(Base):
    __tablename__ = "seats"

    id = Column(Integer, primary_key=True, index=True)
    event_id = Column(Integer, ForeignKey("events.id"), index=True)
    seat_number = Column(String, unique=True, index=True)
    category = Column(String)  # e.g., "VIP", "Standard", etc.

    event = relationship("Event", back_populates="seats")
"""