from fastapi.testclient import TestClient
import pytest
from unittest.mock import patch, MagicMock
import stripe
import os
import sys
import json

# Add the parent directory to the path so we can import app
sys.path.append(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))

from app.main import app

client = TestClient(app)

def test_health_check():
    """Test the health check endpoint."""
    response = client.get("/health")
    assert response.status_code == 200
    assert response.json()["status"] == "healthy"
    assert isinstance(response.json()["stripe_configured"], bool)

@patch('stripe.PaymentIntent.create')
def test_create_payment_intent(mock_create):
    """Test creating a payment intent."""
    # Mock the Stripe payment intent creation
    mock_create.return_value = MagicMock(client_secret="test_client_secret")
    
    # Test data
    payload = {
        "amount": 2000,
        "currency": "sgd",
        "event_id": "event_123",
        "seats": ["A1", "A2"]
    }
    
    response = client.post("/create-payment-intent", json=payload)
    
    # Assertions
    assert response.status_code == 201
    assert response.json() == {"clientSecret": "test_client_secret"}
    mock_create.assert_called_once_with(
        amount=2000,
        currency="sgd",
        metadata={"event_id": "event_123", "seats": "A1,A2"}
    )

@patch('stripe.PaymentIntent.retrieve')
def test_payment_status(mock_retrieve):
    """Test retrieving payment status."""
    # Mock the Stripe payment intent retrieval
    mock_retrieve.return_value = MagicMock(
        status="succeeded",
        amount=2000,
        currency="sgd",
        metadata={"event_id": "event_123", "seats": "A1,A2"}
    )
    
    response = client.get("/payment-status/pi_123")
    
    # Assertions
    assert response.status_code == 200
    assert response.json()["status"] == "succeeded"
    assert response.json()["amount"] == 2000
    assert response.json()["currency"] == "sgd"
    assert response.json()["metadata"]["event_id"] == "event_123"
    assert response.json()["metadata"]["seats"] == "A1,A2"

@patch('stripe.PaymentIntent.retrieve')
def test_validate_payment_success(mock_retrieve):
    """Test validating a successful payment."""
    # Mock the Stripe payment intent retrieval
    mock_retrieve.return_value = MagicMock(
        status="succeeded",
        metadata={"event_id": "event_123", "seats": "A1,A2"}
    )
    
    payload = {
        "payment_intent_id": "pi_123",
        "event_id": "event_123",
        "seats": ["A1", "A2"]
    }
    
    response = client.post("/validate-payment", json=payload)
    
    # Assertions
    assert response.status_code == 200
    assert response.json()["valid"] is True
    assert response.json()["payment_status"] == "succeeded"

@patch('stripe.Refund.create')
def test_refund(mock_create):
    """Test processing a refund."""
    # Mock the Stripe refund creation
    mock_create.return_value = MagicMock(
        id="re_123",
        amount=1000,
        status="succeeded"
    )
    
    payload = {
        "payment_intent_id": "pi_123",
        "amount": 1000,
        "reason": "customer_requested"
    }
    
    response = client.post("/refund", json=payload)
    
    # Assertions
    assert response.status_code == 200
