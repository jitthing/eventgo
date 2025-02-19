from sqlalchemy import create_engine
from sqlalchemy.ext.declarative import declarative_base
from sqlalchemy.orm import sessionmaker
import os
import time

SQLALCHEMY_DATABASE_URL = os.getenv("DATABASE_URL")
MAX_RETRIES = 5
RETRY_DELAY = 5

def get_db_connection():
    for attempt in range(MAX_RETRIES):
        try:
            engine = create_engine(
                SQLALCHEMY_DATABASE_URL,
                pool_pre_ping=True,  # Add connection testing
                pool_recycle=300     # Recycle connections every 5 minutes
            )
            engine.connect()
            return engine
        except Exception as e:
            if attempt < MAX_RETRIES - 1:
                print(f"Database connection attempt {attempt + 1} failed. Retrying in {RETRY_DELAY} seconds...")
                time.sleep(RETRY_DELAY)
            else:
                raise e

engine = get_db_connection()
SessionLocal = sessionmaker(autocommit=False, autoflush=False, bind=engine)
Base = declarative_base()

def get_db():
    db = SessionLocal()
    try:
        yield db
    finally:
        db.close() 