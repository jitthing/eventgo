from sqlalchemy import Column, Integer, DateTime, Float, Enum
from sqlalchemy.sql import func
import enum
from .database import Base


class TicketStatus(enum.Enum):
    AVAILABLE = "available"
    RESERVED = "reserved"
    SOLD = "sold"


class Ticket(Base):
    __tablename__ = "tickets"

    id = Column(Integer, primary_key=True, index=True)
    event_id = Column(Integer, index=True)
    price = Column(Float)
    status = Column(Enum(TicketStatus), default=TicketStatus.AVAILABLE)
    created_at = Column(DateTime, server_default=func.now())
    updated_at = Column(DateTime, onupdate=func.now())
