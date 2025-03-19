from fastapi import FastAPI, Depends, HTTPException, Response, status, Request
from fastapi.middleware.cors import CORSMiddleware
from fastapi.security import OAuth2PasswordRequestForm
from sqlalchemy.orm import Session
from . import models, schemas
from .database import engine, get_db
from .schemas import TokenValidationRequest

from jose import JWTError, jwt
from .dependencies import (
    get_db,
    create_access_token,
    get_current_user,
    SECRET_KEY,
    ALGORITHM,
)
from . import models, schemas
from .database import engine
from .token_blacklist import is_token_blacklisted, add_to_blacklist

app = FastAPI(title="Auth Service")

# Add CORS middleware
app.add_middleware(
    CORSMiddleware,
    allow_origins=["http://localhost:3000"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

# Initialize database
models.Base.metadata.create_all(bind=engine)


from sqlalchemy.sql import text  # Import text for raw SQL execution


@app.get("/health")
async def health_check(db: Session = Depends(get_db)):
    """Health check endpoint to verify service and database connectivity."""
    try:
        db.execute(text("SELECT 1"))  # Wrap SQL query with text()
        return {"status": "healthy", "database": "connected"}
    except Exception as e:
        import traceback

        error_details = traceback.format_exc()  # Get full error traceback
        print(f"❌ Health check failed: {error_details}")  # Print error in logs
        raise HTTPException(status_code=500, detail=str(e))


@app.post("/register")
async def register_user(user: schemas.UserCreate, response: Response, db: Session = Depends(get_db)):
    db_user = db.query(models.User).filter(models.User.email == user.email).first()
    if db_user:
        raise HTTPException(status_code=400, detail="Email already registered")

    # Hash the password and create the user
    hashed_password = models.User.get_password_hash(user.password)
    db_user = models.User(email=user.email, hashed_password=hashed_password)
    db.add(db_user)
    db.commit()
    db.refresh(db_user)

    # ✅ Generate JWT token
    access_token = create_access_token(data={"sub": db_user.email})

    # ✅ Set token as HTTP-only cookie
    response.set_cookie(
        key="access_token",
        value=f"Bearer {access_token}",
        httponly=True,
        samesite="lax",  # Use "none" with "secure=True" for production
        secure=False,  # Use secure=True in production with HTTPS
        path="/"
    )

    return {"message": "User registered successfully", "access_token": access_token, "token_type": "bearer"}



@app.post("/login", response_model=schemas.Token)
async def login(
    response: Response,  # ✅ Include response to set cookies
    form_data: OAuth2PasswordRequestForm = Depends(),
    db: Session = Depends(get_db),
):
    user = db.query(models.User).filter(models.User.email == form_data.username).first()
    if not user or not models.User.verify_password(
        form_data.password, user.hashed_password
    ):
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="Incorrect email or password",
            headers={"WWW-Authenticate": "Bearer"},
        )

    access_token = create_access_token(data={"sub": user.email})

    # ✅ Set the token in an httpOnly cookie
    response.set_cookie(
        key="access_token",
        value=f"Bearer {access_token}",
        httponly=True,  # ✅ Prevents JS access
        # secure=True,  # ✅ Necessary to comment out as we're developing locally with http without https
        # samesite="Strict",
        samesite="lax",  # i changed this to lax, i think "none" required secure to be True
        secure=False,
        path="/"
    )

    return {"access_token": access_token, "token_type": "bearer"}


# @app.get("/me", response_model=schemas.UserResponse)
# async def read_users_me(current_user: models.User = Depends(get_current_user)):
#     return current_user

@app.get("/me")
async def read_users_me(current_user: models.User = Depends(get_current_user)):
    return {"id": current_user.id, "email": current_user.email}


@app.get("/search-users")
async def search_users(email: str, db: Session = Depends(get_db)):
    """
    Search users by email. Supports partial match.
    Example usage: /search-users?email=test
    """
    if not email:
        raise HTTPException(status_code=400, detail="Email parameter is required")

    users = db.query(models.User).filter(models.User.email.ilike(f"%{email}%")).limit(10).all()

    if not users:
        return {"message": "No users found"}

    return [{"id": user.id, "email": user.email} for user in users]

@app.post("/logout")
async def logout(response: Response, request: Request):
    """Invalidate a JWT token and remove the cookie."""
    token = request.cookies.get("access_token")
    if token is not None:
        # Optionally remove the "Bearer " prefix if present
        if token.startswith("Bearer "):
            token = token[len("Bearer ") :]
        add_to_blacklist(token)

    # Remove the cookie so the browser no longer sends it
    response.delete_cookie(key="access_token")

    return {"message": "Successfully logged out"}


@app.post("/validate-token")
async def validate_token(
    token_data: TokenValidationRequest, db: Session = Depends(get_db)
):
    token = token_data.token
    credentials_exception = HTTPException(
        status_code=status.HTTP_401_UNAUTHORIZED,
        detail="Invalid token",
        headers={"WWW-Authenticate": "Bearer"},
    )
    if is_token_blacklisted(token):
        raise credentials_exception

    try:
        payload = jwt.decode(token, SECRET_KEY, algorithms=[ALGORITHM])
        email: str = payload.get("sub")
        if email is None:
            raise credentials_exception
    except JWTError:
        raise credentials_exception

    user = db.query(models.User).filter(models.User.email == email).first()
    if user is None:
        raise credentials_exception

    return {"email": user.email, "id": user.id}

@app.get("/debug-cookies")
async def debug_cookies(request: Request):
    return {"cookies": request.cookies}