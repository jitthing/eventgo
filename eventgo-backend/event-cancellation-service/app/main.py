import os
import asyncio
import httpx
from collections import defaultdict
from fastapi import FastAPI
from pydantic import BaseModel
from app.rabbitmq import publish_notification
from fastapi.middleware.cors import CORSMiddleware
from datetime import datetime
app = FastAPI(title="Event Cancellation Service")

app.add_middleware(
    CORSMiddleware,
    allow_origins=["http://localhost:3000"],
    allow_credentials=True,
    allow_methods=["PATCH", "OPTIONS"],
    allow_headers=["*"],
)

AUTH_URL    = os.getenv("AUTH_API_URL", 'http://auth-service:8000')
EVENTS_URL  = os.getenv("EVENTS_API_URL", 'https://personal-vyyhsf3d.outsystemscloud.com/EventsOutsystem/rest/EventsAPI')
TICKETS_URL = os.getenv("TICKETS_URL", 'http://ticket-inventory:8080')
STRIPE_URL  = os.getenv("STRIPE_SERVICE_URL", 'http://stripe-service:8000')

class RefundOutcome(BaseModel):
    """Summary of refund results for a given payment intent."""
    user_id: int
    email: str
    refund_status: str
    refunded_amount: float
    ticket_ids: list[int]

@app.patch("/cancel-event/{event_id}")
async def cancel_event(event_id: int):
    """
    Cancels an event by:
    1. Marking the event cancelled in the Events service.
    2. Cancelling all associated tickets in the Tickets service.
    3. Grouping cancellations by payment_intent_id to issue a single refund per intent.
    4. Sending one notification per user with total refunded amount and ticket details.

    Returns a summary of refund outcomes.
    """
    async with httpx.AsyncClient() as client:
        # Cancel event
        event_cancel_resp = await client.patch(f"{EVENTS_URL}/events/{event_id}/cancel")
        event_cancel_resp.raise_for_status()
        event = event_cancel_resp.json()       
        
        # Cancel tickets
        resp = await client.patch(f"{TICKETS_URL}/tickets/{event_id}/cancel")
        resp.raise_for_status()
        cancellations = resp.json().get("cancellations", [])

        grouped = defaultdict(list)
        for rec in cancellations:
            pid = rec.get("payment_intent_id")
            if pid:
                grouped[str(pid)].append(rec)

        user_ids = list({r["user_id"] for recs in grouped.values() for r in recs})
        users_resp = await client.post(f"{AUTH_URL}/users/query", json={"ids": user_ids})
        users_resp.raise_for_status()
        users = {u["id"]: u for u in users_resp.json()}

        tasks = [process_group(client, pid, recs, users[recs[0]["user_id"]], event) for pid, recs in grouped.items()]
        results = await asyncio.gather(*tasks)
        return {"status": "completed", "results": [r.dict() for r in results]}

async def process_group(client: httpx.AsyncClient, payment_intent_id: str, records: list[dict], user: dict, event: dict) -> RefundOutcome:
    """
    Issues a refund for all tickets sharing the same payment_intent_id and sends a notification.

    Args:
        client: HTTP client
        payment_intent_id: Stripe payment intent ID
        records: List of ticket cancellation records
        user: User object
        event: Event details dict

    Returns:
        RefundOutcome with refund details.
    """
    total_amount = sum(rec.get("price", 0.0) for rec in records)
    refund_resp = await client.post(f"{STRIPE_URL}/refund", json={"payment_intent_id": payment_intent_id})

    try:
        refund_resp.raise_for_status()
        status = refund_resp.json().get("status", "unknown")
    except httpx.HTTPStatusError:
        status = f"error ({refund_resp.status_code})"

    ticket_ids = [rec["ticket_id"] for rec in records]
    seat_numbers = [rec.get("seat_number", str(rec.get("ticket_id"))) for rec in records]
    formatted_date = datetime.fromisoformat(event.get("date").replace("Z", "+00:00")).strftime("%B %d, %Y at %I:%M %p")
    subject = f"Important Notice: The Event '{event.get('title')}' Has Been Canceled"
    message = (
        f"Hello {user.get('full_name', '')},\n\n"
        f"We regret to inform you that '{event.get('title')}' scheduled for {formatted_date} at {event.get('venue')} has been canceled. "
        f"Your tickets {', '.join(seat_numbers)} have been refunded automatically—no action is needed. "
        f"Total refunded amount: ${total_amount:.2f}. Refund status: {status}.\n\n"
        f"If you have any questions, visit our Help Center at https://help.eventgo.com or reply to this email.\n\n"
        "We apologize for the inconvenience and appreciate your understanding.\n\n"
        "Sincerely,\nEventGo Customer Support"
    )

    await publish_notification({
        "subject": subject,
        "message": message,
        "recipientEmailAddress": user["email"],
    })

    return RefundOutcome(
        user_id=user["id"],
        email=user["email"],
        refund_status=status,
        refunded_amount=total_amount,
        ticket_ids=ticket_ids
    )
