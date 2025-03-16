"use client";

export default function ConfirmModal({ title, message, onConfirm, onCancel }: { title: string; message: string; onConfirm: () => void; onCancel: () => void }) {
	return (
		<div className="fixed inset-0 flex items-center justify-center bg-black bg-opacity-50 z-50">
			<div className="bg-white p-6 rounded-lg shadow-lg w-[400px] max-w-full">
				<h2 className="text-2xl font-bold text-black">{title}</h2>
				<p className="text-black mt-2">{message}</p>

				<div className="mt-6 flex justify-between">
					<button onClick={onCancel} className="bg-gray-500 text-white py-2 px-4 rounded hover:bg-gray-600">
						Cancel
					</button>
					<button onClick={onConfirm} className="bg-red-600 text-white py-2 px-4 rounded hover:bg-red-700">
						Confirm
					</button>
				</div>
			</div>
		</div>
	);
}
