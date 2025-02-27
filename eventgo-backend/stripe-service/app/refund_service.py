import stripe
import os
from fastapi import HTTPException
from dotenv import load_dotenv

load_dotenv()
stripe.api_key = os.environ.get("STRIPE_SECRET_KEY")

async def process_refund(payment_intent_id: str, amount: int = None, reason: str = None):
    """Process a refund for a payment intent"""
    try:
        refund_params = {
            "payment_intent": payment_intent_id,
        }
        
        if amount:
            refund_params["amount"] = amount
            
        if reason:
            refund_params["reason"] = reason
            
        refund = stripe.Refund.create(**refund_params)
        return refund
    except stripe.error.StripeError as e:
        raise HTTPException(status_code=400, detail=str(e))
