export const mockOrders = [
  {
    id: "ORD123456",
    eventId: 1,
    eventTitle: "Taylor Swift - The Eras Tour",
    eventDate: "2024-06-15",
    eventLocation: "MetLife Stadium, NJ",
    seats: [12, 14, 16],
    total: 599.97,
    status: "Upcoming",
  },
  {
    id: "ORD789012",
    eventId: 2,
    eventTitle: "NBA Finals 2024",
    eventDate: "2024-02-20",
    eventLocation: "Madison Square Garden, NY",
    seats: [8, 10],
    total: 499.98,
    status: "Completed",
  },
];

export async function getUserOrders() {
  return new Promise((resolve) => {
    setTimeout(() => resolve(mockOrders), 500); // Simulate API delay
  });
}
