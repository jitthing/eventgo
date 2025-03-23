import Image from "next/image";
import Link from "next/link";
import { getFeaturedEvents, getTicketsForEvent } from "@/lib/api";
import { formatDate } from "@/lib/utils";
import { Suspense } from "react";
import { Event, Ticket, TicketStatus } from "@/lib/interfaces";

// Loading component for events
function EventsSkeleton() {
	return (
		<div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6">
			{[1, 2, 3, 4].map((i) => (
				<div key={i} className="bg-white rounded-lg shadow-md p-4 animate-pulse">
					<div className="h-48 bg-gray-200 rounded-md mb-4"></div>
					<div className="h-4 bg-gray-200 rounded w-1/4 mb-2"></div>
					<div className="h-6 bg-gray-200 rounded w-3/4 mb-2"></div>
					<div className="h-4 bg-gray-200 rounded w-1/2 mb-2"></div>
					<div className="h-4 bg-gray-200 rounded w-2/3 mb-4"></div>
					<div className="h-10 bg-gray-200 rounded"></div>
				</div>
			))}
		</div>
	);
}

// Fetch and display featured events
async function EventsList() {
	let events: Event[] = [];
	try {
		events = await getFeaturedEvents();
	} catch (error) {
		console.error("Error fetching featured events:", error);
	}

	// Fetch ticket prices for each event
	const eventsWithPrices = await Promise.all(
		events.map(async (event) => {
			try {
				const tickets = await getTicketsForEvent(event.event_id);
				const availableTickets = tickets.filter((ticket) => ticket.status === TicketStatus.AVAILABLE);

				// Get the lowest ticket price
				const lowestPrice = availableTickets.length > 0 ? Math.min(...availableTickets.map((ticket) => ticket.price)) : null;

				return { ...event, price: lowestPrice };
			} catch (error) {
				console.error(`Error fetching tickets for event ${event.event_id}:`, error);
				return { ...event, price: null }; // Handle failure gracefully
			}
		})
	);

	return (
		<div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6">
			{eventsWithPrices.map((event) => (
				<div key={event.event_id} className="bg-white rounded-lg shadow-md overflow-hidden flex flex-col">
					<div className="relative h-48">
						<Image src={event.image_url} alt={event.title} fill className="object-cover" />
					</div>
					<div className="p-4 bg-white flex flex-col flex-grow justify-between">
						<div>
							<span className="text-blue-600 text-sm font-medium">{event.category}</span>
							<h3 className="text-black font-semibold mt-1">{event.title}</h3>
							<p className="text-black mt-1">{formatDate(event.date)}</p>
							<p className="text-black mt-1">{event.venue}</p>
							<p className="text-black font-medium mt-2">{event.price !== null ? `Starting at $${event.price}` : "No tickets available"}</p>
						</div>
						<Link href={`/events/${event.event_id}`} className="mt-4 block text-center bg-blue-600 text-white py-2 px-4 rounded-md hover:bg-blue-700 transition-colors">
							Get Tickets
						</Link>
					</div>
				</div>
			))}
		</div>
	);
}

export default function Home() {
	return (
		<div className="min-h-screen bg-white">
			{/* Hero Section */}
			<section className="bg-blue-600 text-white py-20">
				<div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
					<div className="text-center">
						<h1 className="text-4xl md:text-6xl font-bold mb-6">Find Your Next Event</h1>
						<p className="text-xl md:text-2xl mb-8">Discover amazing concerts, sports events, and more</p>
						<div className="max-w-md mx-auto">
							<input type="text" placeholder="Search events..." className="w-full px-4 py-3 rounded-lg text-black" />
						</div>
					</div>
				</div>
			</section>

			{/* Featured Categories */}
			<section className="py-16 bg-gray-50">
				<div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
					<h2 className="text-3xl font-bold text-center mb-12 text-black">Browse by Category</h2>
					<div className="grid grid-cols-1 md:grid-cols-3 gap-8">
						<div className="bg-white rounded-lg shadow-md p-6 text-center">
							<h3 className="text-xl font-semibold mb-4 text-black">Concerts</h3>
							<p className="text-black mb-4">Find tickets to your favorite artists</p>
							<Link href="/concerts" className="text-blue-600 hover:text-blue-800 font-medium">
								Browse Concerts
							</Link>
						</div>
						<div className="bg-white rounded-lg shadow-md p-6 text-center">
							<h3 className="text-xl font-semibold mb-4 text-black">Sports</h3>
							<p className="text-black mb-4">Get tickets to exciting sports events</p>
							<Link href="/sports" className="text-blue-600 hover:text-blue-800 font-medium">
								Browse Sports
							</Link>
						</div>
						<div className="bg-white rounded-lg shadow-md p-6 text-center">
							<h3 className="text-xl font-semibold mb-4 text-black">Theater</h3>
							<p className="text-black mb-4">Experience the magic of live theater</p>
							<Link href="/theater" className="text-blue-600 hover:text-blue-800 font-medium">
								Browse Theater
							</Link>
						</div>
					</div>
				</div>
			</section>

			{/* Featured Events Section */}
			<section className="py-16 bg-white">
				<div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
					<h2 className="text-3xl font-bold text-black mb-8">Featured Events</h2>
					<Suspense fallback={<EventsSkeleton />}>
						<EventsList />
					</Suspense>
					<div className="text-center mt-12">
						<Link href="/events" className="inline-block bg-white text-blue-600 font-medium py-3 px-8 rounded-md border-2 border-blue-600 hover:bg-blue-600 hover:text-white transition-colors">
							View All Events
						</Link>
					</div>
				</div>
			</section>
		</div>
	);
}
