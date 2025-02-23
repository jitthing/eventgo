docker compose down --volumes

docker compose up -d --build

python seed_data.py

Folder Structure:

```
│   docker-compose.yml
│   health_check.py
│   readme.md
│   restart_docker.py
│   seed_data.py
│
├───auth-service
│   │   .env
│   │   .env.example
│   │   Dockerfile
│   │   requirements.txt
│   │
│   └───app
│           database.py
│           dependencies.py
│           init_db.py
│           main.py
│           models.py
│           schemas.py
│           token_blacklist.py
│           __init__.py
│
├───events-service
│   │   .env
│   │   .env.example
│   │   Dockerfile
│   │   requirements.txt
│   │
│   └───app
│           database.py
│           dependencies.py
│           main.py
│           models.py
│           schemas.py
│           __init__.py
│
└───tickets-service
    │   .env
    │   .env.example
    │   Dockerfile
    │   requirements.txt
    │
    └───app
            database.py
            dependencies.py
            main.py
            models.py
            schemas.py
            __init__.py
```
