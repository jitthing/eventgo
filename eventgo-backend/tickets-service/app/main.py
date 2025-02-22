from fastapi import FastAPI, Depends, HTTPException, status
from fastapi.middleware.cors import CORSMiddleware
from sqlalchemy.orm import Session
from typing import List
from . import models, schemas
from .database import engine, get_db
from .dependencies import get_current_user
from sqlalchemy.sql import text

app = FastAPI(title="Tickets Service")

# Add CORS middleware
app.add_middleware(
    CORSMiddleware,
    allow_origins=["http://localhost:3000"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)


# 🛠 Initialize database on startup
@app.on_event("startup")
async def startup():
    models.Base.metadata.create_all(bind=engine)


# 🩺 Health check
@app.get("/health")
async def health_check(db: Session = Depends(get_db)):
    try:
        db.execute(text("SELECT 1"))
        return {"status": "healthy", "database": "connected"}
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))


# 🎭 Get available seats for an event
@app.get("/events/{event_id}/seats", response_model=List[schemas.SeatResponse])
async def get_available_seats(event_id: int, db: Session = Depends(get_db)):
    """Retrieve all available seats for an event."""
    seats = (
        db.query(models.Seat)
        .filter(models.Seat.ticket_id == None, models.Seat.event_id == event_id)
        .all()
    )
    return seats


# ✅ 1️⃣ User selects seats → Reserve tickets
@app.post("/tickets/reserve")
async def reserve_tickets(
    seat_ids: List[int],
    db: Session = Depends(get_db),
    current_user: dict = Depends(get_current_user),
):
    """Reserve multiple tickets based on seat selection."""

    seats = db.query(models.Seat).filter(models.Seat.id.in_(seat_ids)).all()

    # Validate all selected seats
    if len(seats) != len(seat_ids):
        raise HTTPException(
            status_code=400, detail="One or more selected seats are invalid."
        )

    for seat in seats:
        if seat.ticket_id is not None:
            raise HTTPException(
                status_code=400, detail=f"Seat {seat.seat_number} is already taken."
            )

    # Create tickets for the selected seats
    tickets = []
    for seat in seats:
        ticket = models.Ticket(
            event_id=seat.event_id,
            price=50.0,
            status=models.TicketStatus.RESERVED,
            seat_id=seat.id,
        )
        db.add(ticket)
        db.commit()
        db.refresh(ticket)

        # Link ticket to seat
        seat.ticket_id = ticket.id
        db.commit()
        tickets.append(ticket)

    return {
        "message": "Tickets reserved successfully. Proceed to payment.",
        "tickets": [t.id for t in tickets],
    }


# ✅ 2️⃣ User completes purchase → Tickets marked as SOLD
@app.post("/tickets/purchase")
async def purchase_tickets(
    ticket_ids: List[int],
    db: Session = Depends(get_db),
    current_user: dict = Depends(get_current_user),
):
    """Confirm payment for multiple reserved tickets and mark them as SOLD."""

    tickets = db.query(models.Ticket).filter(models.Ticket.id.in_(ticket_ids)).all()

    # Validate all tickets
    if len(tickets) != len(ticket_ids):
        raise HTTPException(
            status_code=400, detail="One or more ticket IDs are invalid."
        )

    for ticket in tickets:
        if ticket.status != models.TicketStatus.RESERVED:
            raise HTTPException(
                status_code=400, detail=f"Ticket {ticket.id} is not reserved."
            )

    # Mark all tickets as SOLD
    for ticket in tickets:
        ticket.status = models.TicketStatus.SOLD
    db.commit()

    return {"message": "Payment confirmed. Tickets are now SOLD."}
