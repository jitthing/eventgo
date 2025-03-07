from typing import List, Optional, Dict, Any
from pydantic import BaseModel
from datetime import datetime

# Request Models
class PaymentIntent(BaseModel):
    amount: int
    currency: str = "sgd"
    event_id: str
    seats: list

class PaymentStatusRequest(BaseModel):
    payment_intent_id: str

class PaymentValidationRequest(BaseModel):
    payment_intent_id: str
    event_id: str
    seats: list

class RefundRequest(BaseModel):
    payment_intent_id: str
    amount: Optional[int] = None  # If None, full refund
    reason: Optional[str] = None

# Response Models
class PaymentIntentResponse(BaseModel):
    clientSecret: str

class PaymentStatusResponse(BaseModel):
    status: str
    amount: int
    currency: str
    metadata: Dict[str, Any]

class WebhookResponse(BaseModel):
    status: str

class PaymentValidationResponse(BaseModel):
    valid: bool
    payment_status: str

class BookingConfirmationResponse(BaseModel):
    status: str
    event_id: str
    seats: list
    payment_intent_id: str

class RefundResponse(BaseModel):
    id: str
    object: str
    amount: int
    currency: str
    payment_intent: str
    status: str

class HealthResponse(BaseModel):
    status: str
    stripe_configured: bool
    database_connected: bool

# Database Models
class PaymentIntentDB(BaseModel):
    stripe_payment_id: str
    client_secret: str
    amount: int
    currency: str
    status: str
    event_id: str
    seats: List[str]

    class Config:
        orm_mode = True

class RefundDB(BaseModel):
    stripe_refund_id: str
    payment_intent_id: str
    amount: int
    status: str
    reason: Optional[str] = None

    class Config:
        orm_mode = True

class PaymentIntentOut(BaseModel):
    id: int
    stripe_payment_id: str
    amount: int
    currency: str
    status: str
    event_id: str
    seats: str
    created_at: datetime
    updated_at: Optional[datetime] = None

    class Config:
        orm_mode = True