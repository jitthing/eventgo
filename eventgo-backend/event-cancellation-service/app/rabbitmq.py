import os, json
import pika
from pika.exceptions import AMQPConnectionError

RABBIT_HOST     = os.getenv("RABBITMQ_HOST", "rabbitmq")
RABBIT_PORT     = int(os.getenv("RABBITMQ_PORT", 5672))
RABBIT_USER     = os.getenv("RABBITMQ_DEFAULT_USER", "rabbitmqusername")
RABBIT_PASS     = os.getenv("RABBITMQ_DEFAULT_PASS", "rabbitmqpassword")
QUEUE           = "notification.queue"

credentials = pika.PlainCredentials(RABBIT_USER, RABBIT_PASS)
params = pika.ConnectionParameters(host=RABBIT_HOST, port=RABBIT_PORT, credentials=credentials)

def _get_channel():
    try:
        conn = pika.BlockingConnection(params)
        ch = conn.channel()
        ch.queue_declare(queue=QUEUE, durable=True)
        return conn, ch
    except AMQPConnectionError as e:
        raise RuntimeError("Cannot connect to RabbitMQ") from e

async def publish_notification(event: dict):
    conn, ch = _get_channel()
    ch.basic_publish(
        exchange="",
        routing_key=QUEUE,
        body=json.dumps(event),
        properties=pika.BasicProperties(content_type="application/json", delivery_mode=2),
    )
    conn.close()
