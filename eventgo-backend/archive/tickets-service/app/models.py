from sqlalchemy import String, Column, Integer, Float, Enum, DateTime
from sqlalchemy.sql import func
import enum
from app.database import Base


class TicketStatus(enum.Enum):
    AVAILABLE = "available"
    RESERVED = "reserved"
    SOLD = "sold"


class Ticket(Base):
    __tablename__ = "tickets"

    id = Column(Integer, primary_key=True, index=True)
    # event_id remains a plain integer because the events table is managed by the events service.
    event_id = Column(Integer, index=True)
    # Remove the ForeignKey constraint here:
    seat_id = Column(Integer, index=True)

    price = Column(Float)
    status = Column(Enum(TicketStatus), default=TicketStatus.AVAILABLE)
    created_at = Column(DateTime, server_default=func.now())
    updated_at = Column(DateTime, onupdate=func.now())

    # Removed the relationship to Seat
    # seat = relationship("Seat", back_populates="ticket", foreign_keys=[seat_id])


class Seat(Base):
    __tablename__ = "seats"

    id = Column(Integer, primary_key=True, index=True)
    event_id = Column(Integer, index=True)
    seat_number = Column(String, unique=True, index=True)
    category = Column(Enum("VIP", "Standard", name="seat_category"))

    # Removed the relationship to Ticket
    # ticket = relationship("Ticket", back_populates="seat", uselist=False)
