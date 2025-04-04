"use client";

import { SkeletonBox } from "./Skeleton";

export default function ProfileSkeleton() {
	return (
		<div className="min-h-screen bg-gray-50 p-8">
			<div className="mx-auto space-y-8">
				{/* Profile Header */}
				<div className="bg-white rounded-lg shadow-lg p-8 space-y-6">
					<div className="flex items-center space-x-4">
						<SkeletonBox width={64} height={64} className="rounded-full" />
						<div className="space-y-2">
							<SkeletonBox width="48" height={24} />
							<SkeletonBox width="64" height={16} />
						</div>
					</div>
				</div>

				{/* Tickets Section */}
				<div className="bg-white rounded-lg shadow-lg p-8 space-y-4">
					<SkeletonBox width="48" height={24} />
					{[...Array(3)].map((_, i) => (
						<div key={i} className="space-y-2">
							<SkeletonBox width="full" height={16} />
							<SkeletonBox width="3/4" height={16} />
						</div>
					))}
				</div>
			</div>
		</div>
	);
}
