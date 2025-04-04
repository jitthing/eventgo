"use client"; // Fixes the error

import { useSearchParams } from "next/navigation";
import Link from "next/link";
import { useState, useEffect } from "react";

const TICKET_INVENTORY_URL = process.env.NEXT_PUBLIC_TICKETS_API_URL;

export default function ConfirmationPage() {
  const searchParams = useSearchParams();
  const eventId = searchParams.get("eventId");
  const seats = searchParams.get("seats");
  const total = searchParams.get("total");
  const split = searchParams.get("split");

  // const [refundOption, setRefundOption] = useState<string | null>(null);

  // Add this useEffect to clear localStorage when confirmation page loads
  useEffect(() => {
    // Clear all reservation and payment related data
    localStorage.removeItem("reservationId");
    localStorage.removeItem("userId");
    localStorage.removeItem("selectedSeats");
    localStorage.removeItem("totalPrice");
    localStorage.removeItem("stripeRedirectUrl");
    localStorage.removeItem("isGroupBooking");
    
    // You may need to update some state in your parent component
    // If you have access to a context that manages this state
    // Example: setHasPendingReservation(false);
  }, []);

  async function handleChoice(choice: String) {
    const preferenceResponse = await fetch(
      `${TICKET_INVENTORY_URL}/tickets/update-preference`,
      {
        method: "PATCH",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({
          eventId: eventId,
          seatId: seats,
          preference: choice,
        }),
      }
    );
  }

  return (
    <div className="min-h-screen bg-white py-12 px-4 sm:px-6 lg:px-8">
      <div className="max-w-3xl mx-auto bg-white shadow-lg rounded-lg p-8 text-center">
        <h1 className="text-3xl font-bold text-black">
          🎉 Payment Successful!
        </h1>
        <p className="text-lg text-black mt-2">
          Your order has been confirmed.
        </p>

        {/* Order Details */}
        <div className="mt-6 p-4 bg-gray-100 rounded-lg">
          <h3 className="text-lg font-semibold text-black">🎟️ Your Order</h3>
          <p className="text-black">Event ID: {eventId}</p>
          <p className="text-black">Selected Seats: {seats}</p>
          <p className="text-lg font-semibold mt-2 text-black">
            Total Paid: <span className="text-blue-600">${total}</span>
          </p>
        </div>

        {/* Refund Option - Only show if split payment is used */}
        {split === "true" && (
          <div className="mt-6 p-4 bg-yellow-50 border border-yellow-200 rounded-lg">
            <h3 className="text-lg font-semibold text-black">
              Automatic Refund Option
            </h3>
            <p className="text-sm text-gray-600 mt-1 mb-4">
              Would you want to keep your ticket if party members don't pay
              within 10 minutes?
            </p>

            <div className="flex space-x-4 justify-center">
              <button
                onClick={() => handleChoice("keep")}
                className={`px-6 py-2 rounded-md font-medium ${"bg-green-600 text-white hover:bg-green-500"}`}
              >
                Yes
              </button>
              <button
                onClick={() => handleChoice("refund")}
                className={`px-6 py-2 rounded-md font-medium ${"bg-red-600 text-white hover:bg-red-500"}`}
              >
                No
              </button>
            </div>
          </div>
        )}

        {/* Go Back to Homepage */}
        <div className="mt-8">
          <Link
            href="/"
            className="block text-center bg-blue-600 text-white font-medium py-3 px-8 rounded-md hover:bg-blue-700 transition-colors"
          >
            Back to Homepage
          </Link>
        </div>
      </div>
    </div>
  );
}
