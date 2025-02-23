"use client";

import Link from "next/link";
import { useEffect, useState } from "react";
import { fetchUser, logoutUser } from "@/lib/auth";
import { useRouter } from "next/navigation";

const Navbar = () => {
	const router = useRouter();
	const [user, setUser] = useState<any>(null);

	useEffect(() => {
		async function checkAuth() {
			try {
				const userData = await fetchUser();
				setUser(userData);
			} catch {
				setUser(null);
			}
		}
		checkAuth();
	}, []);

	const handleLogout = async () => {
		await logoutUser();
		router.push("/login");
	};

	return (
		<nav className="bg-white shadow-lg">
			<div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
				<div className="flex justify-between items-center h-16">
					{" "}
					{/* ✅ Fix alignment */}
					{/* Brand Logo */}
					<Link href="/" className="text-2xl font-bold text-black flex items-center space-x-2">
						<span role="img" aria-label="ticket">
							🎟️
						</span>
						<span>EventGo</span>
					</Link>
					{/* Navigation Links */}
					<div className="flex items-center space-x-6">
						{" "}
						{/* ✅ Added flex alignment */}
						{user ? (
							<button onClick={handleLogout} className="text-black font-medium hover:text-blue-600 transition">
								Logout
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
