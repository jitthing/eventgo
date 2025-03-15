"use client";

import { useEffect, useState } from "react";
import { useRouter } from "next/navigation";
import { fetchUser, logoutUser } from "@/lib/auth";
import { getUserOrders } from "@/lib/mock-orders";
import Link from "next/link";
import { useAuth } from "@/context/AuthContext"; // ✅ Import AuthContext

export default function ProfilePage() {
	const router = useRouter();
	const { user, setUser } = useAuth(); // ✅ Get user and setUser from AuthContext
	const [orders, setOrders] = useState<any[]>([]);
	const [loading, setLoading] = useState(true);

	useEffect(() => {
		async function fetchData() {
			try {
				if (!user) {
					const userData = await fetchUser();
					setUser(userData); // ✅ Only fetch if user isn't in context
				}
				const ordersData: any[] = await getUserOrders();
				setOrders(ordersData);
				setLoading(false);
			} catch {
				router.push("/login");
			}
		}
		fetchData();
	}, [router, user, setUser]);

	const upcomingEvents = orders.filter((order) => order.status === "Upcoming");
	const pastEvents = orders.filter((order) => order.status === "Completed");

	async function handleLogout() {
		try {
			await logoutUser();
			setUser(null); // ✅ Update global state
			router.replace("/login"); // ✅ Use replace to prevent going back
		} catch (err) {
			console.error("Logout error:", err);
			router.replace("/login");
		}
	}

	return (
		<div className="min-h-screen bg-white py-12 px-4 sm:px-6 lg:px-8">
			<div className="max-w-4xl mx-auto bg-white shadow-lg rounded-lg p-8">
				{/* User Profile Section */}
				<div className="flex items-center space-x-4">
					<img src={user?.avatar || "https://api.dicebear.com/6.x/initials/svg?seed=User"} alt="Profile Avatar" className="w-16 h-16 rounded-full" />
					<div>
						<h1 className="text-3xl font-bold text-black">{user?.email}</h1>
						<p className="text-black">User ID: {user?.id}</p>
					</div>
				</div>

				{/* Settings */}
				<div className="mt-6 p-4 bg-gray-100 rounded-lg">
					<h3 className="text-lg font-semibold text-black">⚙️ Settings</h3>
					<ul className="text-black space-y-2 mt-2">
						<li>
							<Link href="#" className="text-blue-600 hover:text-blue-800">
								Change Password
							</Link>
						</li>
						<li>
							<Link href="#" className="text-blue-600 hover:text-blue-800">
								Manage Notifications
							</Link>
						</li>
						<li>
							<Link href="#" className="text-blue-600 hover:text-blue-800">
								Privacy Settings
							</Link>
						</li>
					</ul>
				</div>

				{/* Upcoming Events Section */}
				<div className="mt-8">
					<h3 className="text-2xl font-bold text-black">🎟️ Upcoming Events</h3>
					<p className="text-gray-600">These are your upcoming events.</p>
					{loading ? (
						<p className="text-black mt-4">Loading...</p>
					) : upcomingEvents.length === 0 ? (
						<p className="text-black mt-4">No upcoming events.</p>
					) : (
						<div className="space-y-6 mt-4">
							{upcomingEvents.map((order) => (
								<div key={order.id} className="p-6 bg-white border border-gray-200 shadow-md rounded-lg">
									<h4 className="text-xl font-semibold text-black">{order.eventTitle}</h4>
									<p className="text-lg text-black mt-1">📅 {order.eventDate}</p>
									<p className="text-black">📍 {order.eventLocation}</p>
									<p className="text-lg font-medium mt-2 text-black">🎟️ Seats: {order.seats.join(", ")}</p>
									<p className="text-lg font-semibold text-blue-600 mt-2">💲Total Paid: ${order.total.toFixed(2)}</p>
									<Link href={`/events/${order.eventId}`} className="mt-3 inline-block text-blue-600 hover:text-blue-800 font-medium">
										View Event Details
									</Link>
								</div>
							))}
						</div>
					)}
				</div>

				{/* Past Events Section */}
				<div className="mt-8">
					<h3 className="text-2xl font-bold text-black">📜 Past Events</h3>
					<p className="text-gray-600">Your completed events history.</p>
					{loading ? (
						<p className="text-black mt-4">Loading...</p>
					) : pastEvents.length === 0 ? (
						<p className="text-black mt-4">No past events.</p>
					) : (
						<div className="space-y-6 mt-4">
							{pastEvents.map((order) => (
								<div key={order.id} className="p-6 bg-white border border-gray-200 shadow-md rounded-lg">
									<h4 className="text-xl font-semibold text-black">{order.eventTitle}</h4>
									<p className="text-lg text-black mt-1">📅 {order.eventDate}</p>
									<p className="text-black">📍 {order.eventLocation}</p>
									<p className="text-lg font-medium mt-2 text-black">🎟️ Seats: {order.seats.join(", ")}</p>
									<p className="text-lg font-semibold text-green-600 mt-2">✅ Completed</p>
									<Link href={`/events/${order.eventId}`} className="mt-3 inline-block text-blue-600 hover:text-blue-800 font-medium">
										View Event Details
									</Link>
								</div>
							))}
						</div>
					)}
				</div>

				{/* Logout */}
				<div className="mt-10 text-center">
					<button onClick={handleLogout} className="bg-red-600 text-white py-3 px-6 rounded-md hover:bg-red-700 transition-colors">
						Log Out
					</button>
				</div>
			</div>
		</div>
	);
}
