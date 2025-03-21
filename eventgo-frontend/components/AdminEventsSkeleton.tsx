"use client";

export default function AdminEventsSkeleton() {
	return (
		<div className="min-h-screen bg-white py-16 px-4 max-w-7xl mx-auto">
			<header className="text-center mb-12">
				<div className="h-12 bg-gray-200 rounded w-64 mx-auto mb-2" />
				<div className="h-6 bg-gray-200 rounded w-48 mx-auto" />
			</header>

			<section className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6">
				{Array.from({ length: 8 }).map((_, i) => (
					<div key={i} className="bg-white rounded-lg shadow-md overflow-hidden">
						<div className="h-48 bg-gray-200" />
						<div className="p-4 space-y-2">
							<div className="h-4 bg-gray-200 rounded w-3/4" />
							<div className="h-4 bg-gray-200 rounded w-1/2" />
							<div className="h-4 bg-gray-200 rounded w-2/3" />
							<div className="mt-4 space-y-2">
								<div className="h-8 bg-gray-200 rounded" />
								<div className="h-8 bg-gray-200 rounded" />
							</div>
						</div>
					</div>
				))}
			</section>
		</div>
	);
}
