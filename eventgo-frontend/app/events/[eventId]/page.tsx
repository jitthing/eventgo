import { getEvent } from "@/lib/api";
import { formatDate } from "@/lib/utils";
import Image from "next/image";
import { notFound } from "next/navigation";
import SeatSelection from "@/components/SeatSelection";

interface EventPageProps {
	params: { eventId: string };
}

// Fetch event details
export default async function EventPage({ params }: EventPageProps) {
	const eventId = Number(params.eventId);

	if (isNaN(eventId)) {
		return notFound();
	}

	try {
		const event = await getEvent(eventId);

		return (
			<div className="min-h-screen bg-white py-12 px-4 sm:px-6 lg:px-8">
				<div className="max-w-6xl mx-auto grid grid-cols-1 md:grid-cols-2 gap-10">
					{/* Event Image */}
					<div className="relative w-full h-96 md:h-[500px] rounded-lg overflow-hidden">
						<Image src={event.image_url} alt={event.title} fill className="object-cover" />
					</div>

					{/* Event Details */}
					<div className="p-6 bg-white shadow-lg rounded-lg">
						<h1 className="text-4xl font-bold text-black">{event.title}</h1>
						<p className="text-lg text-gray-700 mt-2">{event.description}</p>

						<div className="mt-6 space-y-3 text-gray-900 text-lg">
							<p>
								📅 <strong>Date:</strong> {formatDate(event.date)}
							</p>
							<p>
								📍 <strong>Location:</strong> {event.location}
							</p>
							<p>
								🏟️ <strong>Venue:</strong> {event.venue}
							</p>
							<p>
								🎟️ <strong>Available Seats:</strong> {event.capacity}
							</p>
						</div>

						<div className="mt-6">
							<p className="text-2xl font-semibold text-black">
								🎫 Ticket Price: <span className="text-blue-600">${event.price}</span>
							</p>
						</div>

						{/* Seat Selection Component */}
						<SeatSelection capacity={event.capacity} eventId={event.id} />
					</div>
				</div>
			</div>
		);
	} catch (error) {
		return notFound();
	}
}
