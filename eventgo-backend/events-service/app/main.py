from fastapi import FastAPI, Depends, HTTPException
from fastapi.middleware.cors import CORSMiddleware
from sqlalchemy.orm import Session
from typing import List
from datetime import datetime
from . import models, schemas
from .database import engine, get_db, Base
from sqlalchemy.sql import text

app = FastAPI(title="Events Service")

# Add CORS middleware
app.add_middleware(
    CORSMiddleware,
    allow_origins=["http://localhost:3000"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)


@app.on_event("startup")
async def startup():
    """Create database tables on application startup."""
    Base.metadata.create_all(bind=engine)


# 🩺 Health check
@app.get("/health")
async def health_check(db: Session = Depends(get_db)):
    try:
        db.execute(text("SELECT 1"))
        return {"status": "healthy", "database": "connected"}
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))


# 🎟 Get all events (without seats)
@app.get("/events", response_model=List[schemas.EventResponse])
async def list_events(db: Session = Depends(get_db)):
    events = db.query(models.Event).all()
    return events


# 🎟 Get event details (with seats)
@app.get("/events/{event_id}", response_model=schemas.EventResponse)
async def get_event(event_id: int, db: Session = Depends(get_db)):
    """Retrieve event details including available seats."""
    event = db.query(models.Event).filter(models.Event.id == event_id).first()
    if event is None:
        raise HTTPException(status_code=404, detail="Event not found")

    return event


# ✅ 1️⃣ Create an event (Automatically generates seats)
@app.post("/events", response_model=schemas.EventResponse)
async def create_event(event: schemas.EventCreate, db: Session = Depends(get_db)):
    """Create a new event and automatically generate seats."""
    db_event = models.Event(**event.dict())
    db.add(db_event)
    db.commit()
    db.refresh(db_event)

    # Generate seats
    seats = []
    for i in range(1, event.capacity + 1):
        seat_number = f"{event.venue[:3].upper()}-{i}"  # Unique seat ID per venue
        seat = models.Seat(
            event_id=db_event.id, seat_number=seat_number, category="Standard"
        )
        seats.append(seat)

    db.add_all(seats)
    db.commit()

    return db_event
