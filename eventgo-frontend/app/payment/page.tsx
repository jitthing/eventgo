"use client"; // Since we'll use useState & useEffect

import { useState, useEffect } from "react";
import { useSearchParams, useRouter } from "next/navigation";
import Link from "next/link";

export default function PaymentPage() {
	const searchParams = useSearchParams();
	const router = useRouter();

	const eventId = searchParams.get("eventId");
	const seats = searchParams.get("seats");
	const total = searchParams.get("total");

	const [loading, setLoading] = useState(false);
	const [success, setSuccess] = useState(false);

	// Simulated "payment" process
	const handleMockPayment = () => {
		setLoading(true);
		setTimeout(() => {
			setLoading(false);
			setSuccess(true);
			router.push(`/confirmation?eventId=${eventId}&seats=${seats}&total=${total}`);
		}, 2000); // Simulate 2 seconds of processing time
	};

	useEffect(() => {
		if (!eventId || !seats || !total) {
			router.push("/checkout"); // Redirect if missing params
		}
	}, [eventId, seats, total, router]);

	return (
		<div className="min-h-screen bg-white py-12 px-4 sm:px-6 lg:px-8">
			<div className="max-w-3xl mx-auto bg-white shadow-lg rounded-lg p-8">
				<h1 className="text-3xl font-bold text-black">Payment</h1>
				<p className="text-black mt-2">Confirm your payment details below.</p>

				{/* Order Summary */}
				<div className="mt-6 p-4 bg-gray-100 rounded-lg">
					<h3 className="text-lg font-semibold text-black">🎟️ Order Summary</h3>
					<p className="text-black">Selected Seats: {seats}</p>
					<p className="text-lg font-semibold mt-2 text-black">
						Total Price: <span className="text-blue-600">${total}</span>
					</p>
				</div>

				{/* Payment Button */}
				<div className="mt-8">
					{success ? (
						<p className="text-green-600 font-medium text-lg">✅ Payment Successful! Redirecting...</p>
					) : (
						<button
							onClick={handleMockPayment}
							disabled={loading}
							className={`w-full text-center bg-blue-600 text-white font-medium py-3 px-8 rounded-md hover:bg-blue-700 transition-colors ${loading ? "opacity-50 cursor-not-allowed" : ""}`}
						>
							{loading ? "Processing Payment..." : "Pay Now"}
						</button>
					)}
				</div>

				{/* Back to Checkout */}
				<div className="mt-4 text-center">
					<Link href="/checkout" className="text-blue-600 hover:text-blue-800 font-medium">
						← Back to Checkout
					</Link>
				</div>
			</div>
		</div>
	);
}
