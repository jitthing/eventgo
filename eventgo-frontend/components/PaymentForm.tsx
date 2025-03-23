import { CardElement, useStripe, useElements } from "@stripe/react-stripe-js";
import { useState, useEffect, FormEvent, JSX } from "react";

interface PaymentFormProps {
  eventId: string | null;
  seats: string | null;
  total: string | null;
  createPaymentIntent: Function;
  onSuccess: () => void;
}

// Create a PaymentForm component that uses Stripe
export function PaymentForm({
  eventId,
  seats,
  total,
  createPaymentIntent,
  onSuccess,
}: PaymentFormProps) {
  const stripe = useStripe();
  const elements = useElements();
  const [loading, setLoading] = useState<boolean>(false);
  const [error, setError] = useState<string | null>(null);
  const [clientSecret, setClientSecret] = useState<string>("");

  useEffect(() => {
    // Create a payment intent when the page loads
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
        // Call confirm-booking endpoint to finalize the booking
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
          throw new Error(errorData.detail || "Failed to confirm booking");
        }

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
