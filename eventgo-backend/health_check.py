import requests

# Define the services and their health check endpoints
SERVICES = {
    "auth-service": "http://localhost:8001/health",
    "events-service": "http://localhost:8002/health",
    "tickets-service": "http://localhost:8003/health",
}


def check_service(service_name, url):
    """Check if a service is running and print the result."""
    try:
        response = requests.get(url, timeout=5)
        if response.status_code == 200:
            print(f"✅ {service_name} is UP! Response: {response.json()}")
        else:
            print(f"❌ {service_name} is DOWN! Status Code: {response.status_code}")
    except requests.exceptions.RequestException as e:
        print(f"❌ {service_name} is DOWN! Error: {e}")


def main():
    """Run health checks for all services."""
    print("🔍 Running health checks...\n")
    for service, url in SERVICES.items():
        check_service(service, url)


if __name__ == "__main__":
    main()
