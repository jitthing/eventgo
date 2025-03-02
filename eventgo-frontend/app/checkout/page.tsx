// eventgo-frontend/app/checkout/page.tsx
"use client";

import { useSearchParams, useRouter } from "next/navigation";
import { useState, useEffect } from "react";
import Link from "next/link";
import { getEvent } from "@/lib/api";
import { formatDate } from "@/lib/utils";
import BackButton from "@/components/BackButton";

export default function CheckoutPage() {
	// 1) Read query params in a Client Component
	const searchParams = useSearchParams();
	const router = useRouter();

	const eventIdParam = searchParams.get("eventId");
	const seatsParam = searchParams.get("seats");

	// 2) Local state for storing the event & any errors
	const [event, setEvent] = useState<any | null>(null);
	const [error, setError] = useState<boolean>(false);

	// 3) Parse query strings
	const eventId = parseInt(eventIdParam ?? "NaN", 10);
	const selectedSeats = seatsParam ? seatsParam.split(",").map(Number) : [];

	// 4) Fetch data (getEvent) in useEffect
	useEffect(() => {
		if (isNaN(eventId) || selectedSeats.length === 0) {
			// If we have invalid or missing query params, set an error
			setError(true);
			return;
		}

		getEvent(eventId)
			.then((res) => setEvent(res))
			.catch(() => setError(true));
	}, [eventId, selectedSeats]);

	// 5) Render states
	if (error) {
		// Instead of a 404, you can do a simple message or redirect
		return (
			<div className="min-h-screen bg-white py-12 px-4 sm:px-6 lg:px-8">
				<p className="text-center text-red-500">
					Invalid or missing Event ID / seats.{" "}
					<Link href="/" className="text-blue-600 underline">
						Go Home
					</Link>
				</p>
			</div>
		);
	}

	if (!event) {
		return (
			<div className="min-h-screen bg-white flex items-center justify-center">
				<p className="text-gray-600">Loading event...</p>
			</div>
		);
	}

	// 6) Once event is loaded, render page
	const totalPrice = selectedSeats.length * event.price;

	return (
		<div className="relative min-h-screen bg-white py-12 px-4 sm:px-6 lg:px-8">
			{/* Global Back Button */}
			<BackButton />
			<div className="max-w-4xl mx-auto bg-white shadow-lg rounded-lg p-8">
				<h1 className="text-3xl font-bold text-black">Checkout</h1>
				<p className="text-black mt-2">Confirm your details before proceeding to payment.</p>

				{/* Event Details */}
				<div className="mt-6">
					<h2 className="text-2xl font-semibold text-black">{event.title}</h2>
					<p className="text-black">
						{formatDate(event.date)} • {event.location}
					</p>
					<p className="text-black">🏟️ Venue: {event.venue}</p>
				</div>

				{/* Selected Seats */}
				<div className="mt-6 p-4 bg-gray-100 rounded-lg">
					<h3 className="text-lg font-semibold text-black">🎟️ Selected Seats</h3>
					<p className="text-black">{selectedSeats.join(", ")}</p>
					<p className="text-lg font-semibold mt-2 text-black">
						Total Price: <span className="text-blue-600">${totalPrice.toFixed(2)}</span>
					</p>
					<Link href={`/events/${event.id}`} className="mt-3 inline-block text-blue-600 hover:text-blue-800 font-medium">
						Edit Selection
					</Link>
				</div>

				{/* Proceed to Payment */}
				<div className="mt-8">
					<Link href={`/stripe-payment?eventId=${event.id}&seats=${selectedSeats.join(",")}&total=${totalPrice}`} className="block text-center bg-blue-600 text-white font-medium py-3 px-8 rounded-md hover:bg-blue-700 transition-colors">
						Proceed to Payment
					</Link>
				</div>
			</div>
		</div>
	);
}
