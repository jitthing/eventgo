from datetime import datetime, timedelta
from typing import Set

# Store invalidated tokens in memory (use Redis in production)
TOKEN_BLACKLIST: Set[str] = set()


def add_to_blacklist(token: str):
    """Add a token to the blacklist."""
    TOKEN_BLACKLIST.add(token)


def is_token_blacklisted(token: str) -> bool:
    """Check if a token is blacklisted."""
    return token in TOKEN_BLACKLIST
