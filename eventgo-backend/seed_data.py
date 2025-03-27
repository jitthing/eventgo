#!/usr/bin/env python3
"""
seed_data.py

This script:
1. Waits for the Event, Ticket, and Auth services to be healthy.
2. Creates predefined events.
3. Creates users and logs in to get an auth token.
4. Generates tickets for each event, linking them correctly.
5. Randomly marks some tickets as 'sold' to mimic real-world behavior.

Run `docker compose up -d --build` first, then execute:
`python seed_data.py`
"""

import time
import random
import requests

# ---------------------------------------------------------------------------
# Configuration: API URLs for services
# ---------------------------------------------------------------------------
AUTH_SERVICE_URL = "http://localhost:8001"
EVENTS_SERVICE_URL = "https://personal-vyyhsf3d.outsystemscloud.com/EventsOutsystem/rest/EventsAPI"
TICKET_SERVICE_URL = "http://localhost:8005"

# ---------------------------------------------------------------------------
# 1) Wait for each service to report healthy
# ---------------------------------------------------------------------------
def wait_for_health(service_name: str, base_url: str, timeout_seconds=60):
    """
    Polls the /health endpoint until the service responds with HTTP 200.
    Raises an Exception if the service is not healthy within the timeout.
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
        {"email": "admin@eventgo.com", "password": "admin", "full_name": "Warren Buffet", "role": "admin"},
        {"email": "user1@example.com", "password": "password", "full_name": "Jack Neo", "role": "user"},
        {"email": "user2@example.com", "password": "password", "full_name": "Lebron James", "role": "user"},
        {"email": "user3@example.com", "password": "password", "full_name": "Mark Lee", "role": "user"},
        {"email": "lyejiajun99@gmail.com", "password": "password", "full_name": "Jia Jun", "role": "user"},
        {"email": "jiajun.lye.2023@scis.smu.edu.sg", "password": "password", "full_name": "SMU JJ", "role": "user"},
    ]

    for u in users:
        try:
            resp = requests.post(f"{AUTH_SERVICE_URL}/register", json=u, timeout=5)
            if resp.status_code == 200:
                print(f"✅ Created user: {u['email']} (Role: {u['role']})")
            elif resp.status_code == 400 and "already registered" in resp.text:
                print(f"⚠️ User {u['email']} already registered. Skipping.")
            else:
                resp.raise_for_status()
        except Exception as e:
            print(f"❌ Failed to create user {u['email']}: {e}")


# ---------------------------------------------------------------------------
# 3) Log in a user to get a JWT token
# ---------------------------------------------------------------------------
def login_user(email: str, password: str) -> str:
    """
    Logs in a user via /login (Auth Service).
    Returns the access token string if successful.
    """
    try:
        resp = requests.post(
            f"{AUTH_SERVICE_URL}/login",
            data={"username": email, "password": password},
            timeout=5,
        )
        resp.raise_for_status()
        data = resp.json()
        token = data["access_token"]
        role = data.get("role", "unknown")  # NEW: Fetch role from response

        print(
            f"✅ User {email} logged in successfully (Role: {role}), got token: {token[:10]}... (truncated)"
        )
        return token
    except Exception as e:
        print(f"❌ Failed to log in user {email}: {e}")
        return ""


# ---------------------------------------------------------------------------
# 4) Create predefined events in the Event service
# ---------------------------------------------------------------------------

def clear_events():
    """
    Calls the /clear endpoint to remove all existing events before re-seeding.
    """
    try:
        resp = requests.post(f"{EVENTS_SERVICE_URL}/clear")
        resp.raise_for_status()
        print("Successfully cleared all events!")
    except Exception as e:
        print(f"Failed to clear events: {e}")



def seed_events() -> list:
    """
    Creates events in the Events-Service using the exact hardcoded SAMPLE_EVENTS list.
    Returns a list of created event IDs.
    """
    SAMPLE_EVENTS = [
        {
            "title": "NBA Finals Game",
            "description": "Experience the NBA Finals live!",
            "category": "Sports",
            "venue": "Madison Square Garden",
            "is_featured": True,
            "image_url": "https://media1.s-nbcnews.com/i/rockcms/2025-01/351270/250104-LeBron-James-ch-0953-26ecee_e08d57a40aaa073423abe1ed6d5e5988584a1a1e.jpg",
            "date": "2025-09-20T13:00:00.000000",
            "status": "Upcoming"
        },
        {
            "title": "Coldplay: Music of the Spheres Tour",
            "description": "Coldplay live in concert!",
            "category": "Concert",
            "venue": "Singapore National Stadium",
            "is_featured": True,
            "image_url": "https://tmw.com.sg/wp-content/uploads/2023/07/Coldplay-concert-in-Singapore-all-you-need-to-know.webp",
            "date": "2025-09-24T19:00:00.000000",
            "status": "Upcoming"
            
        },
        {
            "title": "TEDx Talks: Future of AI",
            "description": "Deep dive into AI and future tech.",
            "category": "Conference",
            "venue": "MIT Auditorium",
            "is_featured": True,
            "image_url": "https://talkstar-photos.s3.amazonaws.com/uploads/5d36796c-8a8d-4832-9b27-5456113e06f4/JimVandehei_2021X-stageshot.jpg",
            "date": "2025-09-26T18:00:00.000000",
            "status": "Upcoming"
        },
        {
            "title": "Formula 1 Grand Prix: Singapore Night Race",
            "description": "Experience the thrill of Formula 1 under the Marina Bay lights.",
            "category": "Sports",
            "venue": "Marina Bay Street Circuit",
            "is_featured": True,
            "image_url": "https://singaporegp.sg/media/2022/press-release/2022-first-wave-of-sgp-entertainment-line-up-walkabout-tickets.png",
            "date": "2025-09-26T18:00:00.000000",
            "status": "Upcoming"
        },
        {
            "title": "Anime Expo 2025",
            "description": "The biggest anime event of the year!",
            "category": "Exhibition",
            "venue": "Suntec Convention Centre",
            "is_featured": True,
            "image_url": "https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcTzFe5vbnckSdpy1qr3o459Btd4b2mqP9xjCg&s",
            "date": "2025-09-26T12:00:00.000000",
            "status": "Upcoming"
        },
        {
            "title": "Techno Music Festival",
            "description": "All-night techno rave party",
            "category": "Concert",
            "venue": "Berlin Arena",
            "is_featured": False,
            "image_url": "https://images.ra.co/4f7482518a367caf2a12ceb719e7da4497663022.jpg",
            "date": "2025-11-11T23:00:00.000000",
            "status": "Upcoming"

        },
    ]

    created_event_ids = []

    # *** First clear existing events ***
    clear_events()
    
    for event in SAMPLE_EVENTS:
        try:
            resp = requests.post(f"{EVENTS_SERVICE_URL}/events/", json=event)
            resp.raise_for_status()
            data = resp.json()
            print('my data is', data)
            event_id = data.get("event_id")
            created_event_ids.append(event_id)
            print(f"[Events] Created event ID {event_id} ({event['title']})")
        except Exception as err:
            print(f"Failed to create event {event['title']}: {err}")

    return created_event_ids

# ---------------------------------------------------------------------------
# 5) Create tickets for each event
# ---------------------------------------------------------------------------
def generate_tickets(event_id, min_seats=10, max_seats=30):
    """
    Creates 10-30 tickets per event with random 'sold' statuses.
    Assigns randomized but consistent prices per event.
    """
    seat_count = random.randint(min_seats, max_seats)
    seats = []
    vip_threshold = int(seat_count * 0.2)
    
    # Randomly decide prices per category for this event
    standard_price = random.randint(100, 300) // 10 * 10
    # standardize the price because UI doesn't show difference between vip and standard, just to make it consistent for now.
    vip_price = standard_price
    # vip_price = random.randint(500, 2000) // 10 * 10
    
    print(f"[Tickets] Generating {seat_count} tickets for Event ID {event_id} with VIP: ${vip_price}, Standard: ${standard_price}...")

    for i in range(1, seat_count + 1):
        seat_category = "VIP" if i <= vip_threshold else "standard"
        seat_number = f"{chr(65 + (i - 1) // 10)}{(i - 1) % 10 + 1}"
        ticket_status = "available" if random.random() > 0.3 else "sold"

        ticket_data = {
            "ticketId": i + (event_id * 100),
            "seatNumber": seat_number,
            "category": seat_category,
            "status": ticket_status,
            "price": vip_price if seat_category == "VIP" else standard_price
        }

        # Assign a random user_id (either 2, 3, or 4) to sold tickets
        if ticket_status == "sold":
            ticket_data["user_id"] = random.choice([2, 3, 4])
        
        seats.append(ticket_data)

    print(f"[Tickets] Successfully generated {seat_count} tickets for Event ID {event_id}.")
    return seats, vip_price, standard_price

def seed_tickets(event_ids):
    """
    Creates ticket inventory for each event with randomized prices.
    Sends one request per event instead of batching all at once.
    """
    
    print("\n=== Seeding Tickets for Events One by One ===")
    
    for event_id in event_ids:
        seats, vip_price, standard_price = generate_tickets(event_id)

        payload = {
            "events": [{
                "eventId": event_id,
                "seats": seats
            }],
            "categoryPrices": [
                {
                    "category": "VIP",
                    "price": vip_price
                },
                {
                    "category": "standard",
                    "price": standard_price
                }
            ]
        }

        print(f"[Tickets] Sending ticket data for Event ID {event_id}...")
        try:
            response = requests.post(f"{TICKET_SERVICE_URL}/tickets/create", json=payload, timeout=10)
            response.raise_for_status()
            print(f"✅ [Tickets] Tickets successfully created for Event ID {event_id}!")
        except requests.exceptions.RequestException as e:
            print(f"❌ [Tickets] Failed to create tickets for Event ID {event_id}: {e}")

# ---------------------------------------------------------------------------
# MAIN SCRIPT
# ---------------------------------------------------------------------------
def main():
    print("=== 1) Checking service health ===")
    wait_for_health("Auth-Service", AUTH_SERVICE_URL)
    # wait_for_health("Events-Service", EVENTS_SERVICE_URL)
    wait_for_health("Ticket-Service", TICKET_SERVICE_URL)

    print("\n=== 2) Creating Users ===")
    seed_users()

    # print("\n=== 3) Logging in User ===")
    # user_token = login_user("user1@example.com", "password123")

    print("\n=== 3) Creating Events ===")
    event_ids = seed_events()

    print("\n=== 4) Creating Tickets for Events ===")
    seed_tickets(event_ids)

    print("\n✅ Seeding complete!")

if __name__ == "__main__":
    main()
