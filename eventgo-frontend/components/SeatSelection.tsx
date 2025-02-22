"use client"; // Enables useState in this component

import { useState } from "react";
import Link from "next/link";

export default function SeatSelection({ capacity, eventId }: { capacity: number; eventId: number }) {
	const [selectedSeats, setSelectedSeats] = useState<number[]>([]);

	const toggleSeat = (seat: number) => {
		setSelectedSeats((prev) => (prev.includes(seat) ? prev.filter((s) => s !== seat) : [...prev, seat]));
	};

	return (
		<div className="mt-6 p-6 bg-gray-100 rounded-lg">
			<h3 className="text-lg font-semibold mb-4">🎟️ Select Your Seats</h3>
			<div className="grid grid-cols-10 gap-2">
				{Array.from({ length: Math.min(capacity, 50) }, (_, i) => i + 1).map((seat) => (
					<button key={seat} className={`w-10 h-10 text-sm font-medium rounded-md ${selectedSeats.includes(seat) ? "bg-blue-600 text-white" : "bg-gray-300 hover:bg-gray-400"}`} onClick={() => toggleSeat(seat)}>
						{seat}
					</button>
				))}
			</div>

			{selectedSeats.length > 0 && <p className="mt-3 text-sm text-gray-600">Selected Seats: {selectedSeats.join(", ")}</p>}

			<Link
				href={`/checkout?eventId=${eventId}&seats=${selectedSeats.join(",")}`}
				className={`mt-6 block text-center bg-blue-600 text-white font-medium py-3 px-8 rounded-md hover:bg-blue-700 transition-colors ${selectedSeats.length === 0 ? "opacity-50 pointer-events-none" : ""}`}
			>
				Buy Tickets ({selectedSeats.length} Selected)
			</Link>
		</div>
	);
}
