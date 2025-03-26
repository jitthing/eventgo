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

# Split payment schemas
class SplitPaymentParticipant(BaseModel):
    email: str
    user_id: int
    ticket_id: int
    amount: int

class CreateSplitPaymentRequest(BaseModel):
    event_id: int
    currency: str = "usd"
    reservation_id: int
    participants: list[SplitPaymentParticipant]
    description: str
    redirect_url: str

class PaymentLinkResponse(BaseModel):
    payment_link_id: str
    url: str
    amount: int
    expires_at: int

# For split payments, use this expanded response schema
class SplitPaymentLinkResponse(BaseModel):
    payment_link_id: str
    url: str
    participant_email: str  # This field is needed for split payments
    amount: int
    expires_at: int

class SplitPaymentResponse(BaseModel):
    split_payment_id: str
    payment_links: list[SplitPaymentLinkResponse]  # Use the specific schema for split payment links
    total_amount: int
    event_id: int

# class SplitPaymentStatusRequest(BaseModel):
#     split_payment_id: str

class PaymentLinkStatus(BaseModel):
    payment_link_id: str
    participant_email: str
    status: str  # 'unpaid', 'paid', 'expired'
    amount: int

# class SplitPaymentStatusResponse(BaseModel):
#     split_payment_id: str
#     event_id: str
#     seats: list[str]
#     total_amount: int
#     status: str  # 'pending', 'completed', 'expired', 'partially_paid'
#     payment_links: list[PaymentLinkStatus]
#     amount_paid: int
#     amount_pending: int

class CreatePaymentLinkRequest(BaseModel):
    amount: int
    currency: str = "sgd"
    description: str
    email: str
    redirect_url: str
    event_id: str = None
    seats: List[str] = None
    metadata: object