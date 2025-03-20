"use client";

import { useEffect, useState } from "react";
import { useRouter } from "next/navigation";
import Link from "next/link";
import { fetchUser } from "@/lib/auth";
import { getUserTickets, getEvent } from "@/lib/api";
import { useAuth } from "@/context/AuthContext";
import { formatEventDuration } from "@/lib/utils";

interface GroupedTickets {
	event: any;
	tickets: any[];
}

export default function ProfilePage() {
	const router = useRouter();
	const { user, setUser } = useAuth();
	const [groupedTickets, setGroupedTickets] = useState<GroupedTickets[]>([]);
	const [loading, setLoading] = useState(true);
	const [error, setError] = useState<string | null>(null);

	useEffect(() => {
		async function fetchData() {
			if (!user) {
				try {
					const userData = await fetchUser();
					setUser(userData);
				} catch (err) {
					router.push("/login");
					return;
				}
			}
			try {
				// Get the user's tickets from the ticket service
				const userTickets = await getUserTickets(user!.id);
				// Fetch event details for each ticket, making sure to use the correct property: eventId.
				const ticketsWithEvent = await Promise.all(
					userTickets.map(async (ticket: any) => {
						try {
							const event = await getEvent(ticket.eventId);
							return { ...ticket, event };
						} catch (err) {
							return { ...ticket, event: null };
						}
					})
				);

				// Group tickets by eventId
				const grouped = ticketsWithEvent.reduce((acc: any, ticket: any) => {
					const key = ticket.eventId;
					if (!acc[key]) {
						acc[key] = { event: ticket.event, tickets: [] };
					}
					acc[key].tickets.push(ticket);
					return acc;
				}, {});

				setGroupedTickets(Object.values(grouped));
			} catch (err) {
				console.log(err);
				setError("Failed to load tickets.");
			} finally {
				setLoading(false);
			}
		}
		fetchData();
	}, [user, setUser, router]);

	async function handleTransfer(ticketId: number) {
		// Placeholder for transfer functionality
		alert(`Transfer functionality coming soon for ticket ${ticketId}`);
	}

	return (
		<div className="min-h-screen bg-gray-50 py-12 px-4 sm:px-6 lg:px-8 space-y-10">
			{/* User Profile and Settings Section */}
			<div className="max-w-4xl mx-auto bg-white shadow-lg rounded-lg p-8 space-y-6">
				{/* Profile Details */}
				<div className="flex items-center space-x-4">
					<img src={user?.avatar || "https://api.dicebear.com/6.x/initials/svg?seed=User"} alt="Profile Avatar" className="w-16 h-16 rounded-full" />
					<div>
						<h1 className="text-3xl font-bold text-black">{user?.full_name || "User"}</h1>
						<p className="text-black text-lg">{user?.email}</p>
					</div>
				</div>

				{/* Settings */}
				<div className="mt-6 p-4 bg-gray-100 rounded-lg">
					<h3 className="text-lg font-semibold text-black">⚙️ Settings</h3>
					<ul className="text-black space-y-2 mt-2">
						<li>
							<Link href="#" className="text-blue-600 hover:text-blue-800">
								Change Password
							</Link>
						</li>
						<li>
							<Link href="#" className="text-blue-600 hover:text-blue-800">
								Manage Notifications
							</Link>
						</li>
						<li>
							<Link href="#" className="text-blue-600 hover:text-blue-800">
								Privacy Settings
							</Link>
						</li>
					</ul>
				</div>
			</div>

			{/* Tickets Section */}
			<div className="max-w-4xl mx-auto bg-white shadow-lg rounded-lg p-8">
				<h2 className="text-2xl font-bold text-black mb-4">My Tickets</h2>
				{loading ? (
					<p className="text-black text-center mt-4">Loading tickets...</p>
				) : error ? (
					<p className="text-red-500 text-center mt-4">{error}</p>
				) : groupedTickets.length === 0 ? (
					<p className="text-black">You have no tickets.</p>
				) : (
					groupedTickets.map((group: GroupedTickets) => (
						<div key={group.event?.event_id || Math.random()} className="mb-8 border border-gray-200 rounded-lg overflow-hidden">
							{/* Event Card Header */}
							<div className="flex items-center bg-gray-100 p-4">
								{group.event?.image_url && <img src={group.event.image_url} alt={group.event.title} className="w-20 h-20 object-cover rounded mr-4" />}
								<div>
									<h3 className="text-xl font-semibold text-black">
										<Link href={`/events/${group.event?.event_id}`}>
											<span className="hover:text-blue-600">{group.event?.title || "Event"}</span>
										</Link>
									</h3>
									<p className="text-gray-600">{group.event?.venue}</p>
									{group.event?.date && <p className="text-gray-600">{formatEventDuration(group.event.date)}</p>}{" "}
								</div>
							</div>

							{/* Tickets for this event */}
							<div className="p-4 grid grid-cols-1 md:grid-cols-2 gap-4">
								{group.tickets.map((ticket) => (
									<div key={ticket.ticketId} className="p-4 bg-white border border-gray-200 rounded-lg shadow hover:shadow-md transition">
										<div className="flex justify-between items-center">
											<p className="text-black font-medium">Seat: {ticket.seatNumber}</p>
											{/* <span className={`text-sm font-medium ${ticket.status === "sold" ? "text-green-600" : ticket.status === "reserved" ? "text-yellow-600" : "text-gray-600"}`}>
												{ticket.status.charAt(0).toUpperCase() + ticket.status.slice(1)}
											</span> */}
										</div>
										<p className="mt-1 text-black">Category: {ticket.category}</p>
										<p className="mt-1 text-black">Price: ${Number(ticket.price).toFixed(2)}</p>
										<div className="mt-4">
											<button onClick={() => handleTransfer(ticket.ticketId)} className="bg-blue-600 text-white py-2 px-4 rounded hover:bg-blue-700 transition-colors w-full">
												Transfer Ticket
											</button>
										</div>
									</div>
								))}
							</div>
						</div>
					))
				)}

				{/* Logout Section */}
				<div className="mt-10 text-center">
					<button
						onClick={async () => {
							try {
								// Call your logout function if available, then clear the context
								// e.g., await logoutUser();
								setUser(null);
								router.replace("/login");
							} catch (err) {
								console.error("Logout error:", err);
								router.replace("/login");
							}
						}}
						className="bg-red-600 text-white py-3 px-6 rounded hover:bg-red-700 transition-colors"
					>
						Log Out
					</button>
				</div>
			</div>
		</div>
	);
}
