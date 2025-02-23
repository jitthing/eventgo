import subprocess
import time


def run_command(command):
    """Run a shell command and wait for it to complete."""
    process = subprocess.run(command, shell=True, check=True)
    return process


def main():
    try:
        print("Stopping and removing volumes...")
        run_command("docker compose down --volumes")

        print("Waiting 3 seconds...")
        time.sleep(3)

        print("Starting Docker containers with build...")
        run_command("docker compose up -d --build")

        print("Waiting 5 seconds...")
        time.sleep(5)

        print("Seeding data...")
        run_command("python seed_data.py")

        print("Process completed successfully.")
    except subprocess.CalledProcessError as e:
        print(f"Error: {e}")


if __name__ == "__main__":
    main()
