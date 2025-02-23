"use client";

import { useRouter } from "next/navigation";
import { ArrowLeft } from "lucide-react";

export default function BackButton() {
	const router = useRouter();

	return (
		<div className="absolute top-4 left-44">
			<button onClick={() => router.back()} className="flex items-center space-x-2 text-blue-600 hover:text-blue-800 font-medium transition-colors">
				<ArrowLeft className="w-5 h-5" />
				<span>Back</span>
			</button>
		</div>
	);
}
