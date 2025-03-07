from fastapi import FastAPI, Depends, HTTPException, Request
from fastapi.middleware.cors import CORSMiddleware
import stripe
import os
from sqlalchemy.orm import Session
from . import schemas, models, crud
from .database import engine, get_db

from dotenv import load_dotenv

load_dotenv()

# Create database tables
models.Base.metadata.create_all(bind=engine)

app = FastAPI(title="Stripe Service")

app.add_middleware(
    CORSMiddleware,
    allow_origins=["http://localhost:3000"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

stripe.api_key = os.environ.get("STRIPE_SECRET_KEY")

@app.post("/create-payment-intent", response_model=schemas.PaymentIntentResponse)
async def create_payment_intent(payment: schemas.PaymentIntent, db: Session = Depends(get_db)):
    """
    Create a new PaymentIntent for the given amount, with seats and event upon checkout click.
    """
    try:
        intent = stripe.PaymentIntent.create(
            amount=payment.amount,
            currency=payment.currency,
            metadata={
                "event_id": payment.event_id,
                "seats": ",".join(payment.seats),
            }
        )
        
        # Store the payment intent in the database
        payment_intent_db = schemas.PaymentIntentDB(
            stripe_payment_id=intent.id,
            client_secret=intent.client_secret,
            amount=payment.amount,
            currency=payment.currency,
            status=intent.status,
            event_id=payment.event_id,
            seats=payment.seats
        )
        crud.create_payment_intent(db, payment_intent_db)
        
        return {"clientSecret": intent.client_secret}
    except Exception as e:
        raise HTTPException(status_code=400, detail=str(e))

@app.post("/webhook", response_model=schemas.WebhookResponse)
async def stripe_webhook(request: Request, db: Session = Depends(get_db)):
    payload = await request.body()
    sig_header = request.headers.get("stripe-signature")
    
    try:
        event = stripe.Webhook.construct_event(
            payload, sig_header, os.environ.get("STRIPE_WEBHOOK_SECRET")
        )
        
        # Handle the event
        if event.type == "payment_intent.succeeded":
            payment_intent = event.data.object
            # Update payment status in database
            crud.update_payment_intent_status(db, payment_intent.id, "succeeded")
            
            print(f"Payment for {payment_intent.metadata.event_id} succeeded!")
            # Here you would typically make an API call to your tickets service
            
        elif event.type == "payment_intent.payment_failed":
            payment_intent = event.data.object
            # Update payment status in database
            crud.update_payment_intent_status(db, payment_intent.id, "failed")
            
            print(f"Payment for {payment_intent.metadata.event_id} failed.")
            # Release the held seats
            
        return {"status": "success"}
    except Exception as e:
        raise HTTPException(status_code=400, detail=str(e))

@app.get("/payment-status/{payment_intent_id}", response_model=schemas.PaymentStatusResponse)
async def get_payment_status(payment_intent_id: str, db: Session = Depends(get_db)):
    """
    Get the status for a given PaymentIntent ID.
    """
    try:
        # First check if it's in our database
        db_payment_intent = crud.get_payment_intent(db, payment_intent_id)
        
        # But always get the latest from Stripe
        payment = stripe.PaymentIntent.retrieve(payment_intent_id)
        
        # Update our database if status changed
        if db_payment_intent and db_payment_intent.status != payment.status:
            crud.update_payment_intent_status(db, payment_intent_id, payment.status)
            
        return {
            "status": payment.status,
            "amount": payment.amount,
            "currency": payment.currency,
            "metadata": payment.metadata
        }
    except stripe.error.StripeError as e:
        raise HTTPException(status_code=400, detail=str(e))

@app.post("/validate-payment", response_model=schemas.PaymentValidationResponse)
async def validate_payment(payment: schemas.PaymentValidationRequest, db: Session = Depends(get_db)):
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
        
        # Update status in our database if needed
        db_payment_intent = crud.get_payment_intent(db, payment.payment_intent_id)
        if db_payment_intent and db_payment_intent.status != payment_intent.status:
            crud.update_payment_intent_status(db, payment.payment_intent_id, payment_intent.status)
            
        return {"valid": True, "payment_status": payment_intent.status}
    except stripe.error.StripeError as e:
        raise HTTPException(status_code=400, detail=str(e))

@app.post("/confirm-booking", response_model=schemas.BookingConfirmationResponse)
async def confirm_booking(payment: schemas.PaymentValidationRequest, db: Session = Depends(get_db)):
    """Finalize booking after successful payment validation"""
    try:
        # First validate the payment
        validation_result = await validate_payment(payment, db)
        
        if not validation_result.get("valid"):
            raise HTTPException(status_code=400, detail="Payment validation failed")
        
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
    
@app.post("/refund", response_model=schemas.RefundResponse)
async def refund_booking(payment: schemas.RefundRequest, db: Session = Depends(get_db)):
    """Refund a payment intent. Payment intent must have a valid payment method to refund to."""
    try:
        refund = stripe.Refund.create(
            payment_intent=payment.payment_intent_id,
            amount=payment.amount,
            reason=payment.reason
        )
        
        # Store the refund in the database
        refund_db = schemas.RefundDB(
            stripe_refund_id=refund.id,
            payment_intent_id=payment.payment_intent_id,
            amount=refund.amount,
            status=refund.status,
            reason=payment.reason
        )
        crud.create_refund(db, refund_db)
        
        return refund
    except stripe.error.StripeError as e:
        raise HTTPException(status_code=400, detail=str(e))

@app.get("/health", response_model=schemas.HealthResponse)
def get_health(db: Session = Depends(get_db)):
    database_connected = False
    try:
        # Try to execute a simple query
        db.execute("SELECT 1")
        database_connected = True
    except Exception:
        pass
    
    return {
        "status": "healthy", 
        "stripe_configured": bool(stripe.api_key),
        "database_connected": database_connected
    }