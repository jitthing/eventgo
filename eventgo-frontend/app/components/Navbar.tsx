"use client";

import Link from "next/link";
import { useState } from "react";

const Navbar = () => {
	const [isOpen, setIsOpen] = useState(false);

	return (
		<nav className="bg-white shadow-lg">
			<div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
				<div className="flex justify-between h-16">
					<div className="flex items-center">
						<Link href="/" className="text-2xl font-bold text-blue-600">
							🎟️ EventGo
						</Link>
					</div>

					{/* Desktop Navigation */}
					<div className="hidden md:flex items-center space-x-8">
						<Link href="/events" className="text-black hover:text-blue-600 font-medium">
							Events
						</Link>
						<Link href="/concerts" className="text-black hover:text-blue-600 font-medium">
							Concerts
						</Link>
						<Link href="/sports" className="text-black hover:text-blue-600 font-medium">
							Sports
						</Link>
						<Link href="/login" className="text-black hover:text-blue-600 font-medium">
							Login
						</Link>
					</div>

					{/* Mobile menu button */}
					<div className="md:hidden flex items-center">
						<button onClick={() => setIsOpen(!isOpen)} className="text-gray-700 hover:text-blue-600">
							<svg className="h-6 w-6" fill="none" viewBox="0 0 24 24" stroke="currentColor">
								{isOpen ? <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M6 18L18 6M6 6l12 12" /> : <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M4 6h16M4 12h16M4 18h16" />}
							</svg>
						</button>
					</div>
				</div>
			</div>

			{/* Mobile Navigation */}
			{isOpen && (
				<div className="md:hidden">
					<div className="px-2 pt-2 pb-3 space-y-1 sm:px-3">
						<Link href="/events" className="block px-3 py-2 text-black hover:text-blue-600 font-medium">
							Events
						</Link>
						<Link href="/concerts" className="block px-3 py-2 text-black hover:text-blue-600 font-medium">
							Concerts
						</Link>
						<Link href="/sports" className="block px-3 py-2 text-black hover:text-blue-600 font-medium">
							Sports
						</Link>
						<Link href="/login" className="block px-3 py-2 text-black hover:text-blue-600 font-medium">
							Login
						</Link>
					</div>
				</div>
			)}
		</nav>
	);
};

export default Navbar;
