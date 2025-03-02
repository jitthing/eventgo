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

        print("Waiting 2 seconds...")
        time.sleep(2)

        print("Starting Docker containers with build...")
        run_command("docker compose up -d --build")

        print("Waiting 2 seconds...")
        time.sleep(2)

        print("Seeding data...")
        try:
            run_command("python seed_data.py")
        except subprocess.CalledProcessError: # I added this because for those who must use python3 instead of python as a command
            print("Seeding data failed. Running fallback command...")
            run_command("python3 seed_data.py")

        print("Process completed successfully.")
    except subprocess.CalledProcessError as e:
        print(f"Error: {e}")


if __name__ == "__main__":
    main()
