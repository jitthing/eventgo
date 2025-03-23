"use client";

import { useEffect, useState } from "react";
import { useParams, notFound, useRouter } from "next/navigation";
import { getEvent, getTicketsForEvent } from "@/lib/api"; // import getTicketsForEvent
import { formatEventDuration } from "@/lib/utils";
import Image from "next/image";
import SeatSelection from "@/components/SeatSelection";
import BackButton from "@/components/BackButton";
import Link from "next/link";
import { searchUsers } from "@/lib/auth";
import { useAuth } from "@/context/AuthContext";

export default function EventPage() {
  const params = useParams();
  const router = useRouter();
  const eventId = Number(params.eventId);

  const { user } = useAuth();
  const [event, setEvent] = useState<any | null>(null);
  const [error, setError] = useState(false);
  const [selectedSeats, setSelectedSeats] = useState<number[]>([]);
  const [totalPrice, setTotalPrice] = useState(0);
  const [isGroupBookingOpen, setIsGroupBookingOpen] = useState(false);
  const [availableSeats, setAvailableSeats] = useState<any[]>([]);
  const [reservationId, setReservationId] = useState<string | null>(null);

  useEffect(() => {
    if (!isNaN(eventId)) {
      (async () => {
        try {
          const fetchedEvent = await getEvent(eventId);
          const tickets = await getTicketsForEvent(eventId);

          // Transform tickets to include needed information
          const transformedSeats = tickets.map((ticket: any) => ({
            id: ticket.ticket_id,
            seat_number: ticket.seat_number,
            status: ticket.status,
            price: ticket.price || 0,
          }));

          setAvailableSeats(transformedSeats);

          // Here is the workaround:
          (fetchedEvent as any).capacity = tickets.length;

          setEvent(fetchedEvent);
        } catch {
          setError(true);
        }
      })();
    } else {
      setError(true);
    }
  }, [eventId]);

  const toggleSeat = (seatId: number) => {
    setSelectedSeats((prev) => {
      const newSelectedSeats = prev.includes(seatId)
        ? prev.filter((s) => s !== seatId)
        : [...prev, seatId];

      // Calculate the total price based on selected seats
      const newTotalPrice = newSelectedSeats.reduce((sum, seatId) => {
        const seat = availableSeats.find((s) => s.id === seatId);
        return sum + (seat?.price || 0);
      }, 0);

      setTotalPrice(newTotalPrice);
      return newSelectedSeats;
    });
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
              📅 <strong>Date:</strong> {formatEventDuration(event.date)}
            </p>
            <p>
              📍 <strong>Venue:</strong> {event.venue}
            </p>
            {/* Use the newly set "capacity" field in local state */}
            <p>
              🎟️ <strong>Available Seats:</strong> {event.capacity}
            </p>
          </div>

          {/* Seat Selection Component */}
          <SeatSelection
            eventId={event.event_id}
            selectedSeats={selectedSeats}
            toggleSeat={toggleSeat}
            availableSeats={availableSeats}
          />

          {/* Display total price */}
          {selectedSeats.length > 0 && (
            <div className="mt-4 p-3 bg-blue-50 border border-blue-200 rounded-md">
              <p className="text-gray-900 font-medium">
                Total Price: ${totalPrice.toFixed(2)}
              </p>
              <p className="text-sm text-gray-600">
                For {selectedSeats.length} seat(s)
              </p>
            </div>
          )}

          <div className="mt-6 space-y-3">
            <button
              onClick={async () => {
                if (user) {
                  try {
                    // First, make the reservation
                    const ticketBody = {
                      eventId: eventId,
                      userId: user.id,
                      seats: selectedSeats.map((id) => {
                        const seat = availableSeats.find((s) => s.id === id);
                        return seat?.seat_number || "";
                      }),
                    };
                    
                    const ticketReservation = await fetch("http://localhost:8005/tickets/reserve", {
                      method: "POST",
                      headers: { "Content-Type": "application/json" },
                      body: JSON.stringify(ticketBody),
                    });

                    const reservationData = await ticketReservation.json();
                    const reservationId = reservationData.data.reservation_id;
                    
                    // Save reservationId to localStorage for access in payment page
                    localStorage.setItem("reservationId", reservationId);
                    localStorage.setItem("userId", user.id.toString());
                    
                    // Navigate to payment page with all necessary params
                    router.push(
                      `/payment?eventId=${eventId}&seats=${selectedSeats
                        .map((seatId) => {
                          const seat = availableSeats.find(s => s.id === seatId);
                          return seat?.seat_number || "";
                        })
                        .join(",")}&total=${totalPrice}&reservationId=${reservationId}`
                    );
                  } catch (error) {
                    console.error("Error making reservation:", error);
                    alert("Failed to reserve seats. Please try again.");
                  }
                } else {
                  router.push("/login");
                }
              }}
              className={`block w-full text-center bg-blue-600 text-white font-medium py-3 px-8 rounded-md hover:bg-blue-700 transition-colors ${
                selectedSeats.length === 0
                  ? "opacity-50 pointer-events-none"
                  : ""
              }`}
              disabled={selectedSeats.length === 0}
            >
              Buy Tickets
            </button>

            <button
              onClick={openGroupBooking}
              className={`block w-full text-center bg-green-600 text-white font-medium py-3 px-8 rounded-md 
                hover:bg-green-700 transition-colors ${
                  selectedSeats.length < 2
                    ? "opacity-50 cursor-not-allowed"
                    : ""
                }`}
              disabled={selectedSeats.length < 2}
            >
              Buy Tickets as A Group (Split Payment)
            </button>
          </div>
        </div>
      </div>

      {isGroupBookingOpen && (
        <GroupBookingModal
          selectedSeats={selectedSeats}
          eventId={eventId}
          onClose={closeGroupBooking}
          totalPrice={totalPrice}
          availableSeats={availableSeats}
        />
      )}
    </div>
  );
}

function GroupBookingModal({
  selectedSeats,
  eventId,
  onClose,
  totalPrice,
  availableSeats,
}: {
  selectedSeats: number[];
  eventId: number;
  onClose: () => void;
  totalPrice: number;
  availableSeats: any[];
}) {
  const router = useRouter();
  const { user } = useAuth(); // Get logged-in user
  const [coBookers, setCoBookers] = useState<{ id: number; email: string }[]>(
    []
  );
  const [email, setEmail] = useState("");
  const [errorMsg, setErrorMsg] = useState("");
  const [seatAssignments, setSeatAssignments] = useState<{
    [seatId: number]: string;
  }>({});

  const totalCoBookersNeeded = selectedSeats.length - 1; // User is already one of the ticket holders
  const remainingCoBookers = totalCoBookersNeeded - coBookers.length;
  const assignedUsers = new Set(Object.values(seatAssignments));

  // Define isAllSeatsAssigned to check if every selected seat has an assignee.
  const isAllSeatsAssigned = selectedSeats.every(
    (seatId) => seatAssignments[seatId]
  );

  const addCoBooker = async () => {
    setErrorMsg("");
    if (!email) {
      setErrorMsg("Please enter an email address.");
      return;
    }
    // Validate email format
    const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
    if (!emailRegex.test(email)) {
      setErrorMsg("Invalid email format.");
      return;
    }
    // Prevent adding your own email as a co-booker
    if (user && email === user.email) {
      setErrorMsg("You cannot add your own email as a co-booker.");
      return;
    }
    try {
      const searchResult = await searchUsers(email);
      if (Array.isArray(searchResult)) {
        if (searchResult.length === 0) {
          setErrorMsg("No user found with this email.");
          return;
        }
        // Use first found user
        const foundUser = searchResult[0];
        if (coBookers.find((co) => co.email === foundUser.email)) {
          setErrorMsg("Co-booker already added.");
          return;
        }
        if (coBookers.length < totalCoBookersNeeded) {
          setCoBookers([
            ...coBookers,
            { id: foundUser.id, email: foundUser.email },
          ]);
          setEmail("");
        }
      } else if (searchResult && searchResult.message) {
        setErrorMsg(searchResult.message);
        return;
      } else {
        setErrorMsg("Unexpected response from user search.");
        return;
      }
    } catch (error) {
      console.error("Error searching for user:", error);
      setErrorMsg("An error occurred while searching for the user.");
    }
  };

  const proceedToCheckout = async () => {
    if (!isAllSeatsAssigned) return;

    // Get seat numbers for all selected seats
    const seatNumbersMap = selectedSeats.reduce((map, seatId) => {
      const seat = availableSeats.find((s) => s.id === seatId);
      map[seatId] = seat?.seat_number || `A${seatId}`;
      return map;
    }, {} as { [key: number]: string });

    const bookingInfo = selectedSeats.map((seatId) => {
      const assignee = seatAssignments[seatId];
      let assignedUserEmail;
      let assignedUserId;
      if (assignee === "You") {
        assignedUserEmail = user ? user.email + ";" : null;
        assignedUserId = user ? user.id : null;
      } else {
        const found = coBookers.find((co) => co.email === assignee);
        assignedUserEmail = found ? found.email : null;
        assignedUserId = found ? found.id : null;
      }

      // Find the price of the selected seat
      const seat = availableSeats.find((s) => s.id === seatId);
      const price = seat?.price || 0;
      const seatNumber = seatNumbersMap[seatId];

      return {
        user_id: assignedUserId,
        user_email: assignedUserEmail,
        ticket_id: seatId,
        price: price,
        seat_number: seatNumber,
      };
    });

    // this sends list of {event_id, user_id, ticket_id, price, seat_number}
    try {
      const ticketBody = {
        eventId: eventId,
        userId: user.id,
        // seats: selectedSeats.join(","),
        seats: selectedSeats.map((id) => seatNumbersMap[id]),
      };
      // alert(JSON.stringify(ticketBody));
      const ticketReservation = await fetch(
        "http://localhost:8005/tickets/reserve",
        {
          method: "POST",
          headers: { "Content-Type": "application/json" },
          body: JSON.stringify(ticketBody),
        }
      );

      const reservationData = await ticketReservation.json();
      // alert(JSON.stringify(reservationData.data.reservation_id));
      const reservationId = reservationData.data.reservation_id;
      const send = {
        items: bookingInfo,
        event_id: eventId,
        reservation_id: reservationId,
      };
      alert(JSON.stringify(send));
      try {
        const response = await fetch("http://localhost:8010/party-booking", {
          method: "POST",
          headers: { "Content-Type": "application/json" },
          body: JSON.stringify(send),
        });

        const paymentResponse = await response.json();
        const redirect = paymentResponse.data.redirect_url;

        if (response.ok) {
          window.location.href =
            redirect +
            `?/confirmation?eventId=${eventId}&seats=${selectedSeats
              .map((seatId) => {
                const seat = availableSeats.find((s) => s.id === seatId);
                return seat?.seat_number || "";
              })
              .join(",")}&total=${totalPrice}`;
        }
      } catch (e) {
        console.error(e);
      }
    } catch (e) {
      console.error(e);
    }
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

        {/* Display total price */}
        <div className="mt-4 p-3 bg-blue-50 border border-blue-200 rounded-md text-center">
          <p className="text-black font-medium">
            Total Price: ${totalPrice.toFixed(2)}
          </p>
          <p className="text-sm text-gray-600">
            Price per person: ${(totalPrice / selectedSeats.length).toFixed(2)}
          </p>
        </div>

        {/* Input for Co-Bookers */}
        <div className="mt-6">
          <label className="text-black font-semibold">
            Enter co-booker email:
          </label>
          <div className="flex mt-2 flex-col">
            <div className="flex">
              <input
                type="email"
                placeholder="Enter email..."
                value={email}
                onChange={(e) => {
                  setEmail(e.target.value);
                  setErrorMsg("");
                }}
                className={`w-full p-2 border rounded text-black ${
                  errorMsg ? "border-red-500" : "border-gray-300"
                }`}
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
            {errorMsg && (
              <p className="mt-1 text-red-500 text-sm">{errorMsg}</p>
            )}
          </div>
        </div>

        {/* List of Co-Bookers */}
        <div className="mt-4">
          <h3 className="text-black font-semibold mb-2">Co-Bookers List:</h3>
          <ul>
            {coBookers.map((coBooker) => (
              <li
                key={coBooker.id}
                className="flex justify-between items-center bg-gray-100 p-3 rounded-lg mb-2"
              >
                <span className="text-black">{coBooker.email}</span>
                <button
                  onClick={() => {
                    setCoBookers(
                      coBookers.filter((co) => co.email !== coBooker.email)
                    );
                    setSeatAssignments((prev) => {
                      const updated = { ...prev };
                      Object.keys(updated).forEach((seatId) => {
                        if (updated[Number(seatId)] === coBooker.email) {
                          updated[Number(seatId)] = "";
                        }
                      });
                      return updated;
                    });
                  }}
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
            const seat = availableSeats.find((s) => s.id === seatId);
            const seatLabel = seat?.seat_number || `A${seatId}`;
            return (
              <div
                key={seatId}
                className="flex justify-between items-center bg-gray-100 p-3 rounded-lg mb-2"
              >
                <span className="text-black font-medium">Seat {seatLabel}</span>
                <select
                  className="p-2 border rounded text-black"
                  value={seatAssignments[seatId] || ""}
                  onChange={(e) => {
                    setSeatAssignments((prev) => ({
                      ...prev,
                      [seatId]: e.target.value,
                    }));
                  }}
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
                      key={coBooker.id}
                      value={coBooker.email}
                      disabled={
                        assignedUsers.has(coBooker.email) &&
                        seatAssignments[seatId] !== coBooker.email
                      }
                    >
                      {coBooker.email}
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
