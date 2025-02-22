"use client";

import { useState, useEffect } from "react";
import { useRouter } from "next/navigation";
import { registerUser, loginUser } from "@/lib/auth";

export default function RegisterPage() {
	const router = useRouter();
	const [email, setEmail] = useState("");
	const [password, setPassword] = useState("");
	const [confirmPassword, setConfirmPassword] = useState("");
	const [error, setError] = useState("");

	useEffect(() => {
		if (localStorage.getItem("token")) {
			router.push("/profile"); // Redirect if already logged in
		}
	}, [router]);

	async function handleRegister(event: React.FormEvent) {
		event.preventDefault();
		setError("");

		if (password !== confirmPassword) {
			setError("Passwords do not match.");
			return;
		}

		try {
			// Register user
			await registerUser(email, password);

			// Auto-login after registration
			const { access_token } = await loginUser(email, password);
			localStorage.setItem("token", access_token);
			router.push("/profile");
		} catch (err: any) {
			setError(err.message || "Failed to register.");
		}
	}

	return (
		<div className="min-h-screen bg-white flex items-center justify-center">
			<div className="max-w-md w-full bg-white shadow-md rounded-lg p-6">
				<h1 className="text-2xl font-bold text-black mb-4">Create an Account</h1>
				{error && <p className="text-red-600">{error}</p>}
				<form onSubmit={handleRegister} className="space-y-4">
					<input type="email" className="w-full p-3 border rounded text-black" placeholder="Email" value={email} onChange={(e) => setEmail(e.target.value)} required />
					<input type="password" className="w-full p-3 border rounded text-black" placeholder="Password" value={password} onChange={(e) => setPassword(e.target.value)} required />
					<input type="password" className="w-full p-3 border rounded text-black" placeholder="Confirm Password" value={confirmPassword} onChange={(e) => setConfirmPassword(e.target.value)} required />
					<button type="submit" className="w-full bg-blue-600 text-white p-3 rounded">
						Register
					</button>
				</form>
				<p className="text-black mt-4">
					Already have an account?{" "}
					<a href="/login" className="text-blue-600 hover:text-blue-800">
						Login here
					</a>
				</p>
			</div>
		</div>
	);
}
