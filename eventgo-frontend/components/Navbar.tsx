"use client";

import Link from "next/link";
import { useAuth } from "@/context/AuthContext";
import { useRouter } from "next/navigation";

const Navbar = () => {
	const router = useRouter();
	const { user, loading } = useAuth();

	return (
		<nav className="bg-white shadow-lg">
			<div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
				<div className="flex justify-between items-center h-16">
					{/* Brand Logo */}
					<Link href="/" className="text-2xl font-bold text-black flex items-center space-x-2">
						<span role="img" aria-label="ticket">
							🎟️
						</span>
						<span>EventGo</span>
					</Link>

					{/* Authentication & Profile */}
					<div className="flex items-center space-x-6">
						{loading ? (
							<div className="w-10 h-10 bg-gray-200 rounded-full animate-pulse" />
						) : user ? (
							<button onClick={() => router.push("/profile")} className="flex items-center">
								<img src={`https://api.dicebear.com/6.x/initials/svg?seed=${encodeURIComponent(user?.full_name || "User")}`} alt="Profile Avatar" className="w-10 h-10 rounded-full border border-gray-300 cursor-pointer" />{" "}
							</button>
						) : (
							<>
								<Link href="/login" className="text-black font-medium hover:text-blue-600 transition">
									Login
								</Link>
								<Link href="/register" className="text-black font-medium hover:text-blue-600 transition">
									Sign Up
								</Link>
							</>
						)}
					</div>
				</div>
			</div>
		</nav>
	);
};

export default Navbar;
