"use client"; // Fixes the error

import { useSearchParams } from "next/navigation";
import Link from "next/link";

export default function ConfirmationPage() {
	const searchParams = useSearchParams();
	const eventId = searchParams.get("eventId");
	const seats = searchParams.get("seats");
	const total = searchParams.get("total");

	return (
		<div className="min-h-screen bg-white py-12 px-4 sm:px-6 lg:px-8">
			<div className="max-w-3xl mx-auto bg-white shadow-lg rounded-lg p-8 text-center">
				<h1 className="text-3xl font-bold text-black">🎉 Payment Successful!</h1>
				<p className="text-lg text-black mt-2">Your order has been confirmed.</p>

				{/* Order Details */}
				<div className="mt-6 p-4 bg-gray-100 rounded-lg">
					<h3 className="text-lg font-semibold text-black">🎟️ Your Order</h3>
					<p className="text-black">Event ID: {eventId}</p>
					<p className="text-black">Selected Seats: {seats}</p>
					<p className="text-lg font-semibold mt-2 text-black">
						Total Paid: <span className="text-blue-600">${total}</span>
					</p>
				</div>

				{/* Go Back to Homepage */}
				<div className="mt-8">
					<Link href="/" className="block text-center bg-blue-600 text-white font-medium py-3 px-8 rounded-md hover:bg-blue-700 transition-colors">
						Back to Homepage
					</Link>
				</div>
			</div>
		</div>
	);
}
