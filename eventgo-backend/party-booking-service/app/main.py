from fastapi import FastAPI, HTTPException, Request
from fastapi.middleware.cors import CORSMiddleware
import requests
from . import schemas
from dotenv import load_dotenv

load_dotenv()

app = FastAPI(title="Parting Booking Service")

app.add_middleware(
    CORSMiddleware,
    allow_origins=["http://localhost:3000"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

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


@app.get("/health")
def get_health():
    return {"status": "healthy"}