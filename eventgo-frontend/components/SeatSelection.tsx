"use client";

import { useState, useEffect } from "react";
import { getAvailableSeats } from "@/lib/api";
import { Seat, TicketStatus } from "@/lib/interfaces";

export default function SeatSelection({ eventId, selectedSeats, toggleSeat }: { eventId: number; selectedSeats: number[]; toggleSeat: (seatId: number) => void }) {
	const [availableSeats, setAvailableSeats] = useState<Seat[]>([]);
	const [loading, setLoading] = useState(true);
	const [error, setError] = useState<string | null>(null);

	useEffect(() => {
		const fetchSeats = async () => {
			try {
				const seats = await getAvailableSeats(eventId);
				setAvailableSeats(seats);
			} catch (err) {
				setError("Failed to load seats.");
			} finally {
				setLoading(false);
			}
		};
		fetchSeats();
	}, [eventId]);

	if (loading) return <p className="text-gray-600 text-center">Loading available seats...</p>;
	if (error) return <p className="text-red-500 text-center">{error}</p>;

	return (
		<div className="mt-6 p-6 bg-gray-100 rounded-lg">
			<h3 className="text-lg text-black font-semibold mb-4">🎟️ Select Your Seats</h3>

			{/* Show selected seat count */}
			<p className="text-sm text-gray-600 mb-3">Selected Seats: {selectedSeats.length > 0 ? selectedSeats.map((seatId) => availableSeats.find((seat) => seat.id === seatId)?.seat_number || `Seat ${seatId}`).join(", ") : "None"}</p>
			{/* Render seat buttons */}
			<div className="grid grid-cols-10 gap-2">
				{availableSeats.map((seat) => {
					const isReserved = seat.status === TicketStatus.RESERVED || seat.status === TicketStatus.SOLD;
					const isSelected = selectedSeats.includes(seat.id);

					return (
						<button
							key={seat.id}
							className={`w-10 h-10 text-sm font-medium rounded-md 
                ${isReserved ? "bg-gray-400 text-white cursor-not-allowed" : isSelected ? "bg-blue-600 text-white" : "bg-gray-300 hover:bg-gray-400"}`}
							onClick={() => !isReserved && toggleSeat(seat.id)}
							disabled={isReserved}
							title={isReserved ? `Seat ${seat.seat_number} is ${seat.status}` : ""}
						>
							{seat.seat_number}
						</button>
					);
				})}
			</div>
		</div>
	);
}
