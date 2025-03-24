"use client";

export default function EventSkeleton() {
	return (
		<div className="min-h-screen bg-white py-12 px-4 sm:px-6 lg:px-8">
			<div className="max-w-6xl mx-auto grid grid-cols-1 md:grid-cols-2 gap-10">
				{/* Image placeholder */}
				<div className="h-96 md:h-[500px] bg-gray-200 rounded-lg" />

				{/* Details placeholder */}
				<div className="space-y-6">
					<div className="h-8 bg-gray-200 rounded w-3/4" />
					<div className="h-4 bg-gray-200 rounded w-full" />
					<div className="h-4 bg-gray-200 rounded w-5/6" />
					<div className="h-4 bg-gray-200 rounded w-2/3" />

					{/* SeatSelection placeholder */}
					<div className="h-48 bg-gray-200 rounded-lg" />

					{/* Buttons placeholder */}
					<div className="space-y-2 mt-4">
						<div className="h-10 bg-gray-200 rounded" />
						<div className="h-10 bg-gray-200 rounded w-1/2" />
					</div>
				</div>
			</div>
		</div>
	);
}
