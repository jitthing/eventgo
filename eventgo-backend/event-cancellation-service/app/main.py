import os
import asyncio
import httpx
from collections import defaultdict
from fastapi import FastAPI
from pydantic import BaseModel
from app.rabbitmq import publish_notification
from fastapi.middleware.cors import CORSMiddleware

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
    """Result returned for each unique payment intent refund."""
    user_id: int
    email: str
    refund_status: str
    refunded_amount: float
    ticket_ids: list[int]

@app.patch("/cancel-event/{event_id}", summary="Cancel an event end-to-end")
async def cancel_event(event_id: int):
    """
    Cancels an event by:
      1. Marking the event cancelled in the Events service.
      2. Cancelling all associated tickets in the Tickets service.
      3. Grouping cancellations by payment_intent_id to issue a single refund per intent.
      4. Sending one notification per user with total refunded amount and ticket details.

    Returns a summary of refund outcomes for each unique payment intent.
    """
    async with httpx.AsyncClient() as client:
        await client.patch(f"{EVENTS_URL}/events/{event_id}/cancel")
        resp = await client.patch(f"{TICKETS_URL}/tickets/{event_id}/cancel")
        resp.raise_for_status()
        cancellations = resp.json().get("cancellations", [])

        # Group cancellation records by payment_intent_id
        grouped = defaultdict(list)
        for rec in cancellations:
            pid = rec.get("payment_intent_id")
            if pid:
                grouped[str(pid)].append(rec)

        # Fetch user details for all impacted users
        user_ids = list({r["user_id"] for recs in grouped.values() for r in recs})
        users_resp = await client.post(f"{AUTH_URL}/users/query", json={"ids": user_ids})
        users_resp.raise_for_status()
        users = {u["id"]: u for u in users_resp.json()}

        # Process each payment intent group concurrently
        tasks = [
            process_group(client, pid, recs, users[recs[0]["user_id"]], event_id)
            for pid, recs in grouped.items()
            if users.get(recs[0]["user_id"])
        ]

        results = await asyncio.gather(*tasks)
        return {"status": "completed", "results": [r.dict() for r in results]}

async def process_group(
    client: httpx.AsyncClient,
    payment_intent_id: str,
    records: list[dict],
    user: dict,
    event_id: int
) -> RefundOutcome:
    """
    Issues a single refund for all tickets sharing the same payment_intent_id.

    Args:
        client: httpx AsyncClient instance.
        payment_intent_id: Stripe payment intent to refund.
        records: List of ticket cancellation records (must include 'price', 'ticket_id').
        user: User object (must include 'email' and 'id').
        event_id: ID of the cancelled event.

    Returns:
        RefundOutcome containing user, refund status, total refunded amount, and ticket IDs.
    """
    total_amount = sum(rec.get("price", 0.0) for rec in records)
    refund_resp = await client.post(f"{STRIPE_URL}/refund", json={"payment_intent_id": payment_intent_id})

    try:
        refund_resp.raise_for_status()
        status = refund_resp.json().get("status", "unknown")
    except httpx.HTTPStatusError:
        status = f"error ({refund_resp.status_code})"

    ticket_ids = [rec["ticket_id"] for rec in records]
    message = (
        f"Your tickets {ticket_ids} for event {event_id} were cancelled. "
        f"Total refunded amount: ${total_amount:.2f}. Refund status: {status}"
    )
    # await publish_notification({
    #     "recipientEmail": user["email"],
    #     "recipientName": user.get("full_name", ""),
    #     "message": message
    # })
    await publish_notification({
        # "notificationId": str(uuid.uuid4()),
        # "timestamp": datetime.now().isoformat(),
        "message": (
            f"Dear {user.get('full_name', '').strip()},\n\n"
            f"{message}\n\n"
            "Best,\n"
            "Team EventGo"
        ),        
        "subject": "Event Cancellation Notice",
        "recipientEmailAddress": user["email"],
    })

    return RefundOutcome(
        user_id=user["id"],
        email=user["email"],
        refund_status=status,
        refunded_amount=total_amount,
        ticket_ids=ticket_ids
    )
