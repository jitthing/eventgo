from fastapi import FastAPI, Depends, HTTPException
from fastapi.middleware.cors import CORSMiddleware
from sqlalchemy.orm import Session
from typing import List
from . import models, schemas
from .database import engine, get_db
from datetime import datetime

app = FastAPI(title="Tickets Service")

# Add CORS middleware
app.add_middleware(
    CORSMiddleware,
    allow_origins=["http://localhost:3000"],  # Add your frontend URL
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

# Initialize database
models.Base.metadata.create_all(bind=engine)

@app.get("/tickets", response_model=List[schemas.TicketResponse])
async def list_tickets(db: Session = Depends(get_db)):
    tickets = db.query(models.Ticket).all()
    return tickets

@app.get("/tickets/{ticket_id}", response_model=schemas.TicketResponse)
async def get_ticket(ticket_id: int, db: Session = Depends(get_db)):
    ticket = db.query(models.Ticket).filter(models.Ticket.id == ticket_id).first()
    if ticket is None:
        raise HTTPException(status_code=404, detail="Ticket not found")
    return ticket

@app.post("/tickets", response_model=schemas.TicketResponse)
async def create_ticket(ticket: schemas.TicketCreate, db: Session = Depends(get_db)):
    db_ticket = models.Ticket(**ticket.dict())
    db.add(db_ticket)
    db.commit()
    db.refresh(db_ticket)
    return db_ticket

@app.post("/tickets/{ticket_id}/purchase")
async def purchase_ticket(ticket_id: int, db: Session = Depends(get_db)):
    ticket = db.query(models.Ticket).filter(models.Ticket.id == ticket_id).first()
    if ticket is None:
        raise HTTPException(status_code=404, detail="Ticket not found")
    if ticket.status != models.TicketStatus.AVAILABLE:
        raise HTTPException(status_code=400, detail="Ticket is not available")
    
    ticket.status = models.TicketStatus.SOLD
    db.commit()
    return {"message": "Ticket purchased successfully"} 