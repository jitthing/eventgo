from fastapi import FastAPI, HTTPException, Request
from fastapi.middleware.cors import CORSMiddleware
import os
import stripe
import requests
import json
from . import schemas
from dotenv import load_dotenv
import pika
import json
from datetime import datetime
import uuid

load_dotenv()

app = FastAPI(title="Parting Booking Service")

app.add_middleware(
    CORSMiddleware,
    allow_origins=["http://localhost:3000"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

stripe.api_key = os.environ.get("STRIPE_SECRET_KEY")

# Service URLs
STRIPE_SERVICE_URL = os.environ.get("STRIPE_SERVICE_URL")
TICKET_INVENTORY_URL = os.environ.get("TICKET_INVENTORY_URL")
TICKET_TRANSFER_URL = os.environ.get("TICKET_TRANSFER_URL")

# RabbitMQ Configuration
RABBITMQ_HOST = os.environ.get("RABBITMQ_HOST")
RABBITMQ_PORT = int(os.environ.get("RABBITMQ_PORT"))
RABBITMQ_USERNAME = os.environ.get("RABBITMQ_USERNAME")
RABBITMQ_PASSWORD = os.environ.get("RABBITMQ_PASSWORD")

# Queue names
NOTIFICATION_QUEUE = "notification.queue"


def get_rabbitmq_connection():
    """Create and return a RabbitMQ connection"""
    credentials = pika.PlainCredentials(RABBITMQ_USERNAME, RABBITMQ_PASSWORD)
    parameters = pika.ConnectionParameters(
        host=RABBITMQ_HOST,
        port=RABBITMQ_PORT,
        credentials=credentials
    )
    return pika.BlockingConnection(parameters)

def publish_notification(notification: schemas.TransferNotification):
    """Publish notification to RabbitMQ queue"""
    try:
        connection = get_rabbitmq_connection()
        channel = connection.channel()
        
        # Declare queue
        channel.queue_declare(queue=NOTIFICATION_QUEUE, durable=True)
        
        # Publish message
        channel.basic_publish(
            exchange='',
            routing_key=NOTIFICATION_QUEUE,
            body=json.dumps({
                "subject": notification.subject,
                "message": notification.message,
                "recipientEmailAddress": notification.recipient_email_address,
                "notificationId": str(uuid.uuid4()),
                "timestamp": datetime.now().isoformat()
            }),
            properties=pika.BasicProperties(
                delivery_mode=2,  # make message persistent
            )
        )
        
        connection.close()
    except Exception as e:
        print(f"Error publishing notification: {str(e)}")
        raise

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
        # print(f"Payload length: {len(payload)}")
        # print(f"Signature Header present: {bool(sig_header)}")
        
        # webhook_secret = os.environ.get("STRIPE_WEBHOOK_SECRET")
        # print(f"Webhook secret (last 5 chars): {webhook_secret[-5:] if webhook_secret else 'None'}")
        
        # For development purposes - parse payload directly without verification
        # WARNING: In production, always verify signatures
        payload_json = json.loads(payload)
        event_data = stripe.util.convert_to_stripe_object(payload_json, stripe.api_key)
        event = stripe.Event.construct_from(event_data, stripe.api_key)
        
        print(f"Event type: {event.type}")
        
        # Handle the event
        if event.type == "payment_intent.succeeded":
            try:
                payment_intent = event.data.object
                # Update your database to mark payment as complete
                print(f"Payment for {payment_intent.metadata.get('event_id')} succeeded!")
                return {"status": "ok"}
            except Exception as e:
                return {"status": "ok"}
            
            # Here you would typically make an API call to your tickets service
            # to create the tickets now that payment is confirmed
            
        elif event.type == "payment_intent.payment_failed":
            try:
                payment_intent = event.data.object
                print(f"Payment for {payment_intent.metadata.get('event_id')} failed.")
                # Release the held seats
                return {"status": "ok"}
            except Exception as e:
                return {"status": "ok"}
            
        elif event.type == "checkout.session.completed":
            # Handle completed payments from payment links
            session = event.data.object

            # If this is a ticket transfer checkout
            if session.metadata and "transfer_id" in session.metadata:
                try:
                    # transfer_id = session.metadata.get("transfer_id")
                    ticket_id = session.metadata.get("ticket_id")
                    seller_id = session.metadata.get("seller_id")
                    seller_email = session.metadata.get("seller_email")
                    buyer_email = session.metadata.get("buyer_email")
                    buyer_id = session.metadata.get("buyer_id")
                    amount_in_cents = session.metadata.get("amount_in_cents")
                    original_payment_intent_id = session.metadata.get("original_payment_intent")

                    # Extract the new payment intent ID from the session
                    new_payment_intent_id = session.payment_intent

                    transfer_body = {
                        "original_payment_intent": original_payment_intent_id,
                        "new_payment_intent": new_payment_intent_id,
                        "ticket_id": ticket_id,
                        "seller_id": seller_id,
                        "seller_email": seller_email,
                        "buyer_id": buyer_id,
                        "buyer_email": buyer_email,
                        "amount": amount_in_cents
                    }
                    print(f"[CALL] Calling {TICKET_TRANSFER_URL} to transfer tickets with {transfer_body}")
                    ticket_transfer = requests.post(
                        f"{TICKET_TRANSFER_URL}/transfer",
                        json=transfer_body
                    )
                    if ticket_transfer.status_code != 200:
                        return {"status": "error", "message": f"Transfer failed: {ticket_transfer.text}"}
                    
                    return { "status": "ok"}
                except Exception as e:
                    raise HTTPException(status_code=500, detail=str(e))
                
                
            # If this is a split payment link checkout
            elif session.metadata and "split_payment_id" in session.metadata:
                # split_payment_id = session.metadata.get("split_payment_id")
                # if split_payment_id in split_payments:
                #     print(f"Payment for split payment {split_payment_id} by {session.metadata.get('participant_email')} completed!")
                    
                    # Extract the payment intent ID
                payment_intent_id = session.payment_intent
                participant_email = session.metadata.get("participant_email")
                user_id = session.metadata.get("user_id")
                reservation_id = session.metadata.get("reservation_id")
                ticket_id = session.metadata.get("ticket_id")
                print(f"Reservation ID is {reservation_id} and Ticket ID is {ticket_id} and belongs to {participant_email}")
                print(f"Payment Intent ID: {payment_intent_id}")
                # need to use this payment id to update ticket service for update, for future use for refund
                    
                # to change one more field to include ticket id for one ticket reservation
                ticket_confirm_req = {
                    "paymentIntentId": payment_intent_id,
                    "reservationId": reservation_id,
                    "userId": user_id
                }

                print(ticket_confirm_req)

                try:
                    confirm_ticket_endpoint = f"{TICKET_INVENTORY_URL}/tickets/confirm"
                    print(f"Calling ticket service at: {confirm_ticket_endpoint}")
                    ticket_response = requests.patch(
                        confirm_ticket_endpoint, 
                        json=ticket_confirm_req,
                        timeout=10)
                    
                    ticket_response_object = ticket_response.json()
                    print(ticket_response_object)

                    # publish to queue
                    return ticket_response_object
                except Exception as e:
                    print(f"Unexpected error: {str(e)}")
                    return {"status": "error", "message": f"Unexpected error: {str(e)}"}
                
        else:
            print("Event type not covered")
            return {"status": "ok"}
        
    except Exception as e:
        # Log the error details
        print(f"Error processing webhook: {str(e)}")
        # Return success anyway to acknowledge the webhook (prevents retries)
        # In development, this allows us to see events even when verification fails
        return {"status": "success"}

@app.post("/party-booking")
async def party_booking(request: schemas.PartyBookingRequest):
    """
    """
    # get links and email
    print(request.items)
    reservation_id = request.reservation_id
    event_id = request.event_id
    leader = ""
    participants = []
    for item in request.items:
        if ";" in item.user_email:
            leader = item.user_email[:-1]
            participants.append({"email": leader, "user_id": item.user_id, "amount": item.price, "ticket_id": item.ticket_id})
        else:
            participants.append({"email": item.user_email, "user_id": item.user_id, "amount": item.price, "ticket_id": item.ticket_id})
    
    split_payments_req = {
        "event_id": event_id,
        "reservation_id": reservation_id,
        "currency": "sgd",
        "participants": participants,
        "description": "test test test",
        "redirect_url": "http://localhost:3000"
        }
    try:
        # Use the Docker service name and port instead of localhost
        create_split_endpoint = f"{STRIPE_SERVICE_URL}/create-split-payment"
        print(f"Calling stripe service at: {create_split_endpoint}")
        
        payment_links_response = requests.post(
            create_split_endpoint, 
            json=split_payments_req,  # Fixed variable name here - was split_payments
            timeout=10  # Add timeout to prevent hanging requests
        )
        
        if payment_links_response.status_code != 200:
            print(f"Error response from stripe service: {payment_links_response.status_code}")
            print(f"Response content: {payment_links_response.text}")
            return {"status": "error", "message": f"Stripe service returned {payment_links_response.status_code}"}
            
        payment_link_objects = payment_links_response.json()
        
        res = {}
        # Access the payment_links array correctly
        for payment_link_obj in payment_link_objects.get("payment_links", []):
            if payment_link_obj.get('participant_email') == leader:
                res["redirect_url"] = payment_link_obj.get('url')
            else:
                # push to queue
                print(f"{payment_link_obj.get('url')} belongs to {payment_link_obj.get('participant_email')}")

        # get leaders url and return, the rest push to queue
        # publish to queue 

        return {"status": "ok", "data": res}
    except requests.exceptions.RequestException as e:
        print(f"Connection error: {str(e)}")
        return {"status": "error", "message": f"Connection to stripe service failed: {str(e)}"}
    except Exception as e:
        print(f"Unexpected error: {str(e)}")
        return {"status": "error", "message": f"Unexpected error: {str(e)}"}
    

async def ticket_booking():
    """
    Handle ticket reservation upon checkout.session.completed from webhook
    """
    return None

@app.get("/health", response_model=schemas.HealthResponse)
def get_health():
    return {"status": "healthy", "stripe_configured": bool(stripe.api_key)}