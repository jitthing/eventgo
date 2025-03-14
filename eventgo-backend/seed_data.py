#!/usr/bin/env python3
"""
seed_data.py

A standalone script to:
1. Wait for each microservice (Auth, Events, Tickets) to be healthy.
2. Seed mock data into each service via their REST API endpoints.

- Auth-Service: creates 3 users
- Events-Service: creates 6 events (30–50 seats each)
- Tickets-Service: for each event, randomly reserve some seats and purchase a subset.

Run this AFTER doing `docker compose up -d --build`.
Then: `python seed_data.py`
"""

import time
import random
import requests
from datetime import datetime, timedelta

# ---------------------------------------------------------------------------
# Configuration: URLs of your services
# ---------------------------------------------------------------------------
AUTH_SERVICE_URL = "http://localhost:8001"
EVENTS_SERVICE_URL = "http://localhost:8002"
TICKETS_SERVICE_URL = "http://localhost:8003"
STRIPE_SERVICE_URL = "http://localhost:8004"
TICKET_INVENTORY_URL = "http://localhost:8005"


# ---------------------------------------------------------------------------
# 1) Wait for each service to report healthy
# ---------------------------------------------------------------------------
def wait_for_health(service_name: str, base_url: str, timeout_seconds=60):
    """
    Poll the /health endpoint until it's 200 or we time out.
    Raises an Exception if not healthy in time.
    """
    start = time.time()
    while True:
        try:
            resp = requests.get(f"{base_url}/health", timeout=3)
            if resp.status_code == 200:
                print(f"[{service_name}] is healthy!")
                return
        except requests.exceptions.RequestException:
            pass  # service not up yet

        elapsed = time.time() - start
        if elapsed > timeout_seconds:
            raise TimeoutError(
                f"[{service_name}] did not become healthy within {timeout_seconds}s."
            )

        print(
            f"Waiting for [{service_name}] to be healthy... ({int(elapsed)}s elapsed)"
        )
        time.sleep(3)


# ---------------------------------------------------------------------------
# 2) Create users in the Auth service
# ---------------------------------------------------------------------------
def seed_users():
    """
    Creates 3 users in Auth-Service via POST /register.
    If a user already exists, ignore the error.
    """
    users = [
        {"email": "user1@example.com", "password": "password123"},
        {"email": "user2@example.com", "password": "securepass456"},
        {"email": "user3@example.com", "password": "mysecretpass789"},
    ]

    for u in users:
        try:
            resp = requests.post(f"{AUTH_SERVICE_URL}/register", json=u, timeout=5)
            if resp.status_code == 200:
                print(f"Created user: {u['email']}")
            elif resp.status_code == 400 and "already registered" in resp.text:
                print(f"User {u['email']} already registered. Skipping.")
            else:
                resp.raise_for_status()
        except Exception as e:
            print(f"Failed to create user {u['email']}: {e}")


# ---------------------------------------------------------------------------
# 3) Log in a user to get a JWT token
# ---------------------------------------------------------------------------
def login_user(email: str, password: str) -> str:
    """
    Logs in a user via /login (Auth Service).
    Returns the access token string if successful.
    """
    try:
        # Auth uses OAuth2PasswordRequestForm => data={}, not json={}
        resp = requests.post(
            f"{AUTH_SERVICE_URL}/login",
            data={"username": email, "password": password},
            timeout=5,
        )
        resp.raise_for_status()
        data = resp.json()
        token = data["access_token"]
        print(
            f"User {email} logged in successfully, got token: {token[:10]}... (truncated)"
        )
        return token
    except Exception as e:
        print(f"Failed to log in user {email}: {e}")
        return ""


# ---------------------------------------------------------------------------
# 4) Create events in the Events service
# ---------------------------------------------------------------------------
def seed_events(num_events=6) -> list:
    """
    Creates `num_events` new events in the Events-Service (POST /events).
    Returns a list of the created event IDs.
    """
    # We'll define 6 sample events, each with an image_url set as you requested:
    SAMPLE_EVENTS = [
        {
            "title": "NBA Finals Game",
            "description": "Experience the NBA Finals live!",
            "location": "Madison Square Garden",
            "category": "Sports",
            "price": 200.0,
            "venue": "Madison Square Garden",
            "is_featured": True,
            "image_url": "https://media1.s-nbcnews.com/i/rockcms/2025-01/351270/250104-LeBron-James-ch-0953-26ecee_e08d57a40aaa073423abe1ed6d5e5988584a1a1e.jpg",
            "capacity": 50,
            "date": "2025-03-13T12:34:56.789123"
        },
        {
            "title": "Coldplay: Music of the Spheres Tour",
            "description": "Coldplay live in concert!",
            "location": "Singapore National Stadium",
            "category": "Concert",
            "price": 120.0,
            "venue": "National Stadium",
            "is_featured": True,
            "image_url": "https://media1.s-nbcnews.com/i/rockcms/2025-01/351270/250104-LeBron-James-ch-0953-26ecee_e08d57a40aaa073423abe1ed6d5e5988584a1a1e.jpg",
            "capacity": 55,
            "date": "2025-03-13T12:34:56.789123"
        },
        {
            "title": "TEDx Talks: Future of AI",
            "description": "Deep dive into AI and future tech.",
            "location": "MIT Media Lab",
            "category": "Conference",
            "price": 50.0,
            "venue": "MIT Auditorium",
            "is_featured": True,
            "image_url": "https://media1.s-nbcnews.com/i/rockcms/2025-01/351270/250104-LeBron-James-ch-0953-26ecee_e08d57a40aaa073423abe1ed6d5e5988584a1a1e.jpg",
            "capacity": 60,
            "date": "2025-03-13T12:34:56.789123"
        },
        {
            "title": "F1 Singapore Grand Prix",
            "description": "Feel the thrill of F1 racing under the lights!",
            "location": "Marina Bay Circuit",
            "category": "Sports",
            "price": 300.0,
            "venue": "Marina Bay Circuit",
            "is_featured": False,
            "image_url": "https://media1.s-nbcnews.com/i/rockcms/2025-01/351270/250104-LeBron-James-ch-0953-26ecee_e08d57a40aaa073423abe1ed6d5e5988584a1a1e.jpg",
            "capacity": 150,
            "date": "2025-03-13T12:34:56.789123"
        },
        {
            "title": "Anime Expo 2025",
            "description": "The biggest anime event of the year!",
            "location": "Suntec Convention Centre",
            "category": "Exhibition",
            "price": 80.0,
            "venue": "Suntec Convention Centre",
            "is_featured": True,
            "image_url": "https://media1.s-nbcnews.com/i/rockcms/2025-01/351270/250104-LeBron-James-ch-0953-26ecee_e08d57a40aaa073423abe1ed6d5e5988584a1a1e.jpg",
            "capacity": 50,
            "date": "2025-03-13T12:34:56.789123"
        },
        {
            "title": "Techno Music Festival",
            "description": "All-night techno rave party",
            "location": "Berlin Arena",
            "category": "Concert",
            "price": 90.0,
            "venue": "Berlin Arena",
            "is_featured": False,
            "image_url": "https://media1.s-nbcnews.com/i/rockcms/2025-01/351270/250104-LeBron-James-ch-0953-26ecee_e08d57a40aaa073423abe1ed6d5e5988584a1a1e.jpg",
            "capacity": 20,
            "date": "2025-03-13T12:34:56.789123"

        },
    ]

    created_ids = []
    # We'll create exactly `num_events` events, cycling through SAMPLE_EVENTS
    for i in range(num_events):
        base_event = SAMPLE_EVENTS[i % len(SAMPLE_EVENTS)].copy()

       

        try:
            resp = requests.post(
                f"{EVENTS_SERVICE_URL}/events", json=base_event
            )
            resp.raise_for_status()
            data = resp.json()
            event_id = data["event_id"]
            created_ids.append(event_id)
            print(
                f"[Events] Created event ID {event_id} "
                f"({base_event['title']}, capacity={base_event['capacity']})"
            )
        except Exception as err:
            print(f"Failed to create event: {err}")

    return created_ids


# ---------------------------------------------------------------------------
# 5) Reserve and purchase seats for realism in the Tickets service
# ---------------------------------------------------------------------------
def reserve_and_purchase_some_seats(
    event_id: int, token: str, reserve_count: int = 5, purchase_count: int = 2
):
    """
    - Fetch available seats from events-service via its /events/{event_id} endpoint.
    - Randomly pick `reserve_count` seats to reserve.
    - Randomly pick `purchase_count` from those to finalize purchase.
    """
    try:
        # Query the events service directly
        resp = requests.get(f"{EVENTS_SERVICE_URL}/events/{event_id}", timeout=5)
        resp.raise_for_status()
        event_data = resp.json()

        # 🔍 Debugging log
        #print(f"[DEBUG] Event Data for {event_id}: {event_data}")

        available_seats = event_data.get("seats", [])
        # 🔍 Debugging log for seats
        #print(f"[DEBUG] Available seats for event {event_id}: {available_seats}")
    except Exception as e:
        print(f"[Tickets] Failed to get seats for event_id={event_id}: {e}")
        return

    if not available_seats:
        print(f"[Tickets] No available seats for event {event_id}. Skipping.")
        return

    # 5b) Randomly pick seats to reserve
    if reserve_count > len(available_seats):
        reserve_count = len(available_seats)

    seats_to_reserve = random.sample(available_seats, reserve_count)
    seat_ids = [s["id"] for s in seats_to_reserve]

    headers = {"Authorization": f"Bearer {token}"}

    try:
        resp = requests.post(
            f"{TICKETS_SERVICE_URL}/tickets/reserve",
            json=seat_ids,
            headers=headers,
            timeout=5,
        )
        resp.raise_for_status()
        data = resp.json()
        reserved_ticket_ids = data["tickets"]  # list of newly created ticket IDs
        print(
            f"[Tickets] Reserved {len(reserved_ticket_ids)} seats for event {event_id}."
        )
    except Exception as e:
        print(f"[Tickets] Failed to reserve seats for event {event_id}: {e}")
        return

    # 5c) Now purchase a subset of those tickets
    if purchase_count > len(reserved_ticket_ids):
        purchase_count = len(reserved_ticket_ids)

    tickets_to_purchase = random.sample(reserved_ticket_ids, purchase_count)
    try:
        resp = requests.post(
            f"{TICKETS_SERVICE_URL}/tickets/purchase",
            json=tickets_to_purchase,
            headers=headers,
            timeout=5,
        )
        resp.raise_for_status()
        print(
            f"[Tickets] Purchased {purchase_count} of those reserved seats for event {event_id}."
        )
    except Exception as e:
        print(f"[Tickets] Failed to purchase seats for event {event_id}: {e}")

# ---------------------------------------------------------------------------
# 6) Populate the ticket inventory
# ---------------------------------------------------------------------------
def generate_seats(event_id, capacity):
    """
    Generates a list of seats dynamically based on event capacity.
    
    - First 20% seats → VIP
    - Remaining seats → Standard
    """
    seats = []
    vip_threshold = int(capacity * 0.2)  # First 20% seats are VIP

    for i in range(1, capacity + 1):
        seat_category = "VIP" if i <= vip_threshold else "standard"
        seat_number = f"{chr(65 + (i - 1) // 10)}{(i - 1) % 10 + 1}"  # Example: A1, A2, ..., B1, B2

        seats.append({
            "ticketId": i + (event_id * 100),  # Unique ticket ID based on event
            "seatNumber": seat_number,
            "category": seat_category,
            "status": "available"
        })
    
    return seats


def seed_inventory():
    """
    Dynamically seeds ticket inventory for multiple events based on capacity.
    """
    events_with_capacity = [
        {"eventId": 1, "capacity": 50},
        {"eventId": 2, "capacity": 55},
        {"eventId": 3, "capacity": 60},
        {"eventId": 4, "capacity": 150},
        {"eventId": 5, "capacity": 50},
        {"eventId": 6, "capacity": 20}
    ]

    # Generate events dynamically
    payload = {
        "events": [],
        "categoryPrices": [
            {"category": "VIP", "price": 100.00},
            {"category": "standard", "price": 50.00}
        ]
    }

    for event in events_with_capacity:
        event_id = event["eventId"]
        capacity = event["capacity"]
        
        payload["events"].append({
            "eventId": event_id,
            "seats": generate_seats(event_id, capacity)
        })

    try:
        response = requests.post(f"{TICKET_INVENTORY_URL}/inventory/create", json=payload, timeout=10)
        response.raise_for_status()
        print("✅ Inventory successfully populated with dynamic events and seats!")
    except requests.exceptions.RequestException as e:
        print(f"❌ Failed to populate inventory: {e}")



# ---------------------------------------------------------------------------
# MAIN SCRIPT
# ---------------------------------------------------------------------------
def main():
    # 1) Wait for all services: Auth, Events, Tickets
    print("=== 1) Checking health of all microservices ===")
    wait_for_health("Auth-Service", AUTH_SERVICE_URL)
    wait_for_health("Events-Service", EVENTS_SERVICE_URL)
    # wait_for_health("Tickets-Service", TICKETS_SERVICE_URL)
    wait_for_health("Ticket-Inventory", TICKET_INVENTORY_URL)


    # 2) Create 3 users in Auth
    print("\n=== 2) Creating mock users in Auth-Service ===")
    seed_users()

    # 3) Log in user1@example.com to get a token (we'll use it to simulate seat reservations)
    print("\n=== 3) Logging in user1@example.com ===")
    user_token = login_user("user1@example.com", "password123")
    if not user_token:
        print("Could not get token for user1. Aborting seat reservations.")
        return

    # 4) Create 6 events in Events
    print("\n=== 4) Creating 6 mock events in Events-Service ===")
    event_ids = seed_events(num_events=6)

    # 5) For each event, randomly reserve/purchase some seats (tickets-service)
    print("\n=== 5) Reserving & purchasing seats in Tickets-Service ===")
    for eid in event_ids:
        reserve_and_purchase_some_seats(
            eid, user_token, reserve_count=5, purchase_count=2
        )

    # 6) Populate the ticket inventory
    print("\n=== 6) Populating the inventory database ===")
    seed_inventory()


    print("\n✅ Seeding complete! Check your DBs and service logs to verify.")


if __name__ == "__main__":
    main()
