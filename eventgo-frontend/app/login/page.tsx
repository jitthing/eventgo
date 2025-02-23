"use client";

import { useState, useEffect } from "react";
import { useRouter } from "next/navigation";
import { loginUser, fetchUser } from "@/lib/auth";

export default function LoginPage() {
	const router = useRouter();
	const [email, setEmail] = useState("");
	const [password, setPassword] = useState("");
	const [error, setError] = useState("");

	useEffect(() => {
		async function checkSession() {
			try {
				await fetchUser();
				router.push("/profile"); // NEW: Redirect if already authenticated
			} catch {}
		}
		checkSession();
	}, [router]);

	async function handleLogin(event: React.FormEvent) {
		event.preventDefault();
		setError("");

		try {
			// Call loginUser; the backend sets the HTTP-only cookie automatically
			await loginUser(email, password);
			// No need to save any token manually
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
