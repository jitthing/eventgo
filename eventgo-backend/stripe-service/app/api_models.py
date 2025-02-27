from pydantic import BaseModel
from typing import List, Optional

class PaymentIntent(BaseModel):
    amount: int
    currency: str = "sgd"
    event_id: str
    seats: List[str]

class PaymentStatusRequest(BaseModel):
    payment_intent_id: str

class PaymentValidationRequest(BaseModel):
    payment_intent_id: str
    event_id: str
    seats: List[str]

class RefundRequest(BaseModel):
    payment_intent_id: str
    amount: Optional[int] = None  # If None, full refund
    reason: Optional[str] = None
