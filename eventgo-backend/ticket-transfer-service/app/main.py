from fastapi import FastAPI, HTTPException, Request
from fastapi.middleware.cors import CORSMiddleware
import os
import requests
import json
import uuid
from . import schemas
from dotenv import load_dotenv
import pika
import json
from datetime import datetime
import logging
import sys
import traceback

logging.basicConfig(
    level=logging.DEBUG,
    format="%(asctime)s [%(levelname)s] %(name)s: %(message)s",
    handlers=[
        logging.StreamHandler(sys.stdout)  # Ensure logs go to stdout (visible in Docker logs)
    ]
)
logger = logging.getLogger(__name__)


load_dotenv()

app = FastAPI(title="Ticket Transfer Service")

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

# Service URLs
STRIPE_SERVICE_URL = os.environ.get("STRIPE_SERVICE_URL")
TICKET_INVENTORY_URL = os.environ.get("TICKET_INVENTORY_URL")
AUTH_URL   = os.getenv("AUTH_API_URL", "http://auth-service:8000")
EVENTS_URL = os.getenv("EVENTS_API_URL", "https://personal-vyyhsf3d.outsystemscloud.com/EventsOutsystem/rest/EventsAPI")

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
        logger.error("Error publishing notification: %s", traceback.format_exc())

        print(f"Error publishing notification: {str(e)}")
        raise

# Step 1: Generating Payment Link and Sending to Buyer
@app.post("/generate-transfer-payment-link", response_model=schemas.TransferPaymentResponse)
async def generate_transfer_payment_link(request: schemas.TransferPaymentRequest):
    """
    Generate a payment link for ticket transfer.
    """
    try:
        # Generate a transfer ID
        transfer_id = str(uuid.uuid4())
        
        # Get ticket information including price from ticket inventory
        ticket_info_response = requests.get(
            f"{TICKET_INVENTORY_URL}/tickets/id/{request.ticket_id}",
            timeout=10
        )
        
        if ticket_info_response.status_code != 200:
            raise HTTPException(
                status_code=ticket_info_response.status_code,
                detail=f"Failed to retrieve ticket info: {ticket_info_response.text}"
            )
        
        ticket_info_json = ticket_info_response.json()
        ticket_data = ticket_info_json.get("data")
        if not ticket_data or len(ticket_data) == 0:
            raise HTTPException(status_code=404, detail="Ticket not found")

        ticket_info = ticket_data[0]

        # New: Mark the ticket as transferring before generating the payment link
        mark_resp = requests.patch(
            f"{TICKET_INVENTORY_URL}/tickets/mark-transferring",
            json={"ticket_id": int(request.ticket_id)},
            timeout=10
        )
        if mark_resp.status_code != 200:
            raise HTTPException(
                status_code=mark_resp.status_code,
                detail=f"Failed to mark ticket as transferring: {mark_resp.text}"
            )

        # Get the ticket price from ticket inventory
        ticket_price = ticket_info.get("price")
        event_id = ticket_info.get("eventId")
        # original_payment_intent = ticket_info.get("payment_intent_id")
        if not ticket_price:
            raise HTTPException(
                status_code=400,
                detail="Ticket price not available"
            )
        
        amount_in_cents = int(float(ticket_price) * 100)
        
        # Create payment link request
        payment_link_req = {
            "amount": amount_in_cents,  # Use price from database
            "currency": "sgd",
            "description": request.description or f"Ticket Transfer - Ticket #{request.ticket_id}",
            "email": request.buyer_email,
            "redirect_url": request.redirect_url + str(ticket_price),
            "event_id": str(event_id),
            "metadata": {
                "transfer_id": transfer_id,
                "ticket_id": str(request.ticket_id),
                "seller_id": str(request.seller_id),
                "seller_email": request.seller_email,
                "buyer_email": request.buyer_email,
                "buyer_id": str(request.buyer_id),
                "amount_in_cents": amount_in_cents,
                "event_id": event_id
                # "original_payment_intent": original_payment_intent
            }
        }

        # Call stripe service to create payment link
        create_link_endpoint = f"{STRIPE_SERVICE_URL}/create-payment-link"
        payment_link_response = requests.post(
            create_link_endpoint,
            json=payment_link_req,
            timeout=30
        )

        if payment_link_response.status_code != 200:
            raise HTTPException(
                status_code=payment_link_response.status_code,
                detail=f"Stripe service error: {payment_link_response.text}"
            )

        # Send notification to buyer
        response_data = payment_link_response.json()
        # notification = schemas.TransferNotification(
        #     subject="Ticket Transfer Payment Link",
        #     message=f"You have received a ticket transfer payment link. Click here to complete the payment: {response_data['url']}",
        #     recipient_email_address=request.buyer_email
        # )
        # publish_notification(notification)

        # Construct the response with the transfer_id

        # Fetch event details for context
        ticket_resp = requests.get(f"{TICKET_INVENTORY_URL}/tickets/id/{request.ticket_id}", timeout=10)
        ticket_resp.raise_for_status()
        ticket = ticket_resp.json()

        # print(ticket["event_id"])

        event_resp = requests.get(f"{EVENTS_URL}/events/{event_id}")
        event_resp.raise_for_status()
        event = event_resp.json().get("EventAPI", {})

        print(event["date"])

        formatted_date = datetime.fromisoformat(event.get("date").replace("Z", "+00:00")).strftime("%B %d, %Y at %I:%M %p")

        subject = f"Important Notice: Ticket Transfer Payment Link for '{event['title']}'"
        message = (
            f"Hello,\n\n"
            f"You have been invited to purchase ticket #{request.ticket_id} for '{event['title']}', scheduled on "
            f"{formatted_date} at {event['venue']}. To complete the transfer, please follow this secure payment link:\n\n"
            f"{response_data['url']}\n\n"
            f"The ticket price is SGD {response_data['amount']/100:.2f}. This link will expire on "
            f"{datetime.fromtimestamp(response_data['expires_at']).strftime('%B %d, %Y at %I:%M %p')}.\n\n"
            f"If you have any questions, visit our Help Center at https://help.eventgo.com or reply to this email.\n\n"
            f"Thank you for choosing EventGo.\n\n"
            f"Sincerely,\nEventGo Customer Support"
        )

        publish_notification(schemas.TransferNotification(
            subject=subject,
            message=message,
            recipient_email_address=request.buyer_email
        ))

        return {
            "transfer_id": transfer_id,
            "payment_link_id": response_data["payment_link_id"],
            "url": response_data["url"],
            "amount": response_data["amount"],
            "expires_at": response_data["expires_at"]
        }

    except Exception as e:
        logger.error("Error in generate_transfer_payment_link: %s", traceback.format_exc())

        raise HTTPException(status_code=500, detail=str(e))


@app.patch("/transfer")
async def transfer(request: schemas.TicketTransferRequest):
    """
    Transfer ownership of ticket from owner to buyer
    """
    # Get the original payment_intent_id from ticket inventory service
    try:
        print(f"Retrieving ticket info for ticket ID: {request.ticket_id}")
        ticket_info_response = requests.get(
            f"{TICKET_INVENTORY_URL}/tickets/id/{request.ticket_id}",
            timeout=10
        )
        
        if ticket_info_response.status_code != 200:
            print(f"Failed to get ticket info: {ticket_info_response.status_code} - {ticket_info_response.text}")
            return {"status": "error", "message": f"Failed to retrieve ticket info: {ticket_info_response.text}"}
        
        # ticket_info = ticket_info_response.json()

        ticket_info_json = ticket_info_response.json()
        ticket_data = ticket_info_json.get("data")
        if not ticket_data or len(ticket_data) == 0:
            raise HTTPException(status_code=404, detail="Ticket not found")

        ticket_info = ticket_data[0]
        print(f"Ticket info retrieved: {ticket_info}")
        
        original_payment_intent_id = ticket_info.get("paymentIntentId")
        
        if not original_payment_intent_id:
            print("No payment_intent_id found in ticket info")
            # return {"status": "error", "message": "Original payment intent ID not found for this ticket"}
        
        # 1. Process refund to seller using the original payment_intent_id
        print(f"Processing refund with payment_intent_id: {original_payment_intent_id}")
        refund_response = requests.post(
            f"{STRIPE_SERVICE_URL}/refund",
            json={
                "payment_intent_id": original_payment_intent_id,
                "amount": request.amount
                # "reason": "ticket_transfer"
            }
        )
        
        print(f"Refund response: {refund_response.status_code} - {refund_response.text}")
        
        if refund_response.status_code != 200:
            return {"status": "error", "message": f"Refund failed: {refund_response.text}"}
        
        # 2. Update ticket ownership
        transfer_response = requests.patch(
            f"{TICKET_INVENTORY_URL}/tickets/transfer",
            json={
                "ticket_id": int(request.ticket_id),
                "current_user_id": int(request.seller_id),
                "new_user_id": int(request.buyer_id),
                "payment_intent_id": request.new_payment_intent
            }
        )
        
        if transfer_response.status_code != 200:
            return {"status": "error", "message": f"Ticket transfer failed: {transfer_response.text}"}
        
        # 3. Send notifications directly using RabbitMQ
        # Fetch event details
        event_resp = requests.get(f"{EVENTS_URL}/events/{request.event_id}")
        event_resp.raise_for_status()
        event = event_resp.json().get("EventAPI", {})

        # Fetch full user profiles
        user_ids = [int(request.buyer_id), int(request.seller_id)]
        users_resp = requests.post(f"{AUTH_URL}/users/query", json={"ids": user_ids})
        users_resp.raise_for_status()
        users = {u["id"]: u for u in users_resp.json()}

        formatted_date = datetime.fromisoformat(event.get("date").replace("Z", "+00:00")).strftime("%B %d, %Y at %I:%M %p")
        seat_number = ticket_info.get("seat_number", str(request.ticket_id))

        # Buyer notification
        buyer = users[int(request.buyer_id)]
        subject = f"Important Notice: Your Ticket #{request.ticket_id} Has Been Transferred"
        message = (
            f"Hello {buyer.get('full_name', '')},\n\n"
            f"We’re pleased to inform you that your transfer for ticket #{request.ticket_id} to '{event.get('title')}' "
            f"scheduled for {formatted_date} at {event.get('venue')} has been completed successfully. "
            f"Your new ticket ({seat_number}) is now in your account—no further action is needed.\n\n"
            f"If you have any questions, visit our Help Center at https://help.eventgo.com or reply to this email.\n\n"
            f"Thank you for choosing EventGo.\n\n"
            f"Sincerely,\nEventGo Customer Support"
        )
        publish_notification(schemas.TransferNotification(subject=subject, message=message, recipient_email_address=buyer["email"]))

        # Seller notification
        seller = users[int(request.seller_id)]
        subject = f"Important Notice: Your Ticket #{request.ticket_id} Has Been Transferred"
        message = (
            f"Hello {seller.get('full_name', '')},\n\n"
            f"This is to confirm that your ticket #{request.ticket_id} for '{event.get('title')}' scheduled for "
            f"{formatted_date} at {event.get('venue')} has been transferred successfully. A refund of SGD {request.amount * 0.01:.2f} "
            f"will be processed to your original payment method shortly.\n\n"
            f"If you have any questions, visit our Help Center at https://help.eventgo.com or reply to this email.\n\n"
            f"Thank you for using EventGo.\n\n"
            f"Sincerely,\nEventGo Customer Support"
        )
        publish_notification(schemas.TransferNotification(subject=subject, message=message, recipient_email_address=seller["email"]))

        # To buyer
        # buyer_notification = schemas.TransferNotification(
        #     subject="Ticket Transfer Successful",
        #     message=f"Your ticket transfer has been completed successfully! Ticket #{request.ticket_id} is now in your account.",
        #     recipient_email_address=request.buyer_email
        # )
        # publish_notification(buyer_notification)

        # # To seller
        # seller_notification = schemas.TransferNotification(
        #     subject="Ticket Transfer Completed",
        #     message=f"Your ticket #{request.ticket_id} has been successfully transferred. The refund will be processed shortly.",
        #     recipient_email_address=request.seller_email
        # )
        # publish_notification(seller_notification)
        
        return {"status": "success", "message": "Ticket transfer completed"}
    
    except Exception as e:

        logger.error("Error processing ticket transfer: %s", traceback.format_exc())

        print(f"Error processing ticket transfer: {str(e)}")
        return {"status": "error", "message": f"Error processing ticket transfer: {str(e)}"}
                    

# Executing Transfer upon Successful Payment

@app.get("/health", response_model=schemas.HealthResponse)
def get_health():
    return {"status": "healthy"} 