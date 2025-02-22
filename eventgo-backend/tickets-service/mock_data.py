import os
import time
from dotenv import load_dotenv
from sqlalchemy.orm import Session
from app.database import engine, get_db
from app import models
from sqlalchemy import inspect

# Load environment variables
load_dotenv()


def wait_for_events_table(db: Session):
    """Wait until the Events table is available before proceeding."""
    max_attempts = 10
    attempts = 0
    inspector = inspect(engine)

    while attempts < max_attempts:
        if "events" in inspector.get_table_names():
            print("✅ Events table is available, proceeding with ticket seeding...")
            return
        print(
            f"⏳ Waiting for Events table to be available... Attempt {attempts + 1}/{max_attempts}"
        )
        time.sleep(5)
        attempts += 1

    raise Exception("❌ Timeout: Events table is still not available after waiting.")


def seed_tickets(db: Session):
    """Create tickets for seats, ensuring events exist first."""
    wait_for_events_table(db)  # ✅ Ensure events table exists before inserting tickets

    if db.query(models.Ticket).count() == 0:
        seats = db.query(models.Seat).all()
        users = db.query(models.User).all()

        tickets = []
        for seat in seats:
            status = models.TicketStatus.AVAILABLE
            owner_id = None

            # Randomly mark some seats as RESERVED or SOLD
            if int(seat.seat_number.split("-")[-1]) % 10 == 0:
                status = models.TicketStatus.SOLD
                owner_id = users[0].id if users else None  # Assign a user if available
            elif int(seat.seat_number.split("-")[-1]) % 7 == 0:
                status = models.TicketStatus.RESERVED
                owner_id = users[1].id if users else None

            ticket = models.Ticket(
                event_id=seat.event_id, seat_id=seat.id, price=100.00, status=status
            )
            tickets.append(ticket)

        db.add_all(tickets)
        db.commit()
        print("✅ Tickets seeded successfully!")


if __name__ == "__main__":
    db = next(get_db())
    seed_tickets(db)
    db.close()
