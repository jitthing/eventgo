# EventGo 🎟️

Ever feel tired of ticketmasters downtime and poor queue management? Fear not, because with EventGo, you will never have to worry about your seat being stolen by another user.

EventGo's Key Features:

- Ticket Transfer 🎫
  - Never get scammed again! Transfer your tickets worry free between users on EventGo, where we manage the payments for you. Tickets are only transferred when the buyer pays successfully!
- Split Payment ↔️
  - Buying for your friends? EventGo offers seamless split payment methods, so you never have to worry if you have enough money to cover the whole group!
- Managed Event Cancellation ❌
  - Remeber when Justin Bieber cancelled his tour.. EventGo maanges these events fuss free, ensuring that ticket holders are kept in the loop and promptly refunded.

# Setup

## Stripe

To set up Stripe with this application, you will have to set up the Stripe webhook listener. Run the following commands in your terminal

```bash
stripe login
stripe listen --forward-to localhost:8010/webhook
```

Make sure to take note of the webhook secret given by Stripe

## Backend

1. Copy the content of `eventgo-backend/.env.example` and paste into `eventgo-backend/.env`

```bash
cd eventgo-backend
cp .env.example .env
```

2. Replace Stripe Secret Key and Webhook Key with your own

```bash
STRIPE_SECRET_KEY=sk_test_...
STRIPE_WEBHOOK_SECRET=whsec_...
```

3. Seed the data and start the containers. This has been managed via a script

```python
python restart_docker.py OR
python3 restart_docker.py
```

## Frontend

1. Install dependencies

```
cd eventgo-frontend
npm i
```

2. Populate `.env.local`

Copy the content of `eventgo-frontend/.env.example` and rename it to `eventgo-frontend/.env.local`.

```bash
cp .env.example .env.local
```

3. Replace the Stripe Public Key with your own:

```bash
NEXT_PUBLIC_STRIPE_PUBLISHABLE_KEY=********
```

## Running Frontend

```
npm run dev
```
