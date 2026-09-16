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
import { createBooking, getSeats, getShow } from "../lib/api";
import { formatDate, formatTime, money } from "../lib/formatters";
import Loading from "../components/Loading";
import { useAuth } from "../context/AuthContext";

export default function SeatLayout() {
  const { id } = useParams();
  const navigate = useNavigate();
  const { isAuthenticated } = useAuth();

  const [show, setShow] = React.useState(null);
  const [seats, setSeats] = React.useState([]);
  const [selected, setSelected] = React.useState([]);
  const [loading, setLoading] = React.useState(true);
  const [booking, setBooking] = React.useState(false);

  /* =========================================================
     LOAD SHOW + SEATS
  ========================================================= */
  React.useEffect(() => {
    Promise.all([getShow(id), getSeats(id)])
      .then(([s, ss]) => {
        setShow(s);
        setSeats(Array.isArray(ss) ? ss : []);
      })
      .catch((e) => {
        toast.error(
          e.message || "Unable to load seats"
        );
      })
      .finally(() => {
        setLoading(false);
      });
  }, [id]);

  /* =========================================================
     SELECT / DESELECT SEAT
  ========================================================= */
  const choose = (seat) => {
    if (seat.booked) return;

    setSelected((prev) =>
      prev.some((x) => x.id === seat.id)
        ? prev.filter((x) => x.id !== seat.id)
        : prev.length >= 5
        ? (toast.error("Maximum 5 seats per booking"), prev)
        : [...prev, seat]
    );
  };

  /* =========================================================
     PRICE
  ========================================================= */
  const price = Number(show?.ticketPrice || 0);
  const total = price * selected.length;

  /* =========================================================
     VISUAL SEAT LABEL
     
     10 seats per row:
     
     A1 A2 A3 ... A10
     B1 B2 B3 ... B10
     C1 C2 C3 ... C10
     
     This is only the UI label.
     Backend seat numbers remain S1, S2, S3...
  ========================================================= */
  const label = (index) => {
    return `${String.fromCharCode(
      65 + Math.floor(index / 10)
    )}${(index % 10) + 1}`;
  };

  /* =========================================================
     GET VISUAL LABEL FOR A SELECTED SEAT
     
     Example:
     backend = S28
     position = 27
     visual label = C8
  ========================================================= */
  const getSeatLabel = (seat) => {
    const index = seats.findIndex(
      (item) => item.id === seat.id
    );

    return index >= 0 ? label(index) : "";
  };

  /* =========================================================
     BOOK SEATS
  ========================================================= */
  const book = async () => {
    if (!isAuthenticated) {
      toast.error("Please login to book tickets");
      navigate("/login");
      return;
    }

    if (!selected.length) {
      toast.error("Select at least one seat");
      return;
    }

    try {
      setBooking(true);

      const result = await createBooking({
        showId: Number(id),
        seatIds: selected.map((s) => s.id),
      });

      toast.success(
        `Booking #${result.id} confirmed!`
      );

      navigate("/My-Bookings");
    } catch (e) {
      toast.error(
        e.message ||
          "Booking failed. Please try again."
      );
    } finally {
      setBooking(false);
    }
  };

  /* =========================================================
     LOADING
  ========================================================= */
  if (loading) {
    return (
      <main className="pt-28">
        <Loading label="Preparing your seat map..." />
      </main>
    );
  }

  /* =========================================================
     SHOW NOT FOUND
  ========================================================= */
  if (!show) {
    return (
      <main className="px-5 pt-40 text-center">
        Show not found.
      </main>
    );
  }

  return (
    <main
      className="
        min-h-screen
        px-4
        pb-20
        pt-28
        sm:px-6
        lg:px-10
      "
    >
      <div className="mx-auto max-w-[1280px]">

        {/* ===================================================
            BACK BUTTON
        =================================================== */}
        <button
          onClick={() => navigate(-1)}
          className="
            mb-6
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

        {/* ===================================================
            SHOW INFORMATION
        =================================================== */}
        <div
          className="
            mb-7
            rounded-2xl
            border
            border-white/10
            bg-white/[.03]
            p-5
            sm:p-6
          "
        >
          <div
            className="
              flex
              flex-col
              justify-between
              gap-4
              md:flex-row
              md:items-center
            "
          >
            <div>
              <p
                className="
                  text-xs
                  font-semibold
                  uppercase
                  tracking-[.22em]
                  text-cyan-300
                "
              >
                Select your seats
              </p>

              <h1
                className="
                  mt-2
                  text-2xl
                  font-semibold
                  sm:text-3xl
                "
              >
                {show.movie?.title || "Movie"}
              </h1>

              <p
                className="
                  mt-2
                  flex
                  flex-wrap
                  gap-3
                  text-sm
                  text-white/40
                "
              >
                <span className="flex items-center gap-1">
                  <MapPin size={14} />
                  {show.theatre?.name || "Theatre"}
                </span>

                <span>
                  {formatDate(show.showDate)}
                </span>

                <span className="flex items-center gap-1">
                  <Clock3 size={14} />
                  {formatTime(show.showTime)}
                </span>
              </p>
            </div>

            {/* =================================================
                TICKET PRICE
            ================================================= */}
            <div
              className="
                rounded-xl
                bg-cyan-300/10
                px-4
                py-3
                text-right
              "
            >
              <p className="text-xs text-white/40">
                Ticket price
              </p>

              <p className="text-xl font-semibold text-cyan-200">
                {money(price)}
              </p>
            </div>
          </div>
        </div>

        {/* ===================================================
            SEAT AREA + BOOKING SUMMARY
        =================================================== */}
        <div
          className="
            grid
            gap-8
            xl:grid-cols-[1fr_330px]
          "
        >

          {/* =================================================
              SEAT MAP
          ================================================= */}
          <div
            className="
              rounded-3xl
              border
              border-white/10
              bg-white/[.02]
              p-5
              sm:p-8
            "
          >
            <div
              className="
                mx-auto
                max-w-2xl
                max-md:max-w-3xl
              "
            >

              {/* SCREEN */}
              <div
                className="
                  mb-2
                  h-2
                  rounded-[50%]
                  bg-gradient-to-r
                  from-transparent
                  via-cyan-200/80
                  to-transparent
                  blur-[1px]
                "
              />

              <p
                className="
                  mb-10
                  text-center
                  text-[10px]
                  font-semibold
                  tracking-[.5em]
                  text-white/35
                "
              >
                SCREEN
              </p>

              {/* =================================================
                  SEAT GRID
              ================================================= */}
              <div
                className="
                  grid
                  grid-cols-10
                  gap-1.5
                  sm:gap-2
                  max-md:gap-1.5
                "
              >
                {seats.map((seat, i) => {
                  const active = selected.some(
                    (s) => s.id === seat.id
                  );

                  return (
                    <button
                      key={seat.id}
                      disabled={seat.booked}
                      onClick={() => choose(seat)}
                      title={
                        seat.booked
                          ? "Booked"
                          : `Seat ${label(i)}`
                      }
                      className={`
                        relative
                        aspect-square
                        rounded-md
                        text-[9px]
                        font-semibold
                        transition
                        duration-200

                        sm:rounded-lg
                        sm:text-[10px]

                        ${
                          seat.booked
                            ? "cursor-not-allowed bg-red-500/75 text-white/70"
                            : active
                            ? "scale-105 bg-cyan-300 text-slate-950 shadow-[0_0_18px_rgba(18,207,232,.35)]"
                            : "bg-white/[.08] text-white/45 hover:bg-cyan-300/30 hover:text-cyan-100 hover:-translate-y-0.5"
                        }
                      `}
                    >
                      {label(i)}

                      {active && (
                        <Check
                          size={9}
                          className="
                            absolute
                            right-1
                            top-1
                            sm:right-1.5
                            sm:top-1.5
                          "
                        />
                      )}
                    </button>
                  );
                })}
              </div>

              {/* =================================================
                  SEAT LEGEND
              ================================================= */}
              <div
                className="
                  mt-9
                  flex
                  flex-wrap
                  justify-center
                  gap-5
                  text-xs
                  text-white/40
                "
              >
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
                  Booked
                </span>
              </div>
            </div>
          </div>

          {/* =================================================
              BOOKING SUMMARY
          ================================================= */}
          <aside
            className="
              h-fit
              rounded-3xl
              border
              border-white/10
              bg-[#0c1017]
              p-6
              xl:sticky
              xl:top-24
            "
          >

            {/* =================================================
                SUMMARY HEADER
            ================================================= */}
            <div
              className="
                mb-6
                flex
                items-center
                gap-3
              "
            >
              <span
                className="
                  grid
                  h-10
                  w-10
                  place-items-center
                  rounded-xl
                  bg-cyan-300/10
                  text-cyan-300
                "
              >
                <Ticket size={19} />
              </span>

              <div>
                <p className="text-sm font-semibold">
                  Booking summary
                </p>

                <p className="text-xs text-white/35">
                  Up to 5 seats
                </p>
              </div>
            </div>

            {/* =================================================
                SUMMARY DETAILS
            ================================================= */}
            <div className="space-y-4 text-sm">

              {/* SELECTED COUNT */}
              <div
                className="
                  flex
                  justify-between
                  text-white/45
                "
              >
                <span>Selected</span>

                <span className="text-white">
                  {selected.length} seat
                  {selected.length !== 1
                    ? "s"
                    : ""}
                </span>
              </div>

              {/* =================================================
                  SELECTED SEATS
                  
                  IMPORTANT:
                  Shows A1 / B2 / C8 etc.
                  NOT S28 / S77.
              ================================================= */}
              <div
                className="
                  rounded-xl
                  border
                  border-white/10
                  bg-white/[.03]
                  p-3
                "
              >
                <p
                  className="
                    mb-2
                    text-[11px]
                    uppercase
                    tracking-wider
                    text-white/30
                  "
                >
                  Seats
                </p>

                <p className="text-cyan-200">
                  {selected.length
                    ? selected
                        .map((seat) =>
                          getSeatLabel(seat)
                        )
                        .filter(Boolean)
                        .join(", ")
                    : "No seats selected"}
                </p>
              </div>

              {/* PRICE PER SEAT */}
              <div
                className="
                  flex
                  justify-between
                  border-t
                  border-white/10
                  pt-4
                  text-white/45
                "
              >
                <span>
                  Price / seat
                </span>

                <span>
                  {money(price)}
                </span>
              </div>

              {/* TOTAL */}
              <div
                className="
                  flex
                  items-end
                  justify-between
                "
              >
                <span className="text-white/55">
                  Total
                </span>

                <span
                  className="
                    text-2xl
                    font-semibold
                    text-white
                  "
                >
                  {money(total)}
                </span>
              </div>
            </div>

            {/* =================================================
                CONFIRM BOOKING
            ================================================= */}
            <button
              disabled={
                booking || !selected.length
              }
              onClick={book}
              className="
                mt-6
                flex
                w-full
                items-center
                justify-center
                gap-2
                rounded-xl
                bg-cyan-300
                py-3.5
                text-sm
                font-semibold
                text-slate-950
                transition
                hover:bg-cyan-200
                disabled:cursor-not-allowed
                disabled:opacity-35
              "
            >
              {booking ? (
                <>
                  <span
                    className="
                      h-4
                      w-4
                      animate-spin
                      rounded-full
                      border-2
                      border-slate-950/30
                      border-t-slate-950
                    "
                  />

                  Processing...
                </>
              ) : (
                <>
                  <LockKeyhole size={16} />
                  Confirm booking
                </>
              )}
            </button>

            {/* =================================================
                INFORMATION
            ================================================= */}
            <p
              className="
                mt-4
                flex
                gap-2
                text-[11px]
                leading-5
                text-white/30
              "
            >
              <Info
                size={14}
                className="
                  mt-0.5
                  shrink-0
                "
              />

              Your seat selection is locked only after
              the booking is successfully confirmed.
            </p>
          </aside>
        </div>
      </div>
    </main>
  );
}