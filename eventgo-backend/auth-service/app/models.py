from sqlalchemy import Boolean, Column, Integer, String, Enum
from passlib.context import CryptContext
from .database import Base
import enum

pwd_context = CryptContext(schemes=["bcrypt"], deprecated="auto")

# Define a role Enum
class UserRole(enum.Enum):
    user = "user"
    admin = "admin"

class User(Base):
    __tablename__ = "users"

    id = Column(Integer, primary_key=True, index=True)
    email = Column(String, unique=True, index=True)
    hashed_password = Column(String)
    is_active = Column(Boolean, default=True)
    full_name = Column(String, nullable=False) 
    role = Column(Enum(UserRole), default=UserRole.user, nullable=False)

    @staticmethod
    def verify_password(plain_password: str, hashed_password: str):
        return pwd_context.verify(plain_password, hashed_password)

    @staticmethod
    def get_password_hash(password: str):
        return pwd_context.hash(password) 