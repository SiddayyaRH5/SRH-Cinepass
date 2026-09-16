import React from "react";
import { useNavigate } from "react-router-dom";
import {
  ArrowLeft,
  CalendarDays,
  CheckCircle2,
  Clock3,
  Users,
  IndianRupee,
  Ticket,
  Loader2,
} from "lucide-react";
import toast from "react-hot-toast";
import { getMyBookings } from "../lib/api";
import {
  formatDate,
  formatDateTime,
  formatTime,
} from "../lib/formatters";
import { useAuth } from "../context/AuthContext";

/* ============================================================
   CONVERT BACKEND SEAT NUMBER TO VISUAL SEAT LABEL

   Backend:
   S1  S2  S3 ... S10
   S11 S12 ... S20
   S21 S22 ... S30

   UI:
   A1  A2  A3 ... A10
   B1  B2  B3 ... B10
   C1  C2  C3 ... C10

   Examples:
   S7  -> A7
   S28 -> C8
   S77 -> H7
============================================================ */

const seatLabel = (seatNumber) => {
  const number = Number(
    String(seatNumber).replace(/^S/i, "")
  );

  if (!Number.isInteger(number) || number < 1) {
    return seatNumber;
  }

  const row = Math.floor((number - 1) / 10);
  const seat = ((number - 1) % 10) + 1;

  return `${String.fromCharCode(65 + row)}${seat}`;
};

export default function MyBookings() {
  const navigate = useNavigate();
  const { isAuthenticated } = useAuth();

  const [bookings, setBookings] = React.useState([]);
  const [loading, setLoading] = React.useState(true);

  /* ==========================================================
     LOAD USER BOOKINGS
  ========================================================== */

  React.useEffect(() => {
    if (!isAuthenticated) {
      setLoading(false);
      return;
    }

    getMyBookings()
      .then((data) => {
        setBookings(
          Array.isArray(data) ? data : []
        );
      })
      .catch((e) => {
        toast.error(
          e.message || "Unable to load bookings"
        );
      })
      .finally(() => {
        setLoading(false);
      });
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

function Booking({ booking: b }) {
  const s = b.show || {};
  const m = s.movie || {};
  const t = s.theatre || {};
  const loc = t.location || {};

  const seats = Array.isArray(b.seatNumbers)
    ? b.seatNumbers
    : [];

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

              <span
                className="
                  inline-flex
                  items-center
                  gap-1.5
                  rounded-full
                  bg-green-400/10
                  px-2.5
                  py-1
                  text-[10px]
                  font-semibold
                  uppercase
                  tracking-wider
                  text-green-300
                "
              >
                <CheckCircle2 size={12} />

                {b.status || "CONFIRMED"}
              </span>

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
                  ? seats
                      .map(seatLabel)
                      .join(", ")
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

            {/* TOTAL */}

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

      <Icon
        size={17}
        className="
          mt-0.5
          shrink-0
          text-cyan-300
        "
      />

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