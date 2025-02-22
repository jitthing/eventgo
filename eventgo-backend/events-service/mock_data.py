import os
from dotenv import load_dotenv
from sqlalchemy.orm import Session
from app.database import get_db, engine
from app import models
from datetime import datetime, timedelta
import random

# Load environment variables
load_dotenv()

EVENTS = [
    {
        "title": "NBA Finals Game 1",
        "description": "Experience the NBA Finals live!",
        "date": datetime.utcnow() + timedelta(days=10),
        "location": "Madison Square Garden",
        "category": "Sports",
        "price": 200.0,
        "image_url": "https://media1.s-nbcnews.com/i/rockcms/2025-01/351270/250104-LeBron-James-ch-0953-26ecee_e08d57a40aaa073423abe1ed6d5e5988584a1a1e.jpg",
        "venue": "Madison Square Garden",
        "capacity": random.randint(30, 50),
        "is_featured": True,
    },
    {
        "title": "Coldplay: Music of the Spheres Tour",
        "description": "Coldplay live in concert!",
        "date": datetime.utcnow() + timedelta(days=20),
        "location": "Singapore National Stadium",
        "category": "Concert",
        "price": 120.0,
        "image_url": "https://media1.s-nbcnews.com/i/rockcms/2025-01/351270/250104-LeBron-James-ch-0953-26ecee_e08d57a40aaa073423abe1ed6d5e5988584a1a1e.jpg",
        "venue": "National Stadium",
        "capacity": random.randint(30, 50),
        "is_featured": True,
    },
    {
        "title": "TEDx Talks: Future of AI",
        "description": "Deep dive into AI and future technology.",
        "date": datetime.utcnow() + timedelta(days=30),
        "location": "MIT Media Lab",
        "category": "Conference",
        "price": 50.0,
        "image_url": "https://media1.s-nbcnews.com/i/rockcms/2025-01/351270/250104-LeBron-James-ch-0953-26ecee_e08d57a40aaa073423abe1ed6d5e5988584a1a1e.jpg",
        "venue": "MIT Auditorium",
        "capacity": random.randint(30, 50),
        "is_featured": True,
    },
    {
        "title": "F1 Singapore Grand Prix",
        "description": "Feel the thrill of F1 racing!",
        "date": datetime.utcnow() + timedelta(days=40),
        "location": "Marina Bay Circuit",
        "category": "Sports",
        "price": 300.0,
        "image_url": "https://media1.s-nbcnews.com/i/rockcms/2025-01/351270/250104-LeBron-James-ch-0953-26ecee_e08d57a40aaa073423abe1ed6d5e5988584a1a1e.jpg",
        "venue": "Marina Bay Circuit",
        "capacity": random.randint(30, 50),
        "is_featured": False,
    },
    {
        "title": "Anime Expo 2025",
        "description": "The biggest anime event of the year!",
        "date": datetime.utcnow() + timedelta(days=50),
        "location": "Suntec Convention Centre",
        "category": "Exhibition",
        "price": 80.0,
        "image_url": "https://media1.s-nbcnews.com/i/rockcms/2025-01/351270/250104-LeBron-James-ch-0953-26ecee_e08d57a40aaa073423abe1ed6d5e5988584a1a1e.jpg",
        "venue": "Suntec Convention Centre",
        "capacity": random.randint(30, 50),
        "is_featured": True,
    },
]


def seed_events(db: Session):
    """Populate the database with events and seats."""

    if db.query(models.Event).count() == 0:
        for event_data in EVENTS:
            event = models.Event(**event_data)
            db.add(event)
            db.commit()
            db.refresh(event)

            seats = []
            for i in range(1, event.capacity + 1):
                seat_number = f"{event.venue[:3].upper()}-{i}"
                seat = models.Seat(
                    event_id=event.id, seat_number=seat_number, category="Standard"
                )
                seats.append(seat)

            db.add_all(seats)
            db.commit()

        print("✅ Events & seats inserted.")


if __name__ == "__main__":
    models.Base.metadata.create_all(bind=engine)  # Ensure tables exist first
    db = next(get_db())
    seed_events(db)
    db.close()
