"use client";

import { useState } from "react";
import { searchUsers } from "@/lib/auth";
import { useAuth } from "@/context/AuthContext";

const TICKET_TRANSFER_URL = process.env.NEXT_PUBLIC_TICKET_TRANSFER_API_URL;

interface TransferModalProps {
	ticketId: number;
	eventId: number;
	eventTitle: string;
	seatNumber: string;
	onClose: () => void;
	onTransferInitiated: (ticketId: number) => void;
}

export default function TransferModal({ ticketId, eventId, eventTitle, seatNumber, onClose, onTransferInitiated }: TransferModalProps) {
	const { user } = useAuth();
	const [email, setEmail] = useState("");
	const [error, setError] = useState("");
	const [recipient, setRecipient] = useState<{
		id: number;
		email: string;
	} | null>(null);

	const [isTransferring, setIsTransferring] = useState(false);

	const handleSearch = async () => {
		setError("");
		if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email)) {
			return setError("Invalid email format.");
		}
		if (user?.email === email) {
			return setError("You cannot transfer a ticket to yourself.");
		}
		try {
			const results = await searchUsers(email);
			if (Array.isArray(results) && results.length) {
				setRecipient({ id: results[0].id, email: results[0].email });
			} else {
				setError("No user found with that email.");
			}
		} catch {
			setError("Search failed. Try again.");
		}
	};

	const handleTransfer = async () => {
		if (!recipient || !user) return;
		setIsTransferring(true);

		const transferBody = {
			ticket_id: ticketId,
			seller_id: user.id,
			buyer_id: recipient.id,
			seller_email: user.email,
			buyer_email: recipient.email,
			redirect_url: `http://localhost:3000/confirmation?eventId=${eventId}&seats=${seatNumber}&total=`,
		};

		try {
			const response = await fetch(`${TICKET_TRANSFER_URL}/generate-transfer-payment-link`, {
				method: "POST",
				headers: { "Content-Type": "application/json" },
				body: JSON.stringify(transferBody),
			});

			const text = await response.text();
			let body;
			try {
				body = JSON.parse(text);
			} catch {
				body = text;
			}

			if (!response.ok) {
				const errorMsg = [`HTTP ${response.status} ${response.statusText}`, body?.error ?? JSON.stringify(body)].filter(Boolean).join(" — ");

				console.error("Transfer failed:", { status: response.status, body });
				alert(errorMsg);
				return;
			}

			window.alert("Ticket transfer request sent successfully!");
			onTransferInitiated(ticketId); // ✅ Inform parent to update state
			console.log("Transfer success:", body);
		} catch (err: unknown) {
			console.error("Network error during transfer:", err);

			const message = err instanceof Error ? err.message : String(err);

			alert("Network error: " + message);
			return;
		} finally {
			setIsTransferring(false);
			onClose();
		}
	};

	return (
		<div className="fixed inset-0 flex items-center justify-center bg-black bg-opacity-50 z-50">
			<div className="bg-white p-10 rounded-lg shadow-lg w-[600px] max-h-[80vh] overflow-auto">
				<h2 className="text-2xl font-bold text-black mb-6">Transfer Ticket</h2>

				{/* Ticket & Event Details */}
				<div className="mb-6 p-4 bg-gray-100 rounded">
					<p className="text-black">
						<strong>Event:</strong> {eventTitle}
					</p>
					<p className="text-black">
						<strong>Ticket ID:</strong> {ticketId}
					</p>
					<p className="text-black">
						<strong>Seat:</strong> {seatNumber}
					</p>
					<p className="text-black">
						<strong>Event ID:</strong> {eventId}
					</p>
				</div>

				{!recipient ? (
					<>
						<label className="block text-black font-medium mb-2">Recipient Email</label>
						<input
							type="email"
							value={email}
							onChange={(e) => {
								setEmail(e.target.value);
								setError("");
							}}
							placeholder="friend@example.com"
							className="w-full p-3 border rounded mb-2 text-black"
						/>
						{error && <p className="text-red-500 mb-4">{error}</p>}
						<div className="flex justify-end space-x-3">
							<button onClick={onClose} className="px-5 py-2 rounded bg-red-600 text-white hover:bg-red-700">
								Cancel
							</button>

							<button onClick={handleSearch} className="px-5 py-2 rounded bg-blue-600 text-white">
								Find User
							</button>
						</div>
					</>
				) : (
					<>
						<p className="text-black mb-6">
							Send this ticket to <strong>{recipient.email}</strong>?
						</p>
						<div className="flex justify-end space-x-3">
							<button onClick={() => setRecipient(null)} className="px-5 py-2 rounded bg-red-600 text-white hover:bg-red-700">
								Cancel
							</button>
							<button
								onClick={handleTransfer}
								disabled={isTransferring} // ⬅️ disable while loading
								className={`px-5 py-2 rounded bg-green-600 text-white flex items-center justify-center ${isTransferring ? "opacity-50 cursor-not-allowed" : ""}`}
							>
								{isTransferring ? (
									<span className="animate-spin w-5 h-5 border-2 border-white border-t-transparent rounded-full" /> // loader
								) : (
									"Send Transfer Payment Request"
								)}
							</button>
						</div>
					</>
				)}
			</div>
		</div>
	);
}
