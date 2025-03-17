"use client";

import { useEffect, useState } from "react";
import { useParams, notFound, useRouter } from "next/navigation";
import { getEvent } from "@/lib/api";
import { formatDate } from "@/lib/utils";
import Image from "next/image";
import SeatSelection from "@/components/SeatSelection";
import BackButton from "@/components/BackButton";
import Link from "next/link";

export default function EventPage() {
  const params = useParams();
  const router = useRouter();
  const eventId = Number(params.eventId);

  const [event, setEvent] = useState<any | null>(null);
  const [error, setError] = useState(false);
  const [selectedSeats, setSelectedSeats] = useState<number[]>([]);
  const [isGroupBookingOpen, setIsGroupBookingOpen] = useState(false);

  useEffect(() => {
    if (!isNaN(eventId)) {
      getEvent(eventId)
        .then((data) => setEvent(data))
        .catch(() => setError(true));
    } else {
      setError(true);
    }
  }, [eventId]);

  const toggleSeat = (seatId: number) => {
    setSelectedSeats((prev) =>
      prev.includes(seatId)
        ? prev.filter((s) => s !== seatId)
        : [...prev, seatId]
    );
  };

  const openGroupBooking = () => {
    if (selectedSeats.length >= 2) {
      setIsGroupBookingOpen(true);
    }
  };

  const closeGroupBooking = () => {
    setIsGroupBookingOpen(false);
  };

  if (error) return notFound();
  if (!event) return <p className="text-center text-black">Loading event...</p>;

  return (
    <div className="relative min-h-screen bg-white py-12 px-4 sm:px-6 lg:px-8">
      <BackButton />

      <div className="max-w-6xl mx-auto grid grid-cols-1 md:grid-cols-2 gap-10">
        <div className="relative w-full h-96 md:h-[500px] rounded-lg overflow-hidden">
          <Image
            src={event.image_url}
            alt={event.title}
            fill
            className="object-cover"
          />
        </div>

        <div className="p-6 bg-white shadow-lg rounded-lg">
          <h1 className="text-4xl font-bold text-black">{event.title}</h1>
          <p className="text-lg text-gray-700 mt-2">{event.description}</p>

          <div className="mt-6 space-y-3 text-gray-900 text-lg">
            <p>
              📅 <strong>Date:</strong> {formatDate(event.date)}
            </p>
            <p>
              📍 <strong>Location:</strong> {event.location}
            </p>
            <p>
              🏟️ <strong>Venue:</strong> {event.venue}
            </p>
            <p>
              🎟️ <strong>Available Seats:</strong> {event.capacity}
            </p>
          </div>

          {/* Seat Selection Component */}
          <SeatSelection
            eventId={event.event_id}
            selectedSeats={selectedSeats}
            toggleSeat={toggleSeat}
          />

          {/* Ticket Buttons
		  >> to change the total to be dynamically rendered
			added placeholder to test UI first
		  */}
          <div className="mt-6 space-y-3">
            <Link
              href={`/stripe-payment?eventId=${eventId}&seats=${selectedSeats.join(
                ","
              )}&total=100`}
              className={`block text-center bg-blue-600 text-white font-medium py-3 px-8 rounded-md 
							hover:bg-blue-700 transition-colors ${
                selectedSeats.length === 0
                  ? "opacity-50 pointer-events-none"
                  : ""
              }`}
            >
              Buy Tickets
            </Link>

            <button
              onClick={openGroupBooking}
              className={`block w-full text-center bg-green-600 text-white font-medium py-3 px-8 rounded-md 
							hover:bg-green-700 transition-colors ${
                selectedSeats.length < 2 ? "opacity-50 cursor-not-allowed" : ""
              }`}
              disabled={selectedSeats.length < 2}
            >
              Buy Tickets as A Group (Split Payment)
            </button>
          </div>
        </div>
      </div>

      {/* Group Booking Modal */}
      {isGroupBookingOpen && (
        <GroupBookingModal
          selectedSeats={selectedSeats}
          eventId={eventId}
          onClose={closeGroupBooking}
        />
      )}
    </div>
  );
}
function GroupBookingModal({
  selectedSeats,
  eventId,
  onClose,
}: {
  selectedSeats: number[];
  eventId: number;
  onClose: () => void;
}) {
  const router = useRouter();
  const [coBookers, setCoBookers] = useState<string[]>([]);
  const [email, setEmail] = useState("");
  const [seatAssignments, setSeatAssignments] = useState<{
    [seatId: number]: string;
  }>({});

  const totalCoBookersNeeded = selectedSeats.length - 1; // User is already one of the ticket holders
  const remainingCoBookers = totalCoBookersNeeded - coBookers.length;

  // Get all assigned users
  const assignedUsers = new Set(Object.values(seatAssignments));

  const addCoBooker = () => {
    if (
      email &&
      !coBookers.includes(email) &&
      coBookers.length < totalCoBookersNeeded
    ) {
      setCoBookers([...coBookers, email]);
      setEmail("");
    }
  };

  const removeCoBooker = (emailToRemove: string) => {
    setCoBookers(coBookers.filter((email) => email !== emailToRemove));

    // Remove assignments of this co-booker
    setSeatAssignments((prev) => {
      const updatedAssignments = { ...prev };
      Object.keys(updatedAssignments).forEach((seatId) => {
        if (updatedAssignments[Number(seatId)] === emailToRemove) {
          updatedAssignments[Number(seatId)] = "";
        }
      });
      return updatedAssignments;
    });
  };

  const assignSeat = (seatId: number, email: string) => {
    setSeatAssignments((prev) => {
      const updatedAssignments = { ...prev };

      // Remove previous assignment for this user if it exists elsewhere
      Object.keys(updatedAssignments).forEach((key) => {
        if (updatedAssignments[Number(key)] === email) {
          updatedAssignments[Number(key)] = "";
        }
      });

      updatedAssignments[seatId] = email;
      return updatedAssignments;
    });
  };

  const isAllSeatsAssigned = selectedSeats.every(
    (seatId) => seatAssignments[seatId]
  );

  const proceedToCheckout = () => {
    if (!isAllSeatsAssigned) return;

    const coBookersQuery = encodeURIComponent(coBookers.join(","));
    const seatAssignmentsQuery = encodeURIComponent(
      JSON.stringify(seatAssignments)
    );

    router.push(
      `/checkout?eventId=${eventId}&seats=${selectedSeats.join(
        ","
      )}&coBookers=${coBookersQuery}&assignments=${seatAssignmentsQuery}`
    );
  };

  return (
    <div className="fixed inset-0 flex items-center justify-center bg-black bg-opacity-50 z-50">
      <div className="bg-white p-8 rounded-lg shadow-lg w-[500px] max-w-full">
        <h2 className="text-2xl font-bold text-black text-center">
          Split Payment Booking
        </h2>
        <p className="text-black text-center mt-2 font-medium">
          You have selected <strong>{selectedSeats.length}</strong> tickets.
          <br />
          You need to add <strong>{remainingCoBookers}</strong> more co-bookers.
        </p>

        {/* Input for Co-Bookers */}
        <div className="mt-6">
          <label className="text-black font-semibold">
            Enter co-booker email:
          </label>
          <div className="flex mt-2">
            <input
              type="email"
              placeholder="Enter email..."
              value={email}
              onChange={(e) => setEmail(e.target.value)}
              className="w-full p-2 border rounded text-black"
            />
            <button
              onClick={addCoBooker}
              className={`ml-2 py-2 px-4 rounded ${
                !email || coBookers.length >= totalCoBookersNeeded
                  ? "bg-gray-400 cursor-not-allowed text-white"
                  : "bg-blue-600 text-white hover:bg-blue-700"
              }`}
              disabled={!email || coBookers.length >= totalCoBookersNeeded}
            >
              Add
            </button>
          </div>
        </div>

        {/* List of Co-Bookers */}
        <div className="mt-4">
          <h3 className="text-black font-semibold mb-2">Co-Bookers List:</h3>
          <ul>
            {coBookers.map((coBooker) => (
              <li
                key={coBooker}
                className="flex justify-between items-center bg-gray-100 p-3 rounded-lg mb-2"
              >
                <span className="text-black">{coBooker}</span>
                <button
                  onClick={() => removeCoBooker(coBooker)}
                  className="text-red-500"
                >
                  Remove
                </button>
              </li>
            ))}
          </ul>
        </div>

        {/* Seat Assignment */}
        <div className="mt-6">
          <h3 className="text-black font-semibold">Assign Seats:</h3>
          {selectedSeats.map((seatId) => {
            const seatLabel = `A${seatId}`; // Replace this with the actual seat label from your API if needed

            return (
              <div
                key={seatId}
                className="flex justify-between items-center bg-gray-100 p-3 rounded-lg mb-2"
              >
                <span className="text-black font-medium">Seat {seatLabel}</span>
                <select
                  className="p-2 border rounded text-black"
                  value={seatAssignments[seatId] || ""}
                  onChange={(e) => assignSeat(seatId, e.target.value)}
                >
                  <option value="">Select co-booker</option>
                  <option
                    value="You"
                    disabled={
                      assignedUsers.has("You") &&
                      seatAssignments[seatId] !== "You"
                    }
                  >
                    You
                  </option>
                  {coBookers.map((coBooker) => (
                    <option
                      key={coBooker}
                      value={coBooker}
                      disabled={
                        assignedUsers.has(coBooker) &&
                        seatAssignments[seatId] !== coBooker
                      }
                    >
                      {coBooker}
                    </option>
                  ))}
                </select>
              </div>
            );
          })}
        </div>

        {/* Modal Buttons */}
        <div className="mt-6 flex justify-between">
          <button
            onClick={onClose}
            className="bg-red-600 text-white py-2 px-4 rounded hover:bg-red-700"
          >
            Cancel
          </button>
          <button
            onClick={proceedToCheckout}
            className={`py-2 px-4 rounded text-white ${
              isAllSeatsAssigned
                ? "bg-green-600 hover:bg-green-700"
                : "bg-green-400 cursor-not-allowed opacity-90"
            }`}
            disabled={!isAllSeatsAssigned}
          >
            Proceed to Checkout
          </button>
        </div>
      </div>
    </div>
  );
}
