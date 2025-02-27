from fastapi import FastAPI, Depends, HTTPException, Request
from fastapi.middleware.cors import CORSMiddleware
import stripe
import os
from dotenv import load_dotenv
from pydantic import BaseModel

load_dotenv()

app = FastAPI()

app.add_middleware(
    CORSMiddleware,
    allow_origins=["http://localhost:3000"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

stripe.api_key = os.environ.get("STRIPE_SECRET_KEY")

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

@app.post("/create-payment-intent")
async def create_payment_intent(payment: PaymentIntent):
    try:
        intent = stripe.PaymentIntent.create(
            amount=payment.amount,
            currency=payment.currency,
            metadata={
                "event_id": payment.event_id,
                "seats": ",".join(payment.seats),
            }
        )
        return {"clientSecret": intent.client_secret}
    except Exception as e:
        raise HTTPException(status_code=400, detail=str(e))

@app.post("/webhook")
async def stripe_webhook(request: Request):
    payload = await request.body()
    sig_header = request.headers.get("stripe-signature")
    
    try:
        event = stripe.Webhook.construct_event(
            payload, sig_header, os.environ.get("STRIPE_WEBHOOK_SECRET")
        )
        
        # Handle the event
        if event.type == "payment_intent.succeeded":
            payment_intent = event.data.object
            # Update your database to mark payment as complete
            # You could call your tickets service here to finalize the booking
            print(f"Payment for {payment_intent.metadata.event_id} succeeded!")
            
            # Here you would typically make an API call to your tickets service
            # to create the tickets now that payment is confirmed
            
        elif event.type == "payment_intent.payment_failed":
            payment_intent = event.data.object
            print(f"Payment for {payment_intent.metadata.event_id} failed.")
            # Release the held seats
            
        return {"status": "success"}
    except Exception as e:
        raise HTTPException(status_code=400, detail=str(e))

@app.get("/payment-status/{payment_intent_id}")
async def get_payment_status(payment_intent_id: str):
    try:
        payment = stripe.PaymentIntent.retrieve(payment_intent_id)
        return {
            "status": payment.status,
            "amount": payment.amount,
            "currency": payment.currency,
            "metadata": payment.metadata
        }
    except stripe.error.StripeError as e:
        raise HTTPException(status_code=400, detail=str(e))

@app.post("/validate-payment")
async def validate_payment(payment: PaymentValidationRequest):
    try:
        # Retrieve the payment intent to verify its status
        payment_intent = stripe.PaymentIntent.retrieve(payment.payment_intent_id)
        
        # Check if payment was successful
        if payment_intent.status != "succeeded":
            raise HTTPException(status_code=400, detail="Payment not successful")
        
        # Verify the payment was for this specific event and seats
        if payment_intent.metadata.get("event_id") != payment.event_id:
            raise HTTPException(status_code=400, detail="Payment was for a different event")
            
        paid_seats = payment_intent.metadata.get("seats", "").split(",")
        if sorted(paid_seats) != sorted(payment.seats):
            raise HTTPException(status_code=400, detail="Payment was for different seats")
            
        return {"valid": True, "payment_status": payment_intent.status}
    except stripe.error.StripeError as e:
        raise HTTPException(status_code=400, detail=str(e))

@app.post("/confirm-booking")
async def confirm_booking(payment: PaymentValidationRequest):
    """Finalize booking after successful payment validation"""
    try:
        # First validate the payment
        validation_result = await validate_payment(payment)
        
        if not validation_result.get("valid"):
            raise HTTPException(status_code=400, detail="Payment validation failed")
        
        # Here you would make an API call to your booking/tickets service
        # to finalize the booking and create the tickets
        
        # For now we'll just return success
        return {
            "status": "success",
            "event_id": payment.event_id,
            "seats": payment.seats,
            "payment_intent_id": payment.payment_intent_id
        }
    except HTTPException:
        raise
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"Error confirming booking: {str(e)}")

@app.get("/health")  # Changed from "/heath"
def get_health():
    return {"status": "healthy", "stripe_configured": bool(stripe.api_key)}