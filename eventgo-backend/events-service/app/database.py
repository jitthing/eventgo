from sqlalchemy import create_engine
from sqlalchemy.ext.declarative import declarative_base
from sqlalchemy.orm import sessionmaker
import os
import time

SQLALCHEMY_DATABASE_URL = os.getenv("DATABASE_URL")
MAX_RETRIES = 10
RETRY_DELAY = 5


def get_db_connection():
    for attempt in range(1, MAX_RETRIES + 1):
        try:
            engine = create_engine(
                SQLALCHEMY_DATABASE_URL, pool_pre_ping=True, pool_recycle=300
            )
            conn = engine.connect()
            conn.close()
            return engine
        except Exception as e:
            if attempt < MAX_RETRIES:
                print(
                    f"[Events-Service] DB connection attempt {attempt} failed: {e}. "
                    f"Retrying in {RETRY_DELAY} seconds..."
                )
                time.sleep(RETRY_DELAY)
            else:
                raise Exception(
                    "[Events-Service] Database connection failed after multiple attempts."
                ) from e


engine = get_db_connection()

SessionLocal = sessionmaker(autocommit=False, autoflush=False, bind=engine)
Base = declarative_base()


def get_db():
    db = SessionLocal()
    try:
        yield db
    finally:
        db.close()
