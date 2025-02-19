from fastapi import FastAPI, Depends, HTTPException
from fastapi.middleware.cors import CORSMiddleware
from sqlalchemy.orm import Session
from typing import List
from . import models, schemas
from .database import engine, get_db, Base  # Import get_db directly from database
from datetime import datetime

app = FastAPI(title="Events Service", version="1.0.0")

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
    Base.metadata.create_all(bind=engine)


@app.on_event("startup")
async def add_sample_data():
    db = next(get_db())
    if db.query(models.Event).count() == 0:
        sample_events = [
            models.Event(
                title="Taylor Swift - The Eras Tour",
                description="Experience the music of all of Taylor's eras",
                date=datetime(2024, 3, 15),
                location="MetLife Stadium, NJ",
                category="Concerts",
                price=199.99,
                image_url="https://media.gettyimages.com/id/74075509/photo/portrait-of-a-rock-band.jpg?s=612x612&w=gi&k=20&c=B8NmhYd6UD7fNSBMGjj3QXQr_THkBfiB6EXH7Ot50VU=",
                venue="MetLife Stadium",
                capacity=82500,
                is_featured=True,
            ),
            models.Event(
                title="NBA Finals 2024",
                description="Watch the ultimate basketball showdown",
                date=datetime(2024, 6, 4),
                location="Madison Square Garden, NY",
                category="Sports",
                price=299.99,
                image_url="https://media-cldnry.s-nbcnews.com/image/upload/rockcms/2025-01/250104-LeBron-James-ch-0953-26ecee.jpg",
                venue="Madison Square Garden",
                capacity=20000,
                is_featured=True,
            ),
            models.Event(
                title="Hamilton",
                description="The story of America then, told by America now",
                date=datetime(2024, 4, 20),
                location="Richard Rodgers Theatre, NY",
                category="Theater",
                price=179.99,
                image_url="https://static.wikia.nocookie.net/marvelcinematicuniverse/images/0/02/Sam_Wilson_Infobox.jpg/revision/latest?cb=20250117164205",
                venue="Richard Rodgers Theatre",
                capacity=1400,
                is_featured=True,
            ),
        ]
        db.add_all(sample_events)
        db.commit()


@app.get("/events", response_model=List[schemas.EventResponse])
async def list_events(db: Session = Depends(get_db)):
    events = db.query(models.Event).all()
    return events


@app.get("/events/featured", response_model=List[schemas.EventResponse])
async def get_featured_events(db: Session = Depends(get_db)):
    events = db.query(models.Event).filter(models.Event.is_featured == True).all()
    return events


@app.get("/events/{event_id}", response_model=schemas.EventResponse)
async def get_event(event_id: int, db: Session = Depends(get_db)):
    event = db.query(models.Event).filter(models.Event.id == event_id).first()
    if event is None:
        raise HTTPException(status_code=404, detail="Event not found")
    return event


@app.post("/events", response_model=schemas.EventResponse)
async def create_event(event: schemas.EventCreate, db: Session = Depends(get_db)):
    db_event = models.Event(**event.dict())
    db.add(db_event)
    db.commit()
    db.refresh(db_event)
    return db_event


@app.get("/health")
async def health_check():
    try:
        # Check database connection
        db = next(get_db())
        db.execute("SELECT 1")
        return {"status": "healthy", "database": "connected"}
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))
