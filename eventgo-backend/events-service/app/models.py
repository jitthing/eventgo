from sqlalchemy import Column, Integer, String, DateTime, Float, Boolean
from .database import Base

class Event(Base):
    __tablename__ = "events"

    id = Column(Integer, primary_key=True, index=True)
    title = Column(String, index=True)
    description = Column(String)
    date = Column(DateTime)
    location = Column(String)
    category = Column(String)
    price = Column(Float)
    image_url = Column(String)
    venue = Column(String)
    capacity = Column(Integer)
    is_featured = Column(Boolean, default=False) 