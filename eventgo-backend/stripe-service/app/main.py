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
    currency: str = "usd"
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

@app.get("/heath")
def get_health():
    return "Stripe Endpoint reached"
# @app.post("/webhook")
# async def stripe_webhook(request: Request):
#     payload = await request.body()
#     sig_header = request.headers.get("stripe-signature")
    
#     try:
#         event = stripe.Webhook.construct_event(
#             payload, sig_header, os.environ.get("STRIPE_WEBHOOK_SECRET")
#         )
        
#         # Handle the event
#         if event.type == "payment_intent.succeeded":
#             payment_intent = event.data.object
#             # Update your database to mark payment as complete
#             # You could call your tickets service here
#             print(f"Payment for {payment_intent.metadata.event_id} succeeded!")
            
#         return {"status": "success"}
#     except Exception as e:
#         raise HTTPException(status_code=400, detail=str(e))