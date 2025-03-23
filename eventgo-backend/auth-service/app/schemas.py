from pydantic import BaseModel, EmailStr
from enum import Enum


class RoleEnum(str, Enum):
    user = "user"
    admin = "admin"

class UserBase(BaseModel):
    email: EmailStr
    full_name: str

class UserCreate(UserBase):
    password: str
    role: RoleEnum = RoleEnum.user


class UserResponse(UserBase):
    id: int
    is_active: bool
    role: RoleEnum

    class Config:
        from_attributes = True


class Token(BaseModel):
    access_token: str
    token_type: str


class TokenData(BaseModel):
    email: str | None = None


class TokenValidationRequest(BaseModel):
    token: str
