"use client";

import { useState, useEffect } from "react";
import { useRouter } from "next/navigation";
import { loginUser } from "@/lib/auth";

export default function LoginPage() {
	const router = useRouter();
	const [email, setEmail] = useState("");
	const [password, setPassword] = useState("");
	const [error, setError] = useState("");

	useEffect(() => {
		if (localStorage.getItem("token")) {
			router.push("/profile"); // Redirect if already logged in
		}
	}, [router]);

	async function handleLogin(event: React.FormEvent) {
		event.preventDefault();
		setError("");

		try {
			const { access_token } = await loginUser(email, password);
			localStorage.setItem("token", access_token);
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
				<p className="text-black mt-4">
					Don't have an account?{" "}
					<a href="/register" className="text-blue-600 hover:text-blue-800">
						Sign up here
					</a>
				</p>
			</div>
		</div>
	);
}
