from fastapi import FastAPI, Depends, HTTPException, status
from fastapi.middleware.cors import CORSMiddleware
from fastapi.security import OAuth2PasswordRequestForm
from sqlalchemy.orm import Session
from . import models, schemas
from .database import engine, get_db
from jose import JWTError, jwt
from .dependencies import SECRET_KEY, ALGORITHM  # if you store them here
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


@app.post("/register", response_model=schemas.UserResponse)
async def register_user(user: schemas.UserCreate, db: Session = Depends(get_db)):
    db_user = db.query(models.User).filter(models.User.email == user.email).first()
    if db_user:
        raise HTTPException(status_code=400, detail="Email already registered")

    hashed_password = models.User.get_password_hash(user.password)
    db_user = models.User(email=user.email, hashed_password=hashed_password)
    db.add(db_user)
    db.commit()
    db.refresh(db_user)
    return db_user


@app.post("/login", response_model=schemas.Token)
async def login(
    form_data: OAuth2PasswordRequestForm = Depends(), db: Session = Depends(get_db)
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
    return {"access_token": access_token, "token_type": "bearer"}


@app.get("/me", response_model=schemas.UserResponse)
async def read_users_me(current_user: models.User = Depends(get_current_user)):
    return current_user


@app.post("/logout")
async def logout(token: str):
    """Invalidate a JWT token."""
    add_to_blacklist(token)
    return {"message": "Successfully logged out"}


@app.post("/validate-token")
async def validate_token(token: str, db: Session = Depends(get_db)):
    """Validate token & check if blacklisted"""

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
