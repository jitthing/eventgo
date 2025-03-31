from fastapi import FastAPI, HTTPException, Request, BackgroundTasks
from fastapi.middleware.cors import CORSMiddleware
import asyncio
import os
import stripe
import datetime
import requests
import json
import pika
from . import schemas
from dotenv import load_dotenv
import pika
import json
from datetime import datetime
import uuid
import time

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
AUTH_URL   = os.getenv("AUTH_API_URL", "http://auth-service:8000")
EVENTS_URL = os.getenv("EVENTS_API_URL", "https://personal-vyyhsf3d.outsystemscloud.com/EventsOutsystem/rest/EventsAPI")

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
        
        # print(f"Published message with ID {message_id} to exchange {NOTIFICATION_EXCHANGE}")
        connection.close()
        return True
    except Exception as e:
        print(f"Failed to publish message: {str(e)}")
        return False



def send_payment_notification(user_id: int, event_id: int, ticket_id: int, amount_cents: int, url: str, subject_prefix: str):
    user = requests.get(f"{AUTH_URL}/users/{user_id}").json()
    event = requests.get(f"{EVENTS_URL}/events/{event_id}").json().get("EventAPI", {})

    formatted_date = datetime.fromisoformat(event["date"].replace("Z","+00:00")).strftime("%B %d, %Y at %I:%M %p")
    subject = f"{subject_prefix}: '{event['title']}'"
    message = (
        f"Hello {user['full_name']},\n\n"
        f"You’re invited to '{event['title']}' on {formatted_date} at {event['venue']}. "
        f"Please complete your payment of ${amount_cents/100:.2f} for ticket #{ticket_id} by clicking below:\n\n"
        f"{url}\n\n"
        "If you have any questions, visit our Help Center at https://help.eventgo.com or reply to this email.\n\n"
        "Sincerely,\nEventGo Customer Support"
    )

    publish_message({"subject": subject, "message": message, "recipientEmailAddress": user["email"]})

async def refund_split(ticketList: list[int], sleepTime: int):
    await asyncio.sleep(sleepTime)
    print("[PROCESS] Starting refund checks")
    ticketsReq = requests.get(
        f"{TICKET_INVENTORY_URL}/tickets/tickets-by-ids",
        json={"ticketList": ticketList}
        )
    # print(ticketsReq.json())
    tickets = ticketsReq.json().get("data")
    toRefund = []
    # print(tickets)
    for ticket in tickets:
        try:
            if ticket.get("status") == "sold" and ticket.get("preference") == "refund":
                print("[PROCESS] Calling refund now")
                toRefund.append(ticket.get("ticketId"))
                refund = requests.post(
                    f"{STRIPE_SERVICE_URL}/refund",
                    json={"payment_intent_id": ticket.get("paymentIntentId")}
                )

        except Exception as e:
            print(f"Unexpected error: {str(e)}")
            return {"status": "error", "message": f"Unexpected error: {str(e)}"}
    
    try:
        print("[PROCESS] Calling cancellation now")
        cancel = requests.patch(
            f"{TICKET_INVENTORY_URL}/tickets/cancel-ticket",
            json={"ticketList": toRefund}
        )
        print(cancel.json())

        return {"status": "ok"}
    except Exception as e:
        print(f"Unexpected error: {str(e)}")
        return {"status": "error", "message": f"Unexpected error: {str(e)}"}
    


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
            # print("[DEBUG] IM HERE")
            # print(session.metadata)

            # If this is a ticket transfer checkout
            if session.metadata and "transfer_id" in session.metadata:
                ticket_id = session.metadata.get("ticket_id")
                seller_id = session.metadata.get("seller_id")
                seller_email = session.metadata.get("seller_email")
                buyer_email = session.metadata.get("buyer_email")
                buyer_id = session.metadata.get("buyer_id")
                amount_in_cents = session.metadata.get("amount_in_cents")
                event_id = session.metadata.get("event_id")
                # original_payment_intent_id = session.metadata.get("original_payment_intent")

                # Extract the new payment intent ID from the session
                new_payment_intent_id = session.payment_intent

                transfer_body = {
                    # "original_payment_intent": original_payment_intent_id,
                    "new_payment_intent": new_payment_intent_id,
                    "ticket_id": ticket_id,
                    "seller_id": seller_id,
                    "seller_email": seller_email,
                    "buyer_id": buyer_id,
                    "buyer_email": buyer_email,
                    "amount": amount_in_cents,
                    "event_id": event_id
                }
                # print(f"[PROCESS] Transfer body is {transfer_body}")
                try:
                    # transfer_id = session.metadata.get("transfer_id")
                    print(f"[CALL] Calling {TICKET_TRANSFER_URL} to transfer tickets with {transfer_body}")
                    ticket_transfer = requests.post(
                        f"{TICKET_TRANSFER_URL}/transfer",
                        json=transfer_body,
                        timeout=10  # Set a timeout of 10 seconds
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
                    "userId": user_id,
                    "ticketId": ticket_id
                }

                print(ticket_confirm_req)

                try:
                    confirm_ticket_endpoint = f"{TICKET_INVENTORY_URL}/tickets/confirm-split"
                    print(f"Calling ticket service at: {confirm_ticket_endpoint}")
                    ticket_response = requests.patch(
                        confirm_ticket_endpoint, 
                        json=ticket_confirm_req,
                        timeout=10)
                    
                    ticket_response_object = ticket_response.json()
                    print(ticket_response_object)

                    # payload = {
                    #     # "notificationId": str(uuid.uuid4()),
                    #     # "timestamp": datetime.now().isoformat(),
                    #     "message": f"Your payment of ${payment_intent_id} has been completed for reservation {reservation_id}",
                    #     "subject": "Payment Completed",
                    #     "recipientEmailAddress": participant_email,
                    #     # "notificationType": "PAYMENT_CONFIRMATION"
                    # }
                    
                    # Publish to notification queue
                    # publish_success = publish_message(payload)

                    send_payment_notification(
                        user_id=user_id,
                        event_id=session.metadata["event_id"],
                        ticket_id=session.metadata["ticket_id"],
                        amount_cents=int(session.amount_total),
                        url="",  # no link needed for confirmation
                        subject_prefix="Confirmation: Payment Completed"
                    )
                                    
                    # if publish_success:
                    #     print(f"Successfully published payment completion notification for {participant_email}")
                    # else:
                    #     print(f"Failed to publish notification for {participant_email}")
                    
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
async def party_booking(request: schemas.PartyBookingRequest, background_tasks: BackgroundTasks):
    """
    """
    # get links and email
    print(request.items)
    reservation_id = request.reservation_id
    event_id = request.event_id
    event_title = request.event_title
    event_description = request.event_description

    leader = ""
    participants = []
    ticket_ids = []
    for item in request.items:
        ticket_ids.append(item.ticket_id)
        if ";" in item.user_email:
            leader = item.user_email[:-1]
            participants.append({"email": leader, "user_id": item.user_id, "amount": item.price, "ticket_id": item.ticket_id, "redirect_url": f"http://localhost:3000/confirmation?eventId={event_id}&ticket={item.ticket_id}&total={item.price/100:.2f}&split=true"})
        else:
            participants.append({"email": item.user_email, "user_id": item.user_id, "amount": item.price, "ticket_id": item.ticket_id, "redirect_url": f"http://localhost:3000/confirmation?eventId={event_id}&ticket={item.ticket_id}&total={item.price/100:.2f}&split=true"})
    
    split_payments_req = {
        "event_id": event_id,
        "reservation_id": reservation_id,
        "currency": "sgd",
        "participants": participants,
        "description": event_title + '\n' + event_description,
        # "redirect_url": "http://localhost:3000/confirmation?"
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
                # payload = {
                #         # "notificationId": str(uuid.uuid4()),
                #         # "timestamp": datetime.now().isoformat(),
                #         "message": f"Please complete your payment using this link: {payment_link_obj.get('url')}. Amount: ${payment_link_obj.get('amount')/100:.2f}",
                #         "subject": "Payment Link",
                #         "recipientEmailAddress": payment_link_obj.get('participant_email'),
                #         # "notificationType": "PAYMENT_LINK"
                #     }
                # publish_message(payload)

                send_payment_notification(
                    user_id=payment_link_obj["user_id"],
                    event_id=event_id,
                    ticket_id=payment_link_obj["ticket_id"],
                    amount_cents=payment_link_obj["amount"],
                    url=payment_link_obj["url"],
                    subject_prefix="Action Required: Complete Your Payment"
                )
                                
                print(f"Published payment link for {payment_link_obj.get('participant_email')} to notification queue")

        background_tasks.add_task(refund_split, ticket_ids, 60)
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