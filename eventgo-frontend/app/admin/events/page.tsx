"use client";

import { useEffect, useState } from "react";
import { getAllEvents } from "@/lib/api";
import Image from "next/image";
import Link from "next/link";
import ConfirmModal from "@/components/ConfirmModal";

export default function AdminEventsPage() {
	const [events, setEvents] = useState<any[]>([]);
	const [loading, setLoading] = useState(true);
	const [error, setError] = useState<string | null>(null);
	const [selectedEvent, setSelectedEvent] = useState<any | null>(null);
	const [isModalOpen, setIsModalOpen] = useState(false);
	const [canceledEventIds, setCanceledEventIds] = useState<number[]>([]); // Track canceled events

	useEffect(() => {
		const fetchEvents = async () => {
			try {
				const data = await getAllEvents();
				setEvents(data);
			} catch (err) {
				setError("Failed to fetch events.");
			} finally {
				setLoading(false);
			}
		};
		fetchEvents();
	}, []);

	const handleCancelClick = (event: any) => {
		setSelectedEvent(event);
		setIsModalOpen(true);
	};

	const confirmCancel = () => {
		if (selectedEvent) {
			alert(`Event "${selectedEvent.title}" has been canceled.`);
			setCanceledEventIds((prev) => [...prev, selectedEvent.event_id]); // Mark event as canceled
		}
		setIsModalOpen(false);
		setSelectedEvent(null);
	};

	if (loading) return <p className="text-center text-black">Loading events...</p>;
	if (error) return <p className="text-red-500 text-center">{error}</p>;

	return (
		<div className="min-h-screen bg-white">
			{/* Admin Page Header */}
			<section className="bg-blue-600 text-white py-16">
				<div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
					<div className="text-center">
						<h1 className="text-4xl md:text-5xl font-bold">Manage Events</h1>
						<p className="text-xl md:text-2xl mt-4">View, Edit, and Cancel Events</p>
					</div>
				</div>
			</section>

			{/* Events Management Section */}
			<section className="py-16 bg-gray-50">
				<div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
					<h2 className="text-3xl font-bold text-black text-center mb-8">Event List</h2>

					<div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6">
						{events.map((event) => {
							const isCanceled = canceledEventIds.includes(event.event_id);

							return (
								<div key={event.event_id} className={`relative bg-white rounded-lg shadow-md overflow-hidden flex flex-col h-full transition-opacity ${isCanceled ? "opacity-50 grayscale" : ""}`}>
									{/* Grey-out overlay for canceled events */}
									{isCanceled && (
										<div className="absolute inset-0 bg-gray-400 bg-opacity-40 flex items-center justify-center">
											<p className="text-white text-lg font-bold">CANCELED</p>
										</div>
									)}

									{/* Event Image */}
									<div className="relative h-48">
										<Image src={event.image_url} alt={event.title} fill className="object-cover" />
									</div>

									{/* Event Content */}
									<div className="p-4 bg-white flex flex-col flex-grow">
										<span className="text-blue-600 text-sm font-medium">{event.category}</span>
										<h3 className="text-black font-semibold mt-1 line-clamp-2">{event.title}</h3>
										<p className="text-black mt-1">{new Date(event.date).toLocaleDateString()}</p>
										<p className="text-black mt-1">{event.location}</p>
										<p className="text-black font-medium mt-2">Starting at ${event.price}</p>

										{/* Spacer to push buttons to bottom */}
										<div className="flex-grow"></div>

										{/* Admin Controls */}
										<div className="mt-4 space-y-2">
											<Link
												href={`/admin/events/edit/${event.event_id}`}
												className={`block text-center py-2 px-4 rounded-md transition-colors ${isCanceled ? "bg-gray-500 text-white cursor-not-allowed" : "bg-blue-600 text-white hover:bg-blue-700"}`}
												tabIndex={isCanceled ? -1 : undefined}
												aria-disabled={isCanceled}
											>
												Edit
											</Link>
											<button
												onClick={() => handleCancelClick(event)}
												className={`block w-full text-center py-2 px-4 rounded-md transition-colors ${isCanceled ? "bg-gray-500 text-white cursor-not-allowed" : "bg-red-600 text-white hover:bg-red-700"}`}
												disabled={isCanceled}
											>
												Cancel
											</button>
										</div>
									</div>
								</div>
							);
						})}
					</div>
				</div>
			</section>

			{/* Confirmation Modal */}
			{isModalOpen && selectedEvent && <ConfirmModal title="Cancel Event" message={`Are you sure you want to cancel "${selectedEvent.title}"? This action cannot be undone.`} onConfirm={confirmCancel} onCancel={() => setIsModalOpen(false)} />}
		</div>
	);
}
