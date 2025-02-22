from sqlalchemy import Column, Integer, String, ForeignKey, Enum
from sqlalchemy.orm import relationship
from .database import Base
import enum


class TicketStatus(enum.Enum):
    AVAILABLE = "available"
    RESERVED = "reserved"
    SOLD = "sold"


class Seat(Base):
    __tablename__ = "seats"

    id = Column(Integer, primary_key=True, index=True)
    event_id = Column(Integer, index=True)
    seat_number = Column(String, unique=True, index=True)
    category = Column(String)  # e.g., "VIP", "General", "Balcony"
    ticket_id = Column(
        Integer, ForeignKey("tickets.id"), nullable=True
    )  # Track which ticket is assigned

    ticket = relationship("Ticket", back_populates="seat")


class Ticket(Base):
    __tablename__ = "tickets"

    id = Column(Integer, primary_key=True, index=True)
    event_id = Column(Integer, index=True)
    price = Column(Integer)
    status = Column(Enum(TicketStatus), default=TicketStatus.AVAILABLE)
    seat_id = Column(
        Integer, ForeignKey("seats.id"), unique=True, nullable=True
    )  # Link to Seat

    seat = relationship("Seat", back_populates="ticket")
