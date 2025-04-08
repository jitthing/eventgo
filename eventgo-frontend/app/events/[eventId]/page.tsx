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
import EventSkeleton from "@/components/EventSkeleton";

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
  const [hasPendingReservation, setHasPendingReservation] = useState(false);

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

  useEffect(() => {
    // Check for pending reservation on component mount
    const storedReservationId = localStorage.getItem("reservationId");
    const storedUserId = localStorage.getItem("userId");
    if (storedReservationId && storedUserId) {
      setReservationId(storedReservationId);
      setHasPendingReservation(true);
    }
  }, []);

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
  if (!event) return <EventSkeleton />;

  return (
    <div className="relative min-h-screen bg-white py-12 px-4 sm:px-6 lg:px-8">
      <BackButton />

      <div className="max-w-6xl mx-auto grid grid-cols-1 md:grid-cols-2 gap-10">
        {hasPendingReservation && (
          <div className="col-span-2 mb-4">
            <div className="bg-yellow-50 border-l-4 border-yellow-400 p-4">
              <div className="flex">
                <div className="flex-shrink-0">
                  <svg
                    className="h-5 w-5 text-yellow-400"
                    viewBox="0 0 20 20"
                    fill="currentColor"
                  >
                    <path
                      fillRule="evenodd"
                      d="M8.257 3.099c.765-1.36 2.722-1.36 3.486 0l5.58 9.92c.75 1.334-.213 2.98-1.742 2.98H4.42c-1.53 0-2.493-1.646-1.743-2.98l5.58-9.92zM11 13a1 1 0 11-2 0 1 1 0 012 0zm-1-8a1 1 0 00-1 1v3a1 1 0 002 0V6a1 1 0 00-1-1z"
                      clipRule="evenodd"
                    />
                  </svg>
                </div>
                <div className="ml-3">
                  <p className="text-sm text-yellow-700">
                    You have a pending payment.
                    <button
                      onClick={() => {
                        const storedReservationId =
                          localStorage.getItem("reservationId");
                        const storedUserId = localStorage.getItem("userId");
                        
                        // Check if we have a stored stripe redirect URL (for group bookings)
                        const storedStripeUrl = localStorage.getItem("stripeRedirectUrl");
                        
                        if (storedStripeUrl) {
                          // If we have a stored Stripe URL, redirect directly to Stripe
                          window.location.href = storedStripeUrl;
                          return;
                        }
                        
                        // Otherwise proceed with normal individual booking payment flow
                        if (storedReservationId && storedUserId) {
                          // Get the stored seat and price information
                          const storedSeats =
                            localStorage.getItem("selectedSeats");
                          const storedTotalPrice =
                            localStorage.getItem("totalPrice");

                          console.log("Stored Data:", {
                            seats: storedSeats,
                            totalPrice: storedTotalPrice,
                            availableSeats,
                          });

                          if (!storedSeats || !storedTotalPrice) {
                            console.error("Missing stored data");
                            return;
                          }

                          const selectedSeatsArray = JSON.parse(storedSeats);

                          // Get the seat numbers from the stored seats
                          const seatNumbers = selectedSeatsArray
                            .map((seatId: number) => {
                              const seat = availableSeats.find(
                                (s) => s.id === seatId
                              );
                              return seat?.seat_number || "";
                            })
                            .filter(Boolean) // Remove any empty strings
                            .join(",");

                          console.log("Generated URL params:", {
                            eventId,
                            seatNumbers,
                            totalPrice: storedTotalPrice,
                            reservationId: storedReservationId,
                          });

                          router.push(
                            `/payment?eventId=${eventId}&seats=${seatNumbers}&total=${storedTotalPrice}&reservationId=${storedReservationId}`
                          );
                        }
                      }}
                      className="ml-2 font-medium underline hover:text-yellow-600"
                    >
                      Click here to complete your payment
                    </button>
                  </p>
                </div>
              </div>
            </div>
          </div>
        )}
        <div className="relative w-full h-96 md:h-[500px] rounded-lg overflow-hidden bg-gray-200">
          <Image
            src={event.image_url}
            alt={event.title}
            fill
            className="object-cover"
            onError={(e) => {
              // Fallback to a placeholder div with event title
              const target = e.target as HTMLImageElement;
              target.style.display = "none";
              const container = target.parentElement;
              if (container) {
                container.innerHTML = `
                  <div class="flex items-center justify-center h-full bg-gray-100">
                    <div class="text-center p-4">
                      <h3 class="text-xl font-semibold text-gray-700">${event.title}</h3>
                      <p class="text-gray-500 mt-2">Image not available</p>
                    </div>
                  </div>
                `;
              }
            }}
            priority
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
                    // Clear any old reservations first
                    localStorage.removeItem("reservationId");
                    localStorage.removeItem("userId");
                    localStorage.removeItem("selectedSeats");
                    localStorage.removeItem("totalPrice");
                    localStorage.removeItem("stripeRedirectUrl");
                    localStorage.removeItem("isGroupBooking");
                    setHasPendingReservation(false);

                    // First, make the reservation
                    const ticketBody = {
                      eventId: eventId,
                      userId: user.id,
                      seats: selectedSeats.map((id) => {
                        const seat = availableSeats.find((s) => s.id === id);
                        return seat?.seat_number || "";
                      }),
                    };

                    const ticketReservation = await fetch(`${process.env.NEXT_PUBLIC_TICKETS_API_URL}/tickets/reserve`,
                      {
                        method: "POST",
                        headers: { "Content-Type": "application/json" },
                        body: JSON.stringify(ticketBody),
                      }
                    );

                    const reservationData = await ticketReservation.json();
                    
                    // Safely access the reservation_id
                    if (!reservationData || !reservationData.data || !reservationData.data.reservation_id) {
                      throw new Error("Invalid reservation response");
                    }
                    
                    const reservationId = reservationData.data.reservation_id;

                    // Save all necessary information to localStorage
                    localStorage.setItem("reservationId", reservationId);
                    localStorage.setItem("userId", user.id.toString());
                    localStorage.setItem(
                      "selectedSeats",
                      JSON.stringify(selectedSeats)
                    );
                    localStorage.setItem("totalPrice", totalPrice.toString());

                    console.log("Saving to localStorage:", {
                      reservationId,
                      userId: user.id,
                      selectedSeats,
                      totalPrice,
                    });

                    // Navigate to payment page with all necessary params
                    router.push(
                      `/payment?eventId=${eventId}&seats=${selectedSeats
                        .map((seatId) => {
                          const seat = availableSeats.find(
                            (s) => s.id === seatId
                          );
                          return seat?.seat_number || "";
                        })
                        .join(
                          ","
                        )}&total=${totalPrice}&reservationId=${reservationId}`
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
          eventTitle={event.title}
          eventDescription={event.description}
          setHasPendingReservation={setHasPendingReservation}
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
  eventTitle,
  eventDescription,
  setHasPendingReservation,
}: {
  selectedSeats: number[];
  eventId: number;
  onClose: () => void;
  totalPrice: number;
  availableSeats: any[];
  eventTitle: string;
  eventDescription: string;
  setHasPendingReservation: (value: boolean) => void;
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
    if (!user) {
      console.error("User not authenticated");
      router.push("/login");
      return;
    }

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
        assignedUserEmail = user.email + ";";
        assignedUserId = user.id;
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
        price: price * 100,
        seat_number: seatNumber,
      };
    });

    // this sends list of {event_id, user_id, ticket_id, price, seat_number}
    try {
      const ticketBody = {
        eventId: eventId,
        userId: user.id,
        seats: selectedSeats.map((id) => seatNumbersMap[id]),
      };

      const ticketReservation =await fetch(`${process.env.NEXT_PUBLIC_TICKETS_API_URL}/tickets/reserve`,
        {
          method: "POST",
          headers: { "Content-Type": "application/json" },
          body: JSON.stringify(ticketBody),
        }
      );

      if (!ticketReservation.ok) {
        throw new Error("Failed to reserve tickets");
      }

      const reservationData = await ticketReservation.json();
      const reservationId = reservationData.data.reservation_id;

      // Clear any old reservations first
      localStorage.removeItem("reservationId");
      localStorage.removeItem("userId");
      localStorage.removeItem("selectedSeats");
      localStorage.removeItem("totalPrice");
      setHasPendingReservation(false);

      const send = {
        items: bookingInfo,
        event_id: eventId,
        event_title: eventTitle,
        event_description: eventDescription,
        reservation_id: reservationId,
      };

      try {
        const response = await fetch(`${process.env.NEXT_PUBLIC_TICKET_SPLIT_API_URL}/party-booking`, {
          method: "POST",
          headers: { "Content-Type": "application/json" },
          body: JSON.stringify(send),
        });

        if (!response.ok) {
          throw new Error("Failed to create party booking");
        }

        const paymentResponse = await response.json();
        const redirect = paymentResponse.data.redirect_url;

        if (response.ok) {
          const seatNumbers = selectedSeats
            .map((seatId) => {
              const seat = availableSeats.find((s) => s.id === seatId);
              return seat?.seat_number || "";
            })
            .join(",");

          // Store the Stripe redirect URL for future use
          localStorage.setItem("stripeRedirectUrl", redirect);
          
          // Store other necessary information in localStorage
          localStorage.setItem("reservationId", reservationId);
          localStorage.setItem("userId", user.id.toString());
          localStorage.setItem("selectedSeats", JSON.stringify(selectedSeats));
          localStorage.setItem("totalPrice", totalPrice.toString());
          
          // Flag this as a group booking
          localStorage.setItem("isGroupBooking", "true");
          
          // Redirect to Stripe
          window.location.href = redirect;
        }
      } catch (e) {
        console.error("Error in party booking:", e);
        alert("Failed to process group booking. Please try again.");
      }
    } catch (e) {
      console.error("Error in ticket reservation:", e);
      alert("Failed to reserve tickets. Please try again.");
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
