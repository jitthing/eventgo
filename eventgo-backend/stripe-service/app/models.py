from sqlalchemy import Column, Integer, String, DateTime, Boolean, Text, ForeignKey
from sqlalchemy.orm import relationship
from sqlalchemy.sql import func
from .database import Base

class PaymentIntent(Base):
    __tablename__ = "payment_intents"

    id = Column(Integer, primary_key=True, index=True)
    stripe_payment_id = Column(String(100), unique=True, index=True)
    client_secret = Column(String(255))
    amount = Column(Integer)
    currency = Column(String(10), default="sgd")
    status = Column(String(50))
    event_id = Column(String(100))
    seats = Column(Text)  # Comma separated list of seat IDs
    created_at = Column(DateTime(timezone=True), server_default=func.now())
    updated_at = Column(DateTime(timezone=True), onupdate=func.now())
    
    # Relationship with refunds (one-to-many)
    refunds = relationship("Refund", back_populates="payment_intent")

class Refund(Base):
    __tablename__ = "refunds"
    
    id = Column(Integer, primary_key=True, index=True)
    stripe_refund_id = Column(String(100), unique=True, index=True)
    payment_intent_id = Column(Integer, ForeignKey("payment_intents.id"))
    amount = Column(Integer)
    status = Column(String(50))
    reason = Column(String(100), nullable=True)
    created_at = Column(DateTime(timezone=True), server_default=func.now())
    
    # Relationship with payment intent (many-to-one)
    payment_intent = relationship("PaymentIntent", back_populates="refunds")
