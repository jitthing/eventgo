"use client";

import { useState, useEffect, FormEvent, JSX } from "react";
import { useSearchParams, useRouter } from "next/navigation";
import Link from "next/link";
import BackButton from "@/components/BackButton";
import { Elements } from "@stripe/react-stripe-js";
import { loadStripe, StripeElementsOptions } from "@stripe/stripe-js";
import { PaymentForm } from "@/components/PaymentForm";

// Make sure to call loadStripe outside component rendering
const stripePromise = loadStripe(
  process.env.NEXT_PUBLIC_STRIPE_PUBLISHABLE_KEY as string
);
async function createPaymentIntent(
  eventId: string,
  seats: string,
  total: string,
  setClientSecret: Function,
  setError: Function
): Promise<void> {
  try {
    if (!eventId || !seats || !total) {
      throw new Error("Missing required payment parameters");
    }

    const response = await fetch(
      `${
        process.env.NEXT_PUBLIC_PAYMENTS_API_URL || "http://localhost:8004"
      }/create-payment-intent`,
      {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({
          amount: Math.round(parseFloat(total) * 100), // Convert to cents and ensure it's an integer
          event_id: eventId,
          seats: seats.split(","),
        }),
      }
    );

    if (!response.ok) {
      const errorData = await response.json();
      throw new Error(errorData.detail || "Payment initialization failed");
    }

    const data = await response.json();
    setClientSecret(data.clientSecret);
  } catch (err) {
    console.error("Error creating payment intent:", err);
    setError(
      err instanceof Error
        ? err.message
        : "Failed to initialize payment. Please try again."
    );
  }
}

export default function PaymentPage(): JSX.Element {
  const searchParams = useSearchParams();
  const router = useRouter();

  const eventId = searchParams.get("eventId");
  const seats = searchParams.get("seats");
  const total = searchParams.get("total");
  const [success, setSuccess] = useState<boolean>(false);

  // Handle successful payment
  const handlePaymentSuccess = (): void => {
    setSuccess(true);
    // Redirect to confirmation page with details
    setTimeout(() => {
      if (eventId && seats && total) {
        router.push(
          `/confirmation?eventId=${eventId}&seats=${seats}&total=${total}`
        );
      } else {
        router.push("/events");
      }
    }, 1500);
  };

  useEffect(() => {
    if (!eventId || !seats || !total) {
      router.push("/checkout"); // Redirect if missing params
    }
  }, [eventId, seats, total, router]);

  // Define stripe element options with proper typing
  const options: StripeElementsOptions = {
    // You can pass additional options here if needed
    locale: "en",
  };

  return (
    <div className="relative min-h-screen bg-white py-12 px-4 sm:px-6 lg:px-8">
      {/* Global Back Button */}
      <BackButton />

      <div className="max-w-3xl mx-auto bg-white shadow-lg rounded-lg p-8">
        <h1 className="text-3xl font-bold text-black">Payment</h1>
        <p className="text-black mt-2">Enter your payment details below.</p>

        {/* Order Summary */}
        <div className="mt-6 p-4 bg-gray-100 rounded-lg">
          <h3 className="text-lg font-semibold text-black">🎟️ Order Summary</h3>
          <p className="text-black">Selected Seats: {seats}</p>
          <p className="text-lg font-semibold mt-2 text-black">
            Total Price: <span className="text-blue-600">${total}</span>
          </p>
        </div>

        {/* Payment Form */}
        <div className="mt-8">
          {success ? (
            <p className="text-green-600 font-medium text-lg">
              ✅ Payment Successful! Redirecting...
            </p>
          ) : (
            <Elements stripe={stripePromise} options={options}>
              <PaymentForm
                eventId={eventId}
                seats={seats}
                total={total}
                createPaymentIntent={createPaymentIntent}
                onSuccess={handlePaymentSuccess}
              />
            </Elements>
          )}
        </div>

        {/* Back to Checkout */}
        <div className="mt-4 text-center">
          <Link
            href="/checkout"
            className="text-blue-600 hover:text-blue-800 font-medium"
          >
            ← Back to Checkout
          </Link>
        </div>
      </div>
    </div>
  );
}
