import os
import requests
from fastapi import Depends, HTTPException, status
from fastapi.security import OAuth2PasswordBearer

AUTH_SERVICE_URL = os.getenv("AUTH_SERVICE_URL", "http://auth-service:8000")
oauth2_scheme = OAuth2PasswordBearer(tokenUrl="login")


async def get_current_user(token: str = Depends(oauth2_scheme)):
    response = requests.post(
        f"{AUTH_SERVICE_URL}/validate-token", json={"token": token}
    )
    if response.status_code != 200:
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="Invalid credentials",
            headers={"WWW-Authenticate": "Bearer"},
        )
    return response.json()  # Return the decoded user info
