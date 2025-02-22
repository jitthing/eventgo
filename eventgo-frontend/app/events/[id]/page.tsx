import Image from "next/image";
import Link from "next/link";
import { notFound } from "next/navigation";
import { formatDate } from "@/lib/utils";
import { getEvent, getTicketsForEvent } from "@/lib/api";

export default async function EventPage({ params }: { params: { id: string } }) {
	try {
		const event = await getEvent(parseInt(params.id));
		const tickets = await getTicketsForEvent(event.id);

		return (
			<main className="container mx-auto px-4 py-8">
				<div className="max-w-4xl mx-auto">
					<div className="bg-white rounded-lg shadow-lg overflow-hidden">
						<div className="relative h-64 mb-6 rounded-lg overflow-hidden">
							<Image 
								src={event.image_url} 
								alt={event.title} 
								fill 
								className="object-cover"
								unoptimized={event.image_url.startsWith('http')}
							/>
						</div>
						<div className="p-4">
							<h1 className="text-3xl font-bold mb-4">{event.title}</h1>
							<p className="text-gray-600 mb-2">{event.description}</p>
							<div className="text-sm text-gray-500">
								<p>
									<strong>Date:</strong> {formatDate(event.date)}
								</p>
								<p>
									<strong>Venue:</strong> {event.venue}
								</p>
								<p>
									<strong>Location:</strong> {event.location}
								</p>
								<p>
									<strong>Category:</strong> {event.category}
								</p>
							</div>
						</div>
						<div className="mt-6">
							<Link href={`/events/${params.id}/tickets`} className="inline-block bg-blue-600 text-white px-6 py-3 rounded-lg hover:bg-blue-700 transition-colors">
								Get Tickets
							</Link>
						</div>
					</div>
				</div>
			</main>
		);
	} catch (error) {
		notFound();
	}
}
