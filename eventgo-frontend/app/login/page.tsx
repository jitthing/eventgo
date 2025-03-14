"use client";

import { useState, useEffect } from "react";
import { useRouter } from "next/navigation";
import { loginUser, fetchUser, debugCookies } from "@/lib/auth";
import { useAuth } from "@/context/AuthContext"; // ✅ Import AuthContext

export default function LoginPage() {
	const router = useRouter();
	const { setUser } = useAuth(); // ✅ Get setUser from AuthContext
	const [email, setEmail] = useState("");
	const [password, setPassword] = useState("");
	const [error, setError] = useState("");

	useEffect(() => {
		async function checkSession() {
			try {
				const userData = await fetchUser();
				setUser(userData); // ✅ Ensure global state updates if already authenticated
				router.push("/profile");
			} catch {}
		}
		checkSession();
	}, [router, setUser]);

	async function handleLogin(event: React.FormEvent) {
		event.preventDefault();
		setError("");

		try {
			await loginUser(email, password);
			const userData = await fetchUser(); // ✅ Fetch user after login
			setUser(userData); // ✅ Update global auth state
			router.push("/profile");
		} catch (err: any) {
			setError(err.message || "Failed to login.");
		}
	}

	return (
		<div className="min-h-screen bg-white flex items-center justify-center">
			<div className="max-w-md w-full bg-white shadow-md rounded-lg p-6">
				<h1 className="text-2xl font-bold text-black mb-4">Login</h1>
				{error && <p className="text-red-600">{error}</p>}
				<form onSubmit={handleLogin} className="space-y-4">
					<input type="email" className="w-full p-3 border rounded text-black" placeholder="Email" value={email} onChange={(e) => setEmail(e.target.value)} required />
					<input type="password" className="w-full p-3 border rounded text-black" placeholder="Password" value={password} onChange={(e) => setPassword(e.target.value)} required />
					<button type="submit" className="w-full bg-blue-600 text-white p-3 rounded">
						Login
					</button>
				</form>
			</div>
		</div>
	);
}
