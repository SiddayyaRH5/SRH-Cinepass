
import React from "react";
import { useNavigate, useParams } from "react-router-dom";
import {
  ArrowLeft,
  Check,
  Clock3,
  Info,
  LockKeyhole,
  MapPin,
  Ticket,
} from "lucide-react";
import toast from "react-hot-toast";

import { apiFetch, getSeats, getShow } from "../lib/api";
import { formatDate, formatTime, money } from "../lib/formatters";
import Loading from "../components/Loading";
import { useAuth } from "../context/AuthContext";

// Load Razorpay Checkout script only once.
const loadRazorpayScript = () => {
  return new Promise((resolve) => {
    if (window.Razorpay) {
      resolve(true);
      return;
    }

    const existingScript = document.querySelector(
      'script[src="https://checkout.razorpay.com/v1/checkout.js"]'
    );

    if (existingScript) {
      existingScript.addEventListener("load", () => resolve(true), {
        once: true,
      });

      existingScript.addEventListener("error", () => resolve(false), {
        once: true,
      });

      return;
    }

    const script = document.createElement("script");

    script.src = "https://checkout.razorpay.com/v1/checkout.js";
    script.async = true;

    script.onload = () => resolve(true);
    script.onerror = () => resolve(false);

    document.body.appendChild(script);
  });
};

export default function SeatLayout() {
  const { id } = useParams();
  const navigate = useNavigate();
  const { isAuthenticated } = useAuth();

  const [show, setShow] = React.useState(null);
  const [seats, setSeats] = React.useState([]);
  const [selected, setSelected] = React.useState([]);
  const [loading, setLoading] = React.useState(true);
  const [booking, setBooking] = React.useState(false);

  const checkoutOpened = React.useRef(false);
  const activeBookingId = React.useRef(null);

  // Load the actual show and seats from the backend.
  const loadShowAndSeats = React.useCallback(async () => {
    const [showData, seatData] = await Promise.all([
      getShow(id),
      getSeats(id),
    ]);

    if (!Array.isArray(seatData)) {
      throw new Error("Invalid seat data received from server");
    }

    setShow(showData);
    setSeats(seatData);
    setSelected([]);
  }, [id]);

  React.useEffect(() => {
    let active = true;

    async function load() {
      setLoading(true);

      try {
        const [showData, seatData] = await Promise.all([
          getShow(id),
          getSeats(id),
        ]);

        if (!active) return;

        if (!Array.isArray(seatData)) {
          throw new Error("Invalid seat data received from server");
        }

        setShow(showData);
        setSeats(seatData);
      } catch (error) {
        if (active) {
          toast.error(error.message || "Unable to load show or seats");
        }
      } finally {
        if (active) setLoading(false);
      }
    }

    load();

    return () => {
      active = false;
    };
  }, [id]);

  // Seat selection.
  const choose = (seat) => {
    if (seat.booked || booking) return;

    setSelected((previous) => {
      const alreadySelected = previous.some(
        (item) => item.id === seat.id
      );

      if (alreadySelected) {
        return previous.filter((item) => item.id !== seat.id);
      }

      if (previous.length >= 5) {
        toast.error("Maximum 5 seats per booking");
        return previous;
      }

      return [...previous, seat];
    });
  };

  const price = Number(show?.ticketPrice || 0);
  const total = price * selected.length;

  // Display labels such as A1, A2, B1, B2.
  const label = (index) =>
    `${String.fromCharCode(65 + Math.floor(index / 10))}${(index % 10) + 1}`;

  const getSeatLabel = (seat) => {
    const index = seats.findIndex((item) => item.id === seat.id);
    return index >= 0 ? label(index) : "";
  };

  // Cancel a pending reservation through the backend.
  const cancelPendingBooking = async (bookingId) => {
    if (!bookingId) return;

    try {
      await apiFetch(`/api/bookings/cancel/${bookingId}`, {
        method: "POST",
      });
    } catch (error) {
      console.error("Unable to cancel pending booking:", error);
      toast.error(
        "Reservation cancellation could not be confirmed. It will expire automatically."
      );
    }

    try {
      await loadShowAndSeats();
    } catch {
      toast.error("Unable to refresh seat availability");
    }
  };

  // Open Razorpay Checkout.
  const openCheckout = (orderData) => {
    return new Promise((resolve, reject) => {
      if (!window.Razorpay) {
        reject(new Error("Razorpay Checkout is not available"));
        return;
      }

      checkoutOpened.current = true;

      const options = {
        key: orderData.keyId,
        amount: orderData.amount,
        currency: orderData.currency || "INR",

        name: orderData.name || "SRH CinePass",
        description: orderData.description || "Movie Ticket Booking",

        order_id: orderData.orderId,

        handler: async function (paymentResponse) {
          try {
            toast.loading("Verifying your payment...", {
              id: "payment-verification",
            });

            const verification = await apiFetch(
              "/api/bookings/verify-payment",
              {
                method: "POST",
                body: JSON.stringify({
                  razorpay_order_id:
                    paymentResponse.razorpay_order_id,

                  razorpay_payment_id:
                    paymentResponse.razorpay_payment_id,

                  razorpay_signature:
                    paymentResponse.razorpay_signature,
                }),
              }
            );

            toast.dismiss("payment-verification");

            if (
              !verification ||
              verification.status !== "CONFIRMED"
            ) {
              throw new Error(
                verification?.message ||
                  "Payment could not be confirmed by the server"
              );
            }

            activeBookingId.current = null;

            toast.success("Payment successful! Booking confirmed.");

            resolve({
              success: true,
              bookingId: verification.bookingId,
            });
          } catch (error) {
            toast.dismiss("payment-verification");

            console.error("Payment verification failed:", error);

            // Do not release seats automatically here:
            // Razorpay may have captured payment even if verification
            // temporarily failed. The reservation expires after 10 minutes.
            toast.error(
              error.message ||
                "Payment verification failed. Please contact support."
            );

            resolve({
              success: false,
              verificationFailed: true,
            });
          }
        },

        prefill: {},

        notes: {
          project: "SRH CinePass",
        },

        theme: {
          color: "#67e8f9",
        },

        modal: {
          ondismiss: async function () {
            if (!checkoutOpened.current) return;

            checkoutOpened.current = false;

            const bookingId = activeBookingId.current;
            activeBookingId.current = null;

            if (bookingId) {
              await cancelPendingBooking(bookingId);
            }

            setBooking(false);
            toast("Payment window closed. No booking was confirmed.");
            resolve({
              success: false,
              dismissed: true,
            });
          },
        },
      };

      try {
        const razorpay = new window.Razorpay(options);

        razorpay.on("payment.failed", function (response) {
          console.error("Razorpay payment failed:", response.error);

          toast.error(
            response.error?.description ||
              "Payment failed. You can retry or close checkout."
          );
        });

        razorpay.open();
      } catch (error) {
        checkoutOpened.current = false;
        reject(error);
      }
    });
  };

  // Complete payment-first booking flow.
  const book = async () => {
    if (booking) return;

    if (!isAuthenticated) {
      toast.error("Please login to book tickets");
      navigate("/login");
      return;
    }

    if (selected.length === 0) {
      toast.error("Select at least one seat");
      return;
    }

    if (selected.length > 5) {
      toast.error("Maximum 5 seats per booking");
      return;
    }

    let pendingBookingId = null;

    try {
      setBooking(true);

      // Load Razorpay Checkout.
      const scriptLoaded = await loadRazorpayScript();

      if (!scriptLoaded) {
        throw new Error(
          "Unable to load Razorpay Checkout. Check your internet connection."
        );
      }

      // Ask the backend to reserve seats and create a Razorpay order.
      const orderData = await apiFetch(
        "/api/bookings/create-order",
        {
          method: "POST",
          body: JSON.stringify({
            showId: Number(id),
            seatIds: selected.map((seat) => seat.id),
          }),
        }
      );

      if (
        !orderData ||
        !orderData.bookingId ||
        !orderData.orderId ||
        !orderData.keyId ||
        !orderData.amount
      ) {
        throw new Error(
          orderData?.message ||
            "Invalid payment order received from server"
        );
      }

      pendingBookingId = orderData.bookingId;
      activeBookingId.current = pendingBookingId;

      // Open Razorpay payment window.
      const result = await openCheckout(orderData);

      if (result.success) {
        navigate("/My-Bookings", {
          state: { refresh: true },
        });
      } else if (result.verificationFailed) {
        // Payment may have succeeded at Razorpay.
        // Keep the reservation until expiry; do not pretend it failed.
        await loadShowAndSeats();
      } else if (result.dismissed) {
        // Cancellation and seat refresh are handled in ondismiss.
      }
    } catch (error) {
      console.error("Booking error:", error);

      toast.error(
        error.message || "Unable to start payment. Please try again."
      );

      // If the order was created but checkout failed to open,
      // release the pending reservation.
      if (pendingBookingId && activeBookingId.current) {
        activeBookingId.current = null;
        await cancelPendingBooking(pendingBookingId);
      } else {
        try {
          await loadShowAndSeats();
        } catch {
          toast.error("Unable to refresh seat availability");
        }
      }
    } finally {
      setBooking(false);
    }
  };

  if (loading) {
    return (
      <main className="pt-28">
        <Loading label="Preparing your seat map..." />
      </main>
    );
  }

  if (!show) {
    return (
      <main className="px-5 pt-40 text-center">
        Show not found.
      </main>
    );
  }

  return (
    <main className="min-h-screen px-4 pb-20 pt-28 sm:px-6 lg:px-10">
      <div className="mx-auto max-w-[1280px]">

        <button
          onClick={() => navigate(-1)}
          className="mb-6 flex items-center gap-2 text-sm text-white/45 transition hover:text-white"
        >
          <ArrowLeft size={17} />
          Back
        </button>

        <div className="mb-7 rounded-2xl border border-white/10 bg-white/[.03] p-5 sm:p-6">
          <div className="flex flex-col justify-between gap-4 md:flex-row md:items-center">

            <div>
              <p className="text-xs font-semibold uppercase tracking-[.22em] text-cyan-300">
                Select your seats
              </p>

              <h1 className="mt-2 text-2xl font-semibold sm:text-3xl">
                {show.movie?.title || "Movie"}
              </h1>

              <p className="mt-2 flex flex-wrap gap-3 text-sm text-white/40">
                <span className="flex items-center gap-1">
                  <MapPin size={14} />
                  {show.theatre?.name || "Theatre"}
                </span>

                <span>{formatDate(show.showDate)}</span>

                <span className="flex items-center gap-1">
                  <Clock3 size={14} />
                  {formatTime(show.showTime)}
                </span>
              </p>
            </div>

            <div className="rounded-xl bg-cyan-300/10 px-4 py-3 text-right">
              <p className="text-xs text-white/40">Ticket price</p>

              <p className="text-xl font-semibold text-cyan-200">
                {money(price)}
              </p>
            </div>

          </div>
        </div>

        <div className="grid gap-8 xl:grid-cols-[1fr_330px]">

          <div className="rounded-3xl border border-white/10 bg-white/[.02] p-5 sm:p-8">

            <div className="mx-auto max-w-2xl">

              <div className="mb-2 h-2 rounded-[50%] bg-gradient-to-r from-transparent via-cyan-200/80 to-transparent blur-[1px]" />

              <p className="mb-10 text-center text-[10px] font-semibold tracking-[.5em] text-white/35">
                SCREEN
              </p>

              {seats.length === 0 ? (
                <div className="rounded-xl border border-white/10 p-6 text-center text-sm text-white/50">
                  No seats are configured for this show yet.
                  Please contact the theatre administrator.
                </div>
              ) : (
                <div className="grid grid-cols-10 gap-1.5 sm:gap-2">

                  {seats.map((seat, index) => {
                    const active = selected.some(
                      (item) => item.id === seat.id
                    );

                    return (
                      <button
                        key={seat.id}
                        disabled={Boolean(seat.booked) || booking}
                        onClick={() => choose(seat)}
                        title={
                          seat.booked
                            ? "Booked or temporarily reserved"
                            : `Seat ${label(index)}`
                        }
                        className={`
                          relative aspect-square rounded-md text-[9px]
                          font-semibold transition duration-200
                          sm:rounded-lg sm:text-[10px]
                          ${
                            seat.booked
                              ? "cursor-not-allowed bg-red-500/75 text-white/70"
                              : active
                              ? "scale-105 bg-cyan-300 text-slate-950 shadow-[0_0_18px_rgba(18,207,232,.35)]"
                              : "bg-white/[.08] text-white/45 hover:-translate-y-0.5 hover:bg-cyan-300/30 hover:text-cyan-100"
                          }
                        `}
                      >
                        {label(index)}

                        {active && (
                          <Check
                            size={9}
                            className="absolute right-1 top-1 sm:right-1.5 sm:top-1.5"
                          />
                        )}
                      </button>
                    );
                  })}

                </div>
              )}

              <div className="mt-9 flex flex-wrap justify-center gap-5 text-xs text-white/40">

                <span className="flex items-center gap-2">
                  <i className="h-3 w-3 rounded bg-white/[.1]" />
                  Available
                </span>

                <span className="flex items-center gap-2">
                  <i className="h-3 w-3 rounded bg-cyan-300" />
                  Selected
                </span>

                <span className="flex items-center gap-2">
                  <i className="h-3 w-3 rounded bg-red-500/75" />
                  Booked / Reserved
                </span>

              </div>
            </div>
          </div>

          <aside className="h-fit rounded-3xl border border-white/10 bg-[#0c1017] p-6 xl:sticky xl:top-24">

            <div className="mb-6 flex items-center gap-3">

              <span className="grid h-10 w-10 place-items-center rounded-xl bg-cyan-300/10 text-cyan-300">
                <Ticket size={19} />
              </span>

              <div>
                <p className="text-sm font-semibold">Booking summary</p>
                <p className="text-xs text-white/35">Up to 5 seats</p>
              </div>

            </div>

            <div className="space-y-4 text-sm">

              <div className="flex justify-between text-white/45">
                <span>Selected</span>

                <span className="text-white">
                  {selected.length} seat{selected.length !== 1 ? "s" : ""}
                </span>
              </div>

              <div className="rounded-xl border border-white/10 bg-white/[.03] p-3">

                <p className="mb-2 text-[11px] uppercase tracking-wider text-white/30">
                  Seats
                </p>

                <p className="text-cyan-200">
                  {selected.length
                    ? selected.map(getSeatLabel).filter(Boolean).join(", ")
                    : "No seats selected"}
                </p>

              </div>

              <div className="flex justify-between border-t border-white/10 pt-4 text-white/45">
                <span>Price / seat</span>
                <span>{money(price)}</span>
              </div>

              <div className="flex items-end justify-between">
                <span className="text-white/55">Total</span>

                <span className="text-2xl font-semibold text-white">
                  {money(total)}
                </span>
              </div>

            </div>

            <button
              disabled={
                booking ||
                selected.length === 0 ||
                seats.length === 0
              }
              onClick={book}
              className="mt-6 flex w-full items-center justify-center gap-2 rounded-xl bg-cyan-300 py-3.5 text-sm font-semibold text-slate-950 transition hover:bg-cyan-200 disabled:cursor-not-allowed disabled:opacity-35"
            >
              {booking ? (
                <>
                  <span className="h-4 w-4 animate-spin rounded-full border-2 border-slate-950/30 border-t-slate-950" />
                  Processing...
                </>
              ) : (
                <>
                  <LockKeyhole size={16} />
                  Proceed to Payment
                </>
              )}
            </button>

            <p className="mt-4 flex gap-2 text-[11px] leading-5 text-white/30">
              <Info size={14} className="mt-0.5 shrink-0" />
              Seats are temporarily reserved for 10 minutes while you complete payment. Booking confirmation happens only after server-side payment verification.
            </p>

          </aside>
        </div>
      </div>
    </main>
  );
}
