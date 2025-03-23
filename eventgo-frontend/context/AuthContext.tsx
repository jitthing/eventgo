"use client";

import { createContext, useContext, useState, useEffect } from "react";
import { fetchUser } from "@/lib/auth";

const AuthContext = createContext<any>(null);

export const AuthProvider = ({ children }: { children: React.ReactNode }) => {
	const [user, setUser] = useState<{ id: number; email: string; full_name: string; role: string } | null>(null);
	const [loading, setLoading] = useState(true);

	useEffect(() => {
		async function loadUser() {
			try {
				const userData = await fetchUser();
				setUser(userData);
			} catch {
				setUser(null);
			} finally {
				setLoading(false);
			}
		}
		loadUser();
	}, []);

	return <AuthContext.Provider value={{ user, setUser, loading }}>{children}</AuthContext.Provider>;
};

export const useAuth = () => useContext(AuthContext);
