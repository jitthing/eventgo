"use client";

import { useEffect, useState } from "react";
import { useAuth } from "@/context/AuthContext";
import { useRouter } from "next/navigation";
import { getAllEvents, cancelEvent, cancelEventUsingCompositeService } from "@/lib/api";
import Image from "next/image";
import Link from "next/link";
import ConfirmModal from "@/components/ConfirmModal";
import { Event } from "@/lib/interfaces";
import AdminEventsSkeleton from "@/components/AdminEventsSkeleton";

export default function AdminEventsPage() {
	const { user, loading: authLoading } = useAuth();
	const router = useRouter();

	const [events, setEvents] = useState<Event[]>([]);
	const [loading, setLoading] = useState(true);
	const [error, setError] = useState<string | null>(null);
	const [selectedEvent, setSelectedEvent] = useState<Event | null>(null);
	const [isModalOpen, setIsModalOpen] = useState(false);

	// Redirect if not admin
	useEffect(() => {
		if (!authLoading) {
			if (!user || user.role !== "admin") {
				router.replace("/404");
			}
		}
	}, [authLoading, user, router]);

	const fetchEvents = async () => {
		setLoading(true);
		try {
			const data = await getAllEvents();
			setEvents(data);
		} catch {
			setError("Failed to fetch events.");
		} finally {
			setLoading(false);
		}
	};

	useEffect(() => {
		if (user?.role === "admin") fetchEvents();
	}, [user]);

	const handleCancelClick = (event: Event) => {
		setSelectedEvent(event);
		setIsModalOpen(true);
	};

	const confirmCancel = async () => {
		if (!selectedEvent) return;
		try {
			// await cancelEvent(selectedEvent.event_id);
			await cancelEventUsingCompositeService(selectedEvent.event_id);
			window.alert(`Event ID: ${selectedEvent.event_id} has been canceled`);
			await fetchEvents();
		} catch {
			window.alert("Failed to cancel event. Please try again.");
		} finally {
			setIsModalOpen(false);
			setSelectedEvent(null);
		}
	};

	if (authLoading || loading) {
		return <AdminEventsSkeleton />;
	}

	if (error) {
		return <p className="text-red-500 text-center mt-20">{error}</p>;
	}

	return (
		<div className="min-h-screen bg-white">
			<section className="bg-blue-600 text-white py-16 text-center">
				<h1 className="text-5xl font-bold">Manage Events</h1>
				<p className="mt-4 text-xl">View, Edit, and Cancel Events</p>
			</section>

			<section className="py-16 bg-gray-50 max-w-7xl mx-auto px-4 grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6">
				{events.map((event) => {
					const isCanceled = event.status === "cancelled";
					return (
						<div key={event.event_id} className={`relative bg-white rounded-lg shadow-md flex flex-col ${isCanceled ? "opacity-50 grayscale" : ""}`}>
							{isCanceled && (
								<div className="absolute inset-0 bg-gray-400 bg-opacity-50 flex items-center justify-center">
									<p className="text-white font-bold">CANCELED</p>
								</div>
							)}
							<div className="relative h-48">
								<Image src={event.image_url} alt={event.title} fill className="object-cover" />
							</div>
							<div className="p-4 flex flex-col flex-grow">
								<span className="text-blue-600 text-sm">{event.category}</span>
								<h3 className="text-black font-semibold mt-1">{event.title}</h3>
								<p className="text-black mt-1">{new Date(event.date).toLocaleDateString()}</p>
								<p className="text-black mt-1">{event.venue}</p>

								<div className="mt-auto space-y-2">
									<Link
										href={`/admin/events/edit/${event.event_id}`}
										className={`block text-center py-2 rounded ${isCanceled ? "bg-gray-500 cursor-not-allowed" : "bg-blue-600 hover:bg-blue-700"} text-white`}
										aria-disabled={isCanceled}
									>
										Edit
									</Link>
									<button onClick={() => handleCancelClick(event)} disabled={isCanceled} className={`w-full py-2 rounded ${isCanceled ? "bg-gray-500 cursor-not-allowed" : "bg-red-600 hover:bg-red-700"} text-white`}>
										Cancel
									</button>
								</div>
							</div>
						</div>
					);
				})}
			</section>

			{isModalOpen && selectedEvent && <ConfirmModal title="Cancel Event" message={`Are you sure you want to cancel "${selectedEvent.title}"? This action cannot be undone.`} onConfirm={confirmCancel} onCancel={() => setIsModalOpen(false)} />}
		</div>
	);
}
