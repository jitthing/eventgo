from sqlalchemy import Column, Integer, Float, Enum, ForeignKey
from sqlalchemy.orm import relationship
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
    event_id = Column(
        Integer, ForeignKey("events.id", ondelete="CASCADE"), index=True
    )  # ✅ Ensure event_id is valid
    seat_id = Column(
        Integer, ForeignKey("seats.id", ondelete="SET NULL"), index=True
    )  # ✅ Ensure seat_id is valid
    price = Column(Float)
    status = Column(Enum(TicketStatus), default=TicketStatus.AVAILABLE)
    created_at = Column(Integer, server_default=func.now())
    updated_at = Column(Integer, onupdate=func.now())

    seat = relationship("Seat", back_populates="ticket", foreign_keys=[seat_id])


class Seat(Base):
    __tablename__ = "seats"

    id = Column(Integer, primary_key=True, index=True)
    event_id = Column(Integer, ForeignKey("events.id", ondelete="CASCADE"), index=True)
    seat_number = Column(Integer, unique=True, index=True)
    category = Column(Enum("VIP", "Standard", name="seat_category"))

    ticket = relationship(
        "Ticket", back_populates="seat", uselist=False, foreign_keys="[Ticket.seat_id]"
    )
