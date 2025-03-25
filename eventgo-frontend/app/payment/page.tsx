"use client";

import { useState, useEffect, FormEvent, JSX } from "react";
import { useSearchParams, useRouter } from "next/navigation";
import Link from "next/link";
import BackButton from "@/components/BackButton";
import { CardElement, useStripe, useElements } from "@stripe/react-stripe-js";
import { Elements } from "@stripe/react-stripe-js";
import { loadStripe, StripeElementsOptions } from "@stripe/stripe-js";
import { useAuth } from "@/context/AuthContext";

// Make sure to call loadStripe outside component rendering
const stripePromise = loadStripe(
  process.env.NEXT_PUBLIC_STRIPE_PUBLISHABLE_KEY as string
);

interface PaymentFormProps {
  eventId: string | null;
  seats: string | null;
  total: string | null;
  reservationId: string | null; // Add reservationId
  userId: string | null;
  userEmail: string | null; // Add userId
  onSuccess: () => void;
}

// Create a PaymentForm component that uses Stripe
function PaymentForm({
  eventId,
  seats,
  total,
  reservationId,
  userId,
  userEmail,
  onSuccess,
}: PaymentFormProps) {
  const stripe = useStripe();
  const elements = useElements();
  const [loading, setLoading] = useState<boolean>(false);
  const [error, setError] = useState<string | null>(null);
  const [clientSecret, setClientSecret] = useState<string>("");

  useEffect(() => {
    // Create a payment intent when the page loads
    async function createPaymentIntent(): Promise<void> {
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

    if (eventId && seats && total) {
      createPaymentIntent();
    }
  }, [eventId, seats, total]);

  const handleSubmit = async (
    event: FormEvent<HTMLFormElement>
  ): Promise<void> => {
    event.preventDefault();

    if (!stripe || !elements) {
      // Stripe.js has not loaded yet
      return;
    }

    setLoading(true);
    setError(null);

    const cardElement = elements.getElement(CardElement);

    if (!cardElement) {
      setError("Payment form failed to load");
      setLoading(false);
      return;
    }

    try {
      const { error, paymentIntent } = await stripe.confirmCardPayment(
        clientSecret,
        {
          payment_method: {
            card: cardElement,
            billing_details: {
              name: "Customer Name", // Ideally get this from a form
            },
          },
        }
      );

      if (error) {
        throw new Error(error.message || "Payment failed");
      }

      if (paymentIntent?.status === "succeeded") {
        // First call confirm-booking endpoint in Stripe service
        const confirmResponse = await fetch(
          `${
            process.env.NEXT_PUBLIC_PAYMENTS_API_URL || "http://localhost:8004"
          }/confirm-booking`,
          {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify({
              payment_intent_id: paymentIntent.id,
              event_id: eventId,
              seats: seats?.split(","),
            }),
          }
        );

        if (!confirmResponse.ok) {
          const errorData = await confirmResponse.json();
          throw new Error(errorData.detail || "Failed to confirm payment");
        }

        // Then call process-booking endpoint in Booking service
        console.log(userEmail);
        const processBookingResponse = await fetch(
          `${
            process.env.NEXT_PUBLIC_BOOKING_SERVICE_API_URL ||
            "http://localhost:8007"
          }/bookings/process-booking`,
          {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify({
              eventId: eventId,
              seats: seats?.split(","),
              paymentIntentId: paymentIntent.id,
              reservationId: reservationId, // Add reservationId
              userId: userId, // Add userId
              userEmail: userEmail ? userEmail : "test@gmail.com",
            }),
          }
        );

        if (!processBookingResponse.ok) {
          const errorData = await processBookingResponse.json();
          throw new Error(
            errorData.errorMessage || "Failed to process booking"
          );
        }

        // Check that the booking was successful
        const bookingResult = await processBookingResponse.json();
        if (bookingResult.status !== "SUCCESS") {
          throw new Error(
            bookingResult.confirmationMessage || "Booking processing failed"
          );
        }

        // Clear localStorage after successful processing
        localStorage.removeItem("reservationId");
        localStorage.removeItem("userId");

        // Pass the payment intent ID to the success handler
        onSuccess();
      } else {
        throw new Error("Payment processing did not complete");
      }
    } catch (err) {
      console.error("Payment error:", err);
      setError(
        err instanceof Error
          ? err.message
          : "Payment processing failed. Please try again."
      );
    } finally {
      setLoading(false);
    }
  };

  return (
    <form onSubmit={handleSubmit}>
      {error && <div className="text-red-500 mb-4">{error}</div>}

      <div className="mb-6">
        <label className="block text-gray-700 mb-2">Card Details</label>
        <div className="border rounded p-3 bg-white">
          <CardElement
            options={{
              style: {
                base: {
                  fontSize: "16px",
                  color: "#32325d",
                  "::placeholder": {
                    color: "#aab7c4",
                  },
                },
                invalid: {
                  color: "#fa755a",
                  iconColor: "#fa755a",
                },
              },
            }}
          />
        </div>
      </div>

      <button
        type="submit"
        disabled={!stripe || loading || !clientSecret}
        className={`w-full text-center bg-blue-600 text-white font-medium py-3 px-8 rounded-md hover:bg-blue-700 transition-colors ${
          loading || !stripe || !clientSecret
            ? "opacity-50 cursor-not-allowed"
            : ""
        }`}
      >
        {loading ? "Processing Payment..." : `Pay $${total}`}
      </button>
    </form>
  );
}

export default function PaymentPage(): JSX.Element {
  const searchParams = useSearchParams();
  const router = useRouter();

  const { user } = useAuth();

  const eventId = searchParams.get("eventId");
  const seats = searchParams.get("seats");
  const total = searchParams.get("total");
  // Get reservationId from URL params or localStorage
  const [reservationId, setReservationId] = useState<string | null>(
    searchParams.get("reservationId") || null
  );
  const [userId, setUserId] = useState<string | null>(null);
  const [success, setSuccess] = useState<boolean>(false);

  useEffect(() => {
    // If reservationId wasn't in URL, try to get from localStorage
    if (!reservationId) {
      const storedReservationId = localStorage.getItem("reservationId");
      if (storedReservationId) {
        setReservationId(storedReservationId);
      }
    }

    // Get userId from localStorage
    const storedUserId = localStorage.getItem("userId");
    if (storedUserId) {
      setUserId(storedUserId);
    }
  }, [reservationId]);

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
                reservationId={reservationId} // Pass reservationId
                userId={userId}
                userEmail={user.email} // Pass userId
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
