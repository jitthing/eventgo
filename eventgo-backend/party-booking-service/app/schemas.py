from typing import List, Optional, Dict, Any
from pydantic import BaseModel

class WebhookResponse(BaseModel):
    status: str
    message: Optional[str] = None

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
    event_id: str
    seats: list[str]

class HealthResponse(BaseModel):
    status: str
    stripe_configured: bool

class PartyBookingRequestItem(BaseModel):
    user_id: int
    user_email: str
    ticket_id: int
    price: int
class PartyBookingRequest(BaseModel):
    items: List[PartyBookingRequestItem]
    event_id: int
    reservation_id: int

class TransferNotification(BaseModel):
    subject: str
    message: str
    recipient_email_address: str