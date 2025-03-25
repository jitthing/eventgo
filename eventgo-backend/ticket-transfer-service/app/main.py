from fastapi import FastAPI, HTTPException, Request
from fastapi.middleware.cors import CORSMiddleware
import os
import stripe
import requests
import json
import uuid
from . import schemas
from dotenv import load_dotenv
import pika
import json
from datetime import datetime

load_dotenv()

app = FastAPI(title="Ticket Transfer Service")

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
        
        ticket_info = ticket_info_response.json()
        
        # Get the ticket price from ticket inventory
        ticket_price = ticket_info.get("price")
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
            "redirect_url": request.redirect_url,
            "event_id": str(request.ticket_id),
            "metadata": {
                "transfer_id": transfer_id,
                "ticket_id": str(request.ticket_id),
                "seller_id": str(request.seller_id),
                "seller_email": request.seller_email,
                "buyer_email": request.buyer_email,
                "buyer_id": str(request.buyer_id),
                "amount_in_cents": amount_in_cents
                # "original_payment_intent": original_payment_intent
            }
        }

        # Call stripe service to create payment link
        create_link_endpoint = f"{STRIPE_SERVICE_URL}/create-payment-link"
        payment_link_response = requests.post(
            create_link_endpoint,
            json=payment_link_req,
            timeout=10
        )

        if payment_link_response.status_code != 200:
            raise HTTPException(
                status_code=payment_link_response.status_code,
                detail=f"Stripe service error: {payment_link_response.text}"
            )

        # Send notification to buyer
        response_data = payment_link_response.json()
        notification = schemas.TransferNotification(
            subject="Ticket Transfer Payment Link",
            message=f"You have received a ticket transfer payment link. Click here to complete the payment: {response_data['url']}",
            recipient_email_address=request.buyer_email
        )
        publish_notification(notification)

        # Construct the response with the transfer_id
        return {
            "transfer_id": transfer_id,
            "payment_link_id": response_data["payment_link_id"],
            "url": response_data["url"],
            "amount": response_data["amount"],
            "expires_at": response_data["expires_at"]
        }

    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))


@app.post("/transfer")
async def transfer(request: schemas.TicketTransferRequest):
    """
    What i need:
    old payment id
    new payment id
    ticket id
    seller id
    seller email
    buyer email
    buyer id
    amount
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
        
        ticket_info = ticket_info_response.json()
        print(f"Ticket info retrieved: {ticket_info}")
        
        original_payment_intent_id = ticket_info.get("payment_intent_id")
        
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
        # To buyer
        buyer_notification = schemas.TransferNotification(
            subject="Ticket Transfer Successful",
            message=f"Your ticket transfer has been completed successfully! Ticket #{request.ticket_id} is now in your account.",
            recipient_email_address=request.buyer_email
        )
        publish_notification(buyer_notification)

        # To seller
        seller_notification = schemas.TransferNotification(
            subject="Ticket Transfer Completed",
            message=f"Your ticket #{request.ticket_id} has been successfully transferred. The refund will be processed shortly.",
            recipient_email_address=request.seller_email
        )
        publish_notification(seller_notification)
        
        return {"status": "success", "message": "Ticket transfer completed"}
    
    except Exception as e:
        print(f"Error processing ticket transfer: {str(e)}")
        return {"status": "error", "message": f"Error processing ticket transfer: {str(e)}"}
                    

# Executing Transfer upon Successful Payment

@app.get("/health", response_model=schemas.HealthResponse)
def get_health():
    return {"status": "healthy", "stripe_configured": bool(stripe.api_key)} 