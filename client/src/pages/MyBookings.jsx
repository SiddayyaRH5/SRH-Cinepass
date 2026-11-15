import React from "react";
import { useNavigate } from "react-router-dom";
import {
  ArrowLeft,
  CalendarDays,
  CheckCircle2,
  Clock3,
  QrCode,
  Users,
  IndianRupee,
  Ticket,
  Loader2,
} from "lucide-react";
import toast from "react-hot-toast";
import { getMyBookings } from "../lib/api";
import { getMyTickets } from "../lib/tickets";
import { seatLabels } from "../lib/seatLabels";
import {
  formatDate,
  formatDateTime,
  formatTime,
} from "../lib/formatters";
import { useAuth } from "../context/AuthContext";

/* ============================================================
   TICKET STATUS LABELS
============================================================ */

const TICKET_STATUS = {
  BOOKED: {
    label: "Valid for entry",
    className: "bg-green-400/10 text-green-300",
  },
  USED: {
    label: "Admitted",
    className: "bg-white/10 text-white/55",
  },
  CANCELLED: {
    label: "Cancelled",
    className: "bg-red-400/10 text-red-300",
  },
  EXPIRED: {
    label: "Expired",
    className: "bg-amber-400/10 text-amber-200",
  },
};

export default function MyBookings() {
  const navigate = useNavigate();
  const { isAuthenticated } = useAuth();

  const [bookings, setBookings] = React.useState([]);
  const [ticketsByBooking, setTicketsByBooking] = React.useState({});
  const [loading, setLoading] = React.useState(true);

  /* ==========================================================
     LOAD USER BOOKINGS + TICKETS
  ========================================================== */

  React.useEffect(() => {
    if (!isAuthenticated) {
      setLoading(false);
      return;
    }

    let active = true;

    Promise.allSettled([getMyBookings(), getMyTickets()])
      .then(([bookingsResult, ticketsResult]) => {
        if (!active) return;

        if (bookingsResult.status === "fulfilled") {
          setBookings(
            Array.isArray(bookingsResult.value)
              ? bookingsResult.value
              : []
          );
        } else {
          toast.error(
            bookingsResult.reason?.message ||
              "Unable to load bookings"
          );
        }

        if (ticketsResult.status === "fulfilled") {
          const tickets = Array.isArray(ticketsResult.value)
            ? ticketsResult.value
            : [];

          setTicketsByBooking(
            tickets.reduce((map, ticket) => {
              if (ticket?.bookingId != null) {
                map[ticket.bookingId] = ticket;
              }

              return map;
            }, {})
          );
        }
      })
      .finally(() => {
        if (active) setLoading(false);
      });

    return () => {
      active = false;
    };
  }, [isAuthenticated]);

  /* ==========================================================
     NOT LOGGED IN
  ========================================================== */

  if (!isAuthenticated) {
    return (
      <main
        className="
          min-h-screen
          px-5
          pt-36
          text-center
        "
      >
        <Ticket
          size={42}
          className="mx-auto text-cyan-300"
        />

        <h1 className="mt-5 text-3xl font-semibold">
          Your tickets are waiting
        </h1>

        <p className="mt-2 text-white/40">
          Sign in to view your booking history.
        </p>

        <button
          onClick={() => navigate("/login")}
          className="
            mt-7
            rounded-full
            bg-cyan-300
            px-6
            py-3
            text-sm
            font-semibold
            text-black
            transition
            hover:bg-cyan-200
          "
        >
          Sign in
        </button>
      </main>
    );
  }

  /* ==========================================================
     LOADING
  ========================================================== */

  if (loading) {
    return (
      <main className="pt-28">
        <div
          className="
            flex
            min-h-[60vh]
            items-center
            justify-center
            gap-2
            text-white/45
          "
        >
          <Loader2
            className="animate-spin"
            size={18}
          />

          Loading bookings...
        </div>
      </main>
    );
  }

  /* ==========================================================
     MAIN PAGE
  ========================================================== */

  return (
    <main
      className="
        min-h-screen
        px-5
        pb-20
        pt-32
        sm:px-8
        lg:px-12
      "
    >
      <div className="mx-auto max-w-[1100px]">

        {/* ====================================================
            BACK BUTTON
        ==================================================== */}

        <button
          onClick={() => navigate(-1)}
          className="
            mb-7
            flex
            items-center
            gap-2
            text-sm
            text-white/45
            transition
            hover:text-white
          "
        >
          <ArrowLeft size={17} />
          Back
        </button>

        {/* ====================================================
            PAGE HEADER
        ==================================================== */}

        <div className="mb-10">
          <p
            className="
              text-xs
              font-semibold
              uppercase
              tracking-[.25em]
              text-cyan-300
            "
          >
            Your cinema history
          </p>

          <h1
            className="
              mt-2
              text-4xl
              font-semibold
            "
          >
            My Bookings
          </h1>

          <p className="mt-2 text-sm text-white/40">
            {bookings.length} confirmed booking
            {bookings.length !== 1 ? "s" : ""}
          </p>
        </div>

        {/* ====================================================
            BOOKINGS
        ==================================================== */}

        {bookings.length ? (
          <div className="space-y-5">
            {bookings.map((booking) => (
              <Booking
                key={booking.id}
                booking={booking}
                ticket={ticketsByBooking[booking.id]}
              />
            ))}
          </div>
        ) : (
          /* ==================================================
             NO BOOKINGS
          ================================================== */

          <div
            className="
              rounded-3xl
              border
              border-white/10
              bg-white/[.03]
              p-14
              text-center
            "
          >
            <Ticket
              size={38}
              className="mx-auto text-white/20"
            />

            <h2 className="mt-5 text-xl font-semibold">
              No bookings yet
            </h2>

            <p className="mt-2 text-sm text-white/40">
              Your next great movie night is one click away.
            </p>

            <button
              onClick={() => navigate("/movies")}
              className="
                mt-6
                rounded-full
                bg-cyan-300
                px-6
                py-3
                text-sm
                font-semibold
                text-black
                transition
                hover:bg-cyan-200
              "
            >
              Browse movies
            </button>
          </div>
        )}
      </div>
    </main>
  );
}

/* ============================================================
   BOOKING CARD
============================================================ */

function Booking({ booking: b, ticket }) {
  const navigate = useNavigate();

  const s = b.show || {};
  const m = s.movie || {};
  const t = s.theatre || {};
  const loc = t.location || {};

  const seats = Array.isArray(b.seatNumbers)
    ? b.seatNumbers
    : [];

  const cancelled =
    String(b.status || "").toUpperCase() === "CANCELLED";

  const ticketStatus = cancelled
    ? TICKET_STATUS.CANCELLED
    : TICKET_STATUS[ticket?.status] || null;

  const canViewTicket = Boolean(ticket?.ticketId);

  return (
    <article
      className="
        overflow-hidden
        rounded-3xl
        border
        border-white/10
        bg-[#0d1118]
        transition
        hover:border-cyan-300/25
      "
    >
      <div
        className="
          grid
          md:grid-cols-[160px_1fr]
        "
      >

        {/* ==================================================
            POSTER
        ================================================== */}

        <div
          className="
            relative
            hidden
            min-h-[230px]
            md:block
          "
        >
          {m.posterUrl && (
            <img
              src={m.posterUrl}
              alt={m.title}
              className="
                absolute
                inset-0
                h-full
                w-full
                object-cover
                opacity-70
              "
            />
          )}

          <div
            className="
              absolute
              inset-0
              bg-gradient-to-r
              from-transparent
              to-[#0d1118]
            "
          />
        </div>

        {/* ==================================================
            BOOKING CONTENT
        ================================================== */}

        <div className="p-6 sm:p-7">

          {/* =================================================
              TOP SECTION
          ================================================= */}

          <div
            className="
              flex
              items-start
              justify-between
              gap-4
            "
          >
            <div>

              {/* STATUS */}

              <div className="flex flex-wrap items-center gap-2">
                <span
                  className={`
                    inline-flex
                    items-center
                    gap-1.5
                    rounded-full
                    px-2.5
                    py-1
                    text-[10px]
                    font-semibold
                    uppercase
                    tracking-wider
                    ${ticketStatus?.className || "bg-green-400/10 text-green-300"}
                  `}
                >
                  <CheckCircle2 size={12} />

                  {ticketStatus?.label || b.status || "CONFIRMED"}
                </span>
              </div>

              {/* MOVIE TITLE */}

              <h2
                className="
                  mt-3
                  text-2xl
                  font-semibold
                "
              >
                {m.title || "Movie"}
              </h2>

              {/* THEATRE */}

              <p className="mt-1 text-sm text-white/40">
                {t.name || "Theatre"}
                {" · "}
                {loc.city || ""}
              </p>
            </div>

            {/* BOOKING NUMBER */}

            <div className="text-right">
              <p className="text-xs text-white/30">
                Booking
              </p>

              <p
                className="
                  font-mono
                  text-sm
                  text-white/60
                "
              >
                #{b.id}
              </p>
            </div>
          </div>

          {/* =================================================
              BOOKING INFORMATION
          ================================================= */}

          <div
            className="
              mt-6
              grid
              gap-4
              border-t
              border-white/10
              pt-5
              sm:grid-cols-2
              lg:grid-cols-3
            "
          >

            {/* DATE */}

            <Info
              icon={CalendarDays}
              label="Date"
              value={formatDate(s.showDate)}
            />

            {/* SHOW TIME */}

            <Info
              icon={Clock3}
              label="Show time"
              value={formatTime(s.showTime)}
            />

            {/* =================================================
                SEATS
               
                IMPORTANT:
                Backend:
                S77

                Display:
                H7
            ================================================= */}

            <Info
              icon={Users}
              label="Seats"
              value={
                seats.length
                  ? seatLabels(seats).join(", ")
                  : "—"
              }
            />
          </div>

          {/* =================================================
              BOTTOM SECTION
          ================================================= */}

          <div
            className="
              mt-6
              flex
              flex-col
              justify-between
              gap-3
              border-t
              border-white/10
              pt-5
              sm:flex-row
              sm:items-center
            "
          >

            {/* BOOKING DATE */}

            <p className="text-xs text-white/30">
              Booked{" "}
              {formatDateTime(b.bookingDate)}
            </p>

            {/* ACTIONS + TOTAL */}

            <div className="flex flex-wrap items-center gap-4">
              {canViewTicket && (
                <button
                  type="button"
                  onClick={() =>
                    navigate(`/tickets/${ticket.ticketId}`)
                  }
                  className="
                    flex
                    items-center
                    gap-2
                    rounded-full
                    bg-cyan-300
                    px-5
                    py-2.5
                    text-xs
                    font-semibold
                    text-slate-950
                    transition
                    hover:bg-cyan-200
                  "
                >
                  <QrCode size={15} />
                  View Ticket
                </button>
              )}

              <p
                className="
                  flex
                  items-center
                  gap-1
                  text-xl
                  font-semibold
                  text-cyan-200
                "
              >
                <IndianRupee size={18} />

                {Number(
                  b.totalAmount || 0
                ).toLocaleString("en-IN")}
              </p>
            </div>
          </div>
        </div>
      </div>
    </article>
  );
}

/* ============================================================
   INFORMATION ITEM
============================================================ */

function Info({
  icon: Icon,
  label,
  value,
}) {
  return (
    <div className="flex gap-3">

      {Icon ? (
        <Icon
          size={17}
          className="
            mt-0.5
            shrink-0
            text-cyan-300
          "
        />
      ) : null}

      <div className="min-w-0">

        <p
          className="
            text-[10px]
            uppercase
            tracking-wider
            text-white/25
          "
        >
          {label}
        </p>

        <p
          className="
            mt-1
            break-words
            text-sm
            text-white/75
          "
        >
          {value}
        </p>

      </div>
    </div>
  );
}