export default async function EventPage({ params }: { params: { id: string } }) {
	try {
		const event = await eventsAPI.getEvent(parseInt(params.id));
		const tickets = await ticketsAPI.getTicketsForEvent(event.id);

		return (
			<div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-12 bg-black text-white">
				<div className="grid grid-cols-1 md:grid-cols-2 gap-8">
					<div>
						<div className="relative h-64 mb-6 rounded-lg overflow-hidden">
							<Image src={event.image_url} alt={event.title} fill className="object-cover" />
						</div>
						<h1 className="text-3xl font-bold mb-4">{event.title}</h1>
						<p className="mb-4">{event.description}</p>
						<div className="space-y-2">
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
					<div>
						<h2 className="text-2xl font-bold mb-4">Available Tickets</h2>
						<div className="space-y-4">
							{tickets
								.filter((ticket) => ticket.status === "available")
								.map((ticket) => (
									<div key={ticket.id} className="border rounded-lg p-4 flex justify-between items-center">
										<div>
											<p className="font-medium">Ticket #{ticket.id}</p>
											<p>${ticket.price.toFixed(2)}</p>
										</div>
										<form
											action={async () => {
												"use server";
												await ticketsAPI.purchaseTicket(ticket.id);
											}}
										>
											<button type="submit" className="bg-blue-600 text-white px-4 py-2 rounded-md hover:bg-blue-700 transition-colors">
												Purchase
											</button>
										</form>
									</div>
								))}
						</div>
					</div>
				</div>
			</div>
		);
	} catch (error) {
		notFound();
	}
}
