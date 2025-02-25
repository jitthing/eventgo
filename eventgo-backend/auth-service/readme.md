# Auth Service for EventGo

## Overview

The **Auth Service** is responsible for user authentication and authorization in the EventGo platform. It provides secure user registration, login, token-based authentication, and session management using **FastAPI**, **JWT tokens**, and **SQLAlchemy**.

## Features

-   **User Registration**: Users can create an account with a hashed password.
-   **User Authentication**: Secure login with password hashing and JWT tokens.
-   **Token-Based Authorization**: Access tokens are stored in HTTP-only cookies.
-   **Token Blacklisting**: Ensures tokens cannot be reused after logout. (this is to trigger logout)
-   **Database Connectivity**: Utilizes PostgreSQL with SQLAlchemy ORM.
-   **Health Check Endpoint**: Monitors database connectivity.
-   **CORS Support**: Configured for frontend communication.

## Tech Stack

-   **Language**: Python 3.11
-   **Framework**: FastAPI
-   **Database**: PostgreSQL (via SQLAlchemy)
-   **Authentication**: JWT (via `python-jose`)
-   **Security**: Bcrypt for password hashing
-   **Containerization**: Docker
    (You can see Docker compose in `eventgo-backend` to see how things are spinned up. Can ask GPT to summarize `main.py` also.)

---

## Directory Structure

(TDLR; main logic is in `main.py` and the rest are supporting scripts. Can go `localhost:8001/docs` to see the endpoints.)

```
eventgo-backend/
│── auth-service/
│   │── app/
│   │   │── __init__.py            # Initialize the package
│   │   │── main.py                 # Main FastAPI application
│   │   │── database.py             # Database connection and session management
│   │   │── dependencies.py         # Auth utilities including token validation
│   │   │── init_db.py              # Initializes database tables
│   │   │── models.py               # SQLAlchemy ORM models
│   │   │── schemas.py              # Pydantic schemas for API validation
│   │   │── token_blacklist.py      # In-memory blacklist for JWT tokens
│   │── Dockerfile                  # Dockerfile for containerizing the service
│   │── requirements.txt            # Dependencies for the service
│   │──.env                         # Environment variables
```

---

## Installation

### Prerequisites

Ensure you have the following installed:

-   **Docker** (recommended) OR
-   **Python 3.11+** and **PostgreSQL**

### Environment Setup

Create a `.env` file in the root directory of the `eventgo-backend` project:

```ini
DATABASE_URL=postgresql://user:password@localhost:5432/auth_db
JWT_SECRET_KEY=your_secret_key
ACCESS_TOKEN_EXPIRE_MINUTES=30
```

### Running with Docker

1. Build and run the container:
    ```sh
    docker build -t auth-service .
    docker run -p 8000:8000 --env-file .env auth-service
    ```

---

## API Endpoints

### **Health Check**

-   **`GET /health`** → Check service status

### **Authentication & Users**

-   **`POST /register`** → Register a new user
-   **`POST /login`** → Authenticate user & return token (stored in cookie)
-   **`GET /me`** → Retrieve authenticated user details
-   **`POST /logout`** → Logout user and invalidate token
-   **`POST /validate-token`** → Validate JWT token

---

## Database Models

The service defines the following model in `models.py`:

### **User Model**

```python
class User(Base):
    __tablename__ = "users"
    id = Column(Integer, primary_key=True, index=True)
    email = Column(String, unique=True, index=True)
    hashed_password = Column(String)
    is_active = Column(Boolean, default=True)
```

---

### Running All Services

You can do `python restart_docker.py` in the `eventgo-backend` folder (just remember to rename the `.env.example` into `.env` for every services, i.e. go to auth folder, copy and then rename the `.env.example` to `.env`)

### Main Problem I'm Facing

-   Unauthorized 401 error

    -   basically, everytime i try to login/register, I get a glimpse of profile page before being redirected back to login page.
    -   this is because i'm not authorized to access profile page
    -   this means thrs probably some logic error causing issues for the JWT (authentication token) to be passed between frontend and backend.
    -   dw to lead you the wrong way, but when i tried `curl` command, my auth endpoint all seems to work okay... so might be the connection between backend and frontend(?)

-   The main logic for auth in the frontend is in `eventgo-frontend/lib/auth.ts` and the respective pages: `eventgo-frontend/app/login/page.tsx`, `eventgo-frontend/app/register/page.tsx`, `eventgo-frontend/app/profile/page.tsx`
