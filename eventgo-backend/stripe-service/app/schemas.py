from typing import List, Optional, Dict, Any
from pydantic import BaseModel

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

# Response models
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