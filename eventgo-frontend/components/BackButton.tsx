"use client";

import { useRouter } from "next/navigation";

export default function BackButton() {
	const router = useRouter();

	return (
		<button onClick={() => router.back()} className="bg-gray-300 hover:bg-gray-400 text-black py-2 px-4 rounded-md">
			← Back
		</button>
	);
}
