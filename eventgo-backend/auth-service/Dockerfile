FROM python:3.11-slim

WORKDIR /app

# Copy requirements first for caching efficiency
COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt

# Copy application files
COPY ./app ./app  

# Copy mock_data.py separately
COPY mock_data.py ./  

# Change CMD to run mock_data.py after the database is ready
CMD ["sh", "-c", "python mock_data.py && uvicorn app.main:app --host 0.0.0.0 --port 8000"]
