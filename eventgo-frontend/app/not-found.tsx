export default function NotFoundPage() {
	return (
		<div className="min-h-screen flex flex-col items-center justify-center bg-gray-50 p-8">
			<h1 className="text-6xl font-bold text-red-600 mb-4">404</h1>
			<p className="text-2xl text-gray-700 mb-8">Unauthorized — Page Not Found</p>
			<a href="/" className="px-6 py-3 bg-blue-600 text-white rounded hover:bg-blue-700 transition">
				Go Home
			</a>
		</div>
	);
}
