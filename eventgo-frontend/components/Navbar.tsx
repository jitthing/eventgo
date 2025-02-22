"use client";

import Link from "next/link";
import { useEffect, useState } from "react";

const Navbar = () => {
	const [token, setToken] = useState<string | null>(null);

	useEffect(() => {
		setToken(localStorage.getItem("token"));
	}, []);

	return (
		<nav className="bg-white shadow-lg">
			<div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
				<div className="flex justify-between h-16">
					<div className="flex items-center">
						<Link href="/" className="text-2xl font-bold text-blue-600">
							🎟️ EventGo
						</Link>
					</div>

					<div className="hidden md:flex items-center space-x-8">
						{/* <Link href="/events" className="text-black hover:text-blue-600 font-medium">
							Events
						</Link> */}
						{token ? (
							<Link href="/profile">
								<img src="https://api.dicebear.com/6.x/initials/svg?seed=User" alt="Profile" className="w-10 h-10 rounded-full border border-gray-300" />
							</Link>
						) : (
							<>
								<Link href="/login" className="text-black hover:text-blue-600 font-medium">
									Login
								</Link>
								<Link href="/register" className="text-black hover:text-blue-600 font-medium">
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
