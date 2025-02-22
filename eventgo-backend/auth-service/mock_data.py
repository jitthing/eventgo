import os
from dotenv import load_dotenv
from sqlalchemy.orm import Session
from app.database import get_db, engine
from app import models

# Load environment variables
load_dotenv()

USERS = [
    {"email": "user1@example.com", "password": "password123"},
    {"email": "user2@example.com", "password": "securepass456"},
    {"email": "user3@example.com", "password": "mysecretpass789"},
]


def seed_users(db: Session):
    """Populate the database with mock users."""

    if db.query(models.User).count() == 0:
        for user_data in USERS:
            user = models.User(
                email=user_data["email"],
                hashed_password=models.User.get_password_hash(user_data["password"]),
            )
            db.add(user)
        db.commit()
        print("✅ Users inserted.")


if __name__ == "__main__":
    models.Base.metadata.create_all(bind=engine)  # Ensure tables exist first
    db = next(get_db())
    seed_users(db)
    db.close()
