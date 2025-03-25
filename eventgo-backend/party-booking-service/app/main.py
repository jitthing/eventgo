from fastapi import FastAPI, HTTPException, Request
from fastapi.middleware.cors import CORSMiddleware
import os
import stripe
import datetime
import requests
import json
import pika
import uuid
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

stripe.api_key = os.environ.get("STRIPE_SECRET_KEY")

# Define the base URL for the stripe service - use the Docker service name
STRIPE_SERVICE_URL = os.environ.get("STRIPE_SERVICE_URL", "http://stripe-service:8000")
TICKET_INVENTORY_URL = os.environ.get("TICKET_INVENTORY_URL", "http://ticket-inventory:8080")

# RabbitMQ Configuration
RABBITMQ_HOST = os.environ.get("RABBITMQ_HOST", "rabbitmq")
RABBITMQ_PORT = int(os.environ.get("RABBITMQ_PORT", 5672))
RABBITMQ_USERNAME = os.environ.get("RABBITMQ_USERNAME", "guest")
RABBITMQ_PASSWORD = os.environ.get("RABBITMQ_PASSWORD", "guest")
NOTIFICATION_EXCHANGE = os.environ.get("NOTIFICATION_EXCHANGE", "notification_exchange")
NOTIFICATION_ROUTING_KEY = os.environ.get("NOTIFICATION_ROUTING_KEY", "notification.queue")

def publish_message(message, routing_key=NOTIFICATION_ROUTING_KEY):
    """
    Publish a message to RabbitMQ
    """
    try:
        # Setup RabbitMQ connection
        credentials = pika.PlainCredentials(RABBITMQ_USERNAME, RABBITMQ_PASSWORD)
        connection = pika.BlockingConnection(
            pika.ConnectionParameters(
                host=RABBITMQ_HOST,
                port=RABBITMQ_PORT,
                credentials=credentials
            )
        )
        channel = connection.channel()
        
        # Ensure exchange exists
        channel.exchange_declare(
            exchange=NOTIFICATION_EXCHANGE,
            exchange_type='topic',
            durable=True
        )
        
        # Publish message
        channel.basic_publish(
            exchange="",
            routing_key=routing_key,
            body=json.dumps(message),
            properties=pika.BasicProperties(delivery_mode=2, content_type='application/json')
        )
        
        print(f"Published message with ID {message_id} to exchange {NOTIFICATION_EXCHANGE}")
        connection.close()
        return True
    except Exception as e:
        print(f"Failed to publish message: {str(e)}")
        return False

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
            
            # If this is a split payment link checkout
            if session.metadata and "split_payment_id" in session.metadata:
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

                    payload = {
                        # "notificationId": str(uuid.uuid4()),
                        # "timestamp": datetime.now().isoformat(),
                        "message": f"Your payment of ${payment_intent_id} has been completed for reservation {reservation_id}",
                        "subject": "Payment Completed",
                        "recipientEmailAddress": participant_email,
                        "notificationType": "PAYMENT_CONFIRMATION"
                    }

                    # Prepare notification payload
                    # notification_payload = {
                    #     "subject": "Payment Completed",
                    #     "message": f"Your payment of ${payment_intent_id} has been completed for reservation {reservation_id}",
                    #     "recipientEmailAddress": participant_email,
                    #     "notificationType": "PAYMENT_CONFIRMATION"
                    # }
                    
                    # Publish to notification queue
                    publish_success = publish_message(payload)
                    if publish_success:
                        print(f"Successfully published payment completion notification for {participant_email}")
                    else:
                        print(f"Failed to publish notification for {participant_email}")
                    
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
    ORCHESTRATOR FUNCTION TO HANDLE PARTY BOOKING
    1. Initiate split payment to payment service
    2. Publish payment link and payment receiver to RabbitMQ Queue
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
                # Push notification about payment link to queue
                # notification_payload = {
                #     "subject": "Payment Link Created",
                #     "message": f"Please complete your payment using this link: {payment_link_obj.get('url')}. Amount: ${payment_link_obj.get('amount')/100:.2f}",
                #     "recipientEmailAddress": payment_link_obj.get('participant_email'),
                #     "notificationType": "PAYMENT_LINK"
                # }
                payload = {
                        # "notificationId": str(uuid.uuid4()),
                        # "timestamp": datetime.now().isoformat(),
                        "message": f"Please complete your payment using this link: {payment_link_obj.get('url')}. Amount: ${payment_link_obj.get('amount')/100:.2f}",
                        "subject": "Payment Link",
                        "recipientEmailAddress": payment_link_obj.get('participant_email'),
                        "notificationType": "PAYMENT_LINK"
                    }
                publish_message(payload)
                print(f"Published payment link for {payment_link_obj.get('participant_email')} to notification queue")

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