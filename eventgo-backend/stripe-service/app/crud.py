from sqlalchemy.orm import Session
from . import models, schemas

def create_payment_intent(db: Session, payment_intent: schemas.PaymentIntentDB):
    db_payment_intent = models.PaymentIntent(
        stripe_payment_id=payment_intent.stripe_payment_id,
        client_secret=payment_intent.client_secret,
        amount=payment_intent.amount,
        currency=payment_intent.currency,
        status=payment_intent.status,
        event_id=payment_intent.event_id,
        seats=",".join(payment_intent.seats) if payment_intent.seats else ""
    )
    db.add(db_payment_intent)
    db.commit()
    db.refresh(db_payment_intent)
    return db_payment_intent

def get_payment_intent(db: Session, stripe_payment_id: str):
    return db.query(models.PaymentIntent).filter(models.PaymentIntent.stripe_payment_id == stripe_payment_id).first()

def update_payment_intent_status(db: Session, stripe_payment_id: str, status: str):
    db_payment_intent = get_payment_intent(db, stripe_payment_id)
    if db_payment_intent:
        db_payment_intent.status = status
        db.commit()
        db.refresh(db_payment_intent)
    return db_payment_intent

def create_refund(db: Session, refund: schemas.RefundDB):
    payment_intent = get_payment_intent(db, refund.payment_intent_id)
    if not payment_intent:
        return None
        
    db_refund = models.Refund(
        stripe_refund_id=refund.stripe_refund_id,
        payment_intent_id=payment_intent.id,
        amount=refund.amount,
        status=refund.status,
        reason=refund.reason
    )
    db.add(db_refund)
    db.commit()
    db.refresh(db_refund)
    return db_refund

def get_refund(db: Session, stripe_refund_id: str):
    return db.query(models.Refund).filter(models.Refund.stripe_refund_id == stripe_refund_id).first()
