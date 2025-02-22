import { getEvent } from "@/lib/api";
import { formatDate } from "@/lib/utils";
import { notFound } from "next/navigation";
import Link from "next/link";

interface CheckoutPageProps {
	searchParams: { eventId?: string; seats?: string };
}

// Fetch event details
export default async function CheckoutPage({ searchParams }: CheckoutPageProps) {
	const eventId = Number(searchParams.eventId);
	const selectedSeats = searchParams.seats ? searchParams.seats.split(",").map(Number) : [];

	if (isNaN(eventId) || selectedSeats.length === 0) {
		return notFound(); // Show 404 if missing event ID or no seats selected
	}

	try {
		const event = await getEvent(eventId);
		const totalPrice = selectedSeats.length * event.price;

		return (
			<div className="min-h-screen bg-white py-12 px-4 sm:px-6 lg:px-8">
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
						<Link href={`/payment?eventId=${event.id}&seats=${selectedSeats.join(",")}&total=${totalPrice}`} className="block text-center bg-blue-600 text-white font-medium py-3 px-8 rounded-md hover:bg-blue-700 transition-colors">
							Proceed to Payment
						</Link>
					</div>
				</div>
			</div>
		);
	} catch (error) {
		return notFound(); // Show 404 if event not found
	}
}
