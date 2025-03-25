from typing import Optional
from pydantic import BaseModel

class TransferPaymentRequest(BaseModel):
    ticket_id: int
    seller_id: int
    buyer_id: int
    seller_email: str
    buyer_email: str
    description: Optional[str] = None
    redirect_url: str = "http://localhost:3000"

class TransferPaymentResponse(BaseModel):
    transfer_id: str
    payment_link_id: str
    url: str
    amount: int
    expires_at: int

class WebhookResponse(BaseModel):
    status: str
    message: Optional[str] = None

class HealthResponse(BaseModel):
    status: str
    stripe_configured: bool

class TransferNotification(BaseModel):
    subject: str
    message: str
    recipient_email_address: str 