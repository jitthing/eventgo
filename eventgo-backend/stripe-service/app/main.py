from fastapi import FastAPI, HTTPException, Request
from fastapi.middleware.cors import CORSMiddleware
import stripe
import os
from . import schemas
import uuid
import time
import json

from dotenv import load_dotenv

load_dotenv()

app = FastAPI(title="Stripe Service")

app.add_middleware(
    CORSMiddleware,
    allow_origins=["http://localhost:3000"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

stripe.api_key = os.environ.get("STRIPE_SECRET_KEY")

# Global dictionary to store split payment records
# In production, use a database instead
# split_payments = {}

@app.post("/create-payment-intent", response_model=schemas.PaymentIntentResponse)
async def create_payment_intent(payment: schemas.PaymentIntent):
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
        return {"clientSecret": intent.client_secret}
    except Exception as e:
        raise HTTPException(status_code=400, detail=str(e))

# this will be moved to orchestrator to react to webhook events
@app.post("/webhook", response_model=schemas.WebhookResponse)
async def stripe_webhook(request: Request):
    """
    This endpoint receives webhook events from Stripe.
    For Stripe to reach this endpoint:
    1. Your server must be publicly accessible (e.g., via ngrok in development)
    2. You must configure the webhook URL in your Stripe dashboard to point to this endpoint
    3. Set up the webhook signing secret in your Stripe dashboard and environment variables
    """
    payload = await request.body()
    sig_header = request.headers.get("stripe-signature")
    
    try:
        # Log the payload and signature header for debugging
        print(f"Payload length: {len(payload)}")
        print(f"Signature Header present: {bool(sig_header)}")
        
        webhook_secret = os.environ.get("STRIPE_WEBHOOK_SECRET")
        print(f"Webhook secret (last 5 chars): {webhook_secret[-5:] if webhook_secret else 'None'}")
        
        # For development purposes - parse payload directly without verification
        # WARNING: In production, always verify signatures
        payload_json = json.loads(payload)
        event_data = stripe.util.convert_to_stripe_object(payload_json, stripe.api_key)
        event = stripe.Event.construct_from(event_data, stripe.api_key)
        
        print(f"Event type: {event.type}")
        
        # Handle the event
        if event.type == "payment_intent.succeeded":
            payment_intent = event.data.object
            # Update your database to mark payment as complete
            print(f"Payment for {payment_intent.metadata.get('event_id')} succeeded!")
            
            # Here you would typically make an API call to your tickets service
            # to create the tickets now that payment is confirmed
            
        elif event.type == "payment_intent.payment_failed":
            payment_intent = event.data.object
            print(f"Payment for {payment_intent.metadata.get('event_id')} failed.")
            # Release the held seats
            
        elif event.type == "checkout.session.completed":
            # Handle completed payments from payment links
            session = event.data.object
            
            # If this is a split payment link checkout
            if session.metadata and "split_payment_id" in session.metadata:
                split_payment_id = session.metadata.get("split_payment_id")
                if split_payment_id in split_payments:
                    print(f"Payment for split payment {split_payment_id} by {session.metadata.get('participant_email')} completed!")
                    
                    # Extract the payment intent ID
                    payment_intent_id = session.payment_intent
                    print(f"Payment Intent ID: {payment_intent_id}")
                    # need to use this payment id to update ticket service for refund
                    
                    # Check if all payments are complete
                    # In a real implementation, you'd update your database
                    status = await get_split_payment_status(schemas.SplitPaymentStatusRequest(split_payment_id=split_payment_id))
                    if status.status == "completed":
                        print(f"Split payment {split_payment_id} fully completed!")
                        # You would finalize the booking here

        return {"status": "success"}
    except Exception as e:
        # Log the error details
        print(f"Error processing webhook: {str(e)}")
        # Return success anyway to acknowledge the webhook (prevents retries)
        # In development, this allows us to see events even when verification fails
        return {"status": "success"}

@app.get("/payment-status/{payment_intent_id}", response_model=schemas.PaymentStatusResponse)
async def get_payment_status(payment_intent_id: str):
    """
    Get the status for a given PaymentIntent ID.
    """
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

@app.post("/validate-payment", response_model=schemas.PaymentValidationResponse)
async def validate_payment(payment: schemas.PaymentValidationRequest):
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

@app.post("/confirm-booking", response_model=schemas.BookingConfirmationResponse)
async def confirm_booking(payment: schemas.PaymentValidationRequest):
    """Finalize booking after successful payment validation"""
    try:
        # First validate the payment
        validation_result = await validate_payment(payment)
        
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
async def refund_booking(payment: schemas.RefundRequest):
    """Refund a payment intent. Payment intent must have a valid payment method to refund to."""
    try:
        refund = stripe.Refund.create(
            payment_intent=payment.payment_intent_id,
            amount=payment.amount,
            reason=payment.reason
        )
        return refund
    except stripe.error.StripeError as e:
        raise HTTPException(status_code=400, detail=str(e))

@app.get("/health", response_model=schemas.HealthResponse)
def get_health():
    return {"status": "healthy", "stripe_configured": bool(stripe.api_key)}

async def generate_payment_link(
    amount: int,
    currency: str,
    name: str,
    email: str,
    redirect_url: str,
    metadata: dict = None,
    expiration: int = None
):
    """
    Generate a Stripe payment link for a given amount
    
    Args:
        amount: The amount in cents to charge
        currency: The currency code (e.g., 'sgd', 'usd')
        name: Name of the product/recipient
        email: Email of the recipient
        redirect_url: URL to redirect after payment
        metadata: Additional metadata to attach to the payment
        expiration: Expiration timestamp (defaults to 24 hours from now)
        
    Returns:
        dict: Payment link details including URL and ID
    """
    if expiration is None:
        expiration = int(time.time()) + 24 * 60 * 60  # 24 hours from now
        
    try:
        # Create a one-time price
        price = stripe.Price.create(
            unit_amount=amount,
            currency=currency,
            product_data={
                'name': name,
            }
        )
        
        # Create a payment link that references the price
        payment_link = stripe.PaymentLink.create(
            line_items=[{
                'price': price.id,
                'quantity': 1
            }],
            after_completion={'type': 'redirect', 'redirect': {'url': redirect_url}},
            metadata=metadata or {}
        )
        
        # Return payment link details
        return {
            "payment_link_id": payment_link.id,
            "url": payment_link.url,
            "email": email,
            "amount": amount,
            "expires_at": expiration
        }
    except Exception as e:
        print(f"Error generating payment link: {e}")
        raise

@app.post("/create-payment-link", response_model=schemas.PaymentLinkResponse)
async def create_payment_link(request: schemas.CreatePaymentLinkRequest):
    """
    Create a single payment link for a customer. Used for ticket transfer
    """
    try:
        metadata = {
            "event_id": request.event_id,
            "description": request.description,
        }
        
        if request.seats:
            metadata["seats"] = ",".join(request.seats)
        
        payment_link = await generate_payment_link(
            amount=request.amount,
            currency=request.currency,
            name=f"Payment for {request.description}",
            email=request.email,
            redirect_url=request.redirect_url,
            metadata=metadata
        )
        
        return {
            "payment_link_id": payment_link["payment_link_id"],
            "url": payment_link["url"],
            "amount": payment_link["amount"],
            "expires_at": payment_link["expires_at"]
        }
    except Exception as e:
        raise HTTPException(status_code=400, detail=str(e))

@app.post("/create-split-payment", response_model=schemas.SplitPaymentResponse)
async def create_split_payment(request: schemas.CreateSplitPaymentRequest):
    """
    Create payment links for multiple participants in a split payment scenario
    """
    try:
        # Generate a unique ID for this split payment
        split_payment_id = str(uuid.uuid4())
        total_amount = sum(participant.amount for participant in request.participants)
        payment_links = []
        
        for participant in request.participants:
            # Create metadata for this participant's payment
            metadata = {
                "split_payment_id": split_payment_id,
                "event_id": request.event_id,
                "seats": ",".join(request.seats),
                "participant_email": participant.email,
                "description": f"Your share of tickets for seats: {', '.join(request.seats)}"
            }
            
            # Generate payment link using the reusable function
            payment_link = await generate_payment_link(
                amount=participant.amount,
                currency=request.currency,
                name=f"Split payment for {request.description} - {participant.name}",
                email=participant.email,
                redirect_url=request.redirect_url,
                metadata=metadata
            )
            
            # Ensure we have participant_email in the result
            payment_link["participant_email"] = participant.email
            
            payment_links.append(payment_link)
        
        # # Store the split payment info (in production, use a database)
        # split_payments[split_payment_id] = {
        #     "event_id": request.event_id,
        #     "seats": request.seats,
        #     "total_amount": total_amount,
        #     "payment_links": payment_links,
        #     "created_at": int(time.time())
        # }
        
        return {
            "split_payment_id": split_payment_id, 
            "payment_links": payment_links,
            "total_amount": total_amount,
            "event_id": request.event_id,
            "seats": request.seats
        }
        
    except Exception as e:
        raise HTTPException(status_code=400, detail=str(e))

# @app.post("/split-payment-status", response_model=schemas.SplitPaymentStatusResponse)
# async def get_split_payment_status(request: schemas.SplitPaymentStatusRequest):
    """
    Check the status of a split payment by retrieving all payment links
    """
    try:
        if request.split_payment_id not in split_payments:
            raise HTTPException(status_code=404, detail="Split payment not found")
        
        split_payment_data = split_payments[request.split_payment_id]
        payment_link_statuses = []
        amount_paid = 0
        
        # Check status of each payment link
        for link in split_payment_data["payment_links"]:
            payment_link = stripe.PaymentLink.retrieve(link["payment_link_id"])
            
            # Check if there are completed payments for this link
            payment_status = "unpaid"
            
            # Get list of checkouts from this payment link
            checkouts = stripe.checkout.Session.list(
                payment_link=link["payment_link_id"]
            )
            
            # Check if any checkout is paid
            for checkout in checkouts.data:
                if checkout.payment_status == "paid":
                    payment_status = "paid"
                    amount_paid += link["amount"]
                    break
            
            # Check if expired
            if payment_status == "unpaid" and time.time() > link["expires_at"]:
                payment_status = "expired"
            
            payment_link_statuses.append({
                "payment_link_id": link["payment_link_id"],
                "participant_email": link["participant_email"],
                "status": payment_status,
                "amount": link["amount"]
            })
        
        # Determine overall status
        amount_pending = split_payment_data["total_amount"] - amount_paid
        
        if amount_paid == 0 and time.time() > split_payment_data["payment_links"][0]["expires_at"]:
            status = "expired"
        elif amount_paid == split_payment_data["total_amount"]:
            status = "completed"
        elif amount_paid > 0:
            status = "partially_paid"
        else:
            status = "pending"
        
        return {
            "split_payment_id": request.split_payment_id,
            "event_id": split_payment_data["event_id"],
            "seats": split_payment_data["seats"],
            "total_amount": split_payment_data["total_amount"],
            "status": status,
            "payment_links": payment_link_statuses,
            "amount_paid": amount_paid,
            "amount_pending": amount_pending
        }
        
    except Exception as e:
        raise HTTPException(status_code=400, detail=str(e))