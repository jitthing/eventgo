"use client";

import { useState, useEffect } from "react";
import { useRouter } from "next/navigation";
import { registerUser, loginUser, fetchUser } from "@/lib/auth";
import { useAuth } from "@/context/AuthContext"; // ✅ Import AuthContext

export default function RegisterPage() {
	const router = useRouter();
	const { setUser } = useAuth(); // ✅ Get setUser from AuthContext
	const [email, setEmail] = useState("");
	const [password, setPassword] = useState("");
	const [confirmPassword, setConfirmPassword] = useState("");
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

	const [fullName, setFullName] = useState("");

	async function handleRegister(event: React.FormEvent) {
		event.preventDefault();
		setError("");

		if (password !== confirmPassword) {
			setError("Passwords do not match.");
			return;
		}

		try {
			await registerUser(email, password, fullName);
			await loginUser(email, password);
			const userData = await fetchUser(); // ✅ Fetch user after login
			setUser(userData); // ✅ Update auth state with full_name and role
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
					<input type="text" className="w-full p-3 border rounded text-black" placeholder="Full Name" value={fullName} onChange={(e) => setFullName(e.target.value)} required />
					<input type="email" className="w-full p-3 border rounded text-black" placeholder="Email" value={email} onChange={(e) => setEmail(e.target.value)} required />
					<input type="password" className="w-full p-3 border rounded text-black" placeholder="Password" value={password} onChange={(e) => setPassword(e.target.value)} required />
					<input type="password" className="w-full p-3 border rounded text-black" placeholder="Confirm Password" value={confirmPassword} onChange={(e) => setConfirmPassword(e.target.value)} required />
					<button type="submit" className="w-full bg-blue-600 text-white p-3 rounded">
						Register
					</button>
				</form>
			</div>
		</div>
	);
}
