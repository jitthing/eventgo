from .database import get_db

# Re-export get_db
# This allows us to change the implementation later without changing the imports
get_db = get_db 