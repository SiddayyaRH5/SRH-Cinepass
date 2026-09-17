import React from "react";
import {
  Activity,
  Building2,
  CalendarDays,
  IndianRupee,
  Ticket,
  Users,
  ArrowRight,
  ShieldCheck,
} from "lucide-react";
import { useNavigate } from "react-router-dom";
import {
  getAllBookings,
  getShows,
  getPendingTheatreVerifications,
} from "../../lib/api";
import Title from "../../components/Admin/Title";
import { money } from "../../lib/formatters";

export default function Dashboard() {
  const navigate = useNavigate();

  const [bookings, setBookings] = React.useState([]);
  const [shows, setShows] = React.useState([]);
  const [pendingVerifications, setPendingVerifications] =
    React.useState([]);

  const [loading, setLoading] = React.useState(true);

  React.useEffect(() => {
    Promise.allSettled([
      getAllBookings(),
      getShows(),
      getPendingTheatreVerifications(),
    ])
      .then(([b, s, v]) => {
        setBookings(
          b.status === "fulfilled" && Array.isArray(b.value)
            ? b.value
            : []
        );

        setShows(
          s.status === "fulfilled" && Array.isArray(s.value)
            ? s.value
            : []
        );

        setPendingVerifications(
          v.status === "fulfilled" && Array.isArray(v.value)
            ? v.value
            : []
        );
      })
      .finally(() => setLoading(false));
  }, []);

  const revenue = bookings.reduce(
    (a, b) => a + Number(b.totalAmount || 0),
    0
  );

  const cards = [
    ["Bookings", bookings.length, Ticket],
    ["Revenue", money(revenue), IndianRupee],
    ["Active Shows", shows.length, CalendarDays],
    [
      "Users",
      new Set(bookings.map((b) => b.userId)).size,
      Users,
    ],
  ];

  return (
    <>
      <Title text1="Overview" text2="Dashboard" />

      {/* =====================================================
          OVERVIEW CARDS
      ===================================================== */}

      <div className="mt-8 grid gap-4 sm:grid-cols-2 xl:grid-cols-4">
        {cards.map(([label, value, Icon], index) => (
          <div
            key={label}
            className="
              rounded-2xl
              border
              border-white/10
              bg-white/[.03]
              p-5
              animate-cine-rise
              transition-all
              duration-300
              hover:-translate-y-1
              hover:border-cyan-300/20
              hover:bg-white/[.045]
            "
            style={{
              animationDelay: `${index * 60}ms`,
            }}
          >
            <div className="flex items-center justify-between">
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
                <Icon size={19} />
              </span>

              <Activity
                size={15}
                className="text-white/20"
              />
            </div>

            <p className="mt-6 text-xs uppercase tracking-wider text-white/30">
              {label}
            </p>

            <p className="mt-1 text-2xl font-semibold text-white">
              {loading ? "—" : value}
            </p>
          </div>
        ))}
      </div>

      {/* =====================================================
          THEATRE VERIFICATION
      ===================================================== */}

      <div
        className="
          mt-6
          overflow-hidden
          rounded-2xl
          border
          border-cyan-400/10
          bg-gradient-to-br
          from-cyan-400/[.07]
          via-white/[.03]
          to-white/[.02]
          animate-cine-rise
        "
      >
        <button
          type="button"
          onClick={() =>
            navigate("/admin/theatre-verification")
          }
          className="
            group
            relative
            w-full
            p-6
            text-left
            transition-all
            duration-300
            hover:bg-white/[.025]
          "
        >
          {/* Glow */}
          <div
            className="
              pointer-events-none
              absolute
              -right-16
              -top-20
              h-48
              w-48
              rounded-full
              bg-cyan-400/10
              blur-3xl
              transition-all
              duration-500
              group-hover:bg-cyan-400/20
            "
          />

          <div
            className="
              relative
              flex
              flex-col
              gap-6
              md:flex-row
              md:items-center
              md:justify-between
            "
          >
            {/* Left */}
            <div className="flex items-start gap-4">
              <div
                className="
                  flex
                  h-12
                  w-12
                  shrink-0
                  items-center
                  justify-center
                  rounded-xl
                  border
                  border-cyan-300/20
                  bg-cyan-300/10
                  text-cyan-300
                  transition-all
                  duration-300
                  group-hover:scale-105
                  group-hover:bg-cyan-300/15
                "
              >
                <Building2 size={22} />
              </div>

              <div>
                <div className="flex flex-wrap items-center gap-3">
                  <h2 className="text-lg font-semibold text-white">
                    Theatre Verification
                  </h2>

                  {!loading && (
                    <span
                      className="
                        rounded-full
                        border
                        border-amber-300/20
                        bg-amber-300/10
                        px-2.5
                        py-1
                        text-[10px]
                        font-bold
                        uppercase
                        tracking-wider
                        text-amber-300
                      "
                    >
                      {pendingVerifications.length} Pending
                    </span>
                  )}
                </div>

                <p className="mt-2 max-w-2xl text-sm leading-6 text-white/40">
                  Review theatre owner applications, verify
                  business details and approve or reject
                  theatre registrations.
                </p>

                <div
                  className="
                    mt-4
                    flex
                    items-center
                    gap-2
                    text-xs
                    font-semibold
                    text-cyan-300
                  "
                >
                  <ShieldCheck size={15} />

                  <span>
                    Manage Theatre Applications
                  </span>

                  <ArrowRight
                    size={15}
                    className="
                      transition-transform
                      duration-300
                      group-hover:translate-x-1
                    "
                  />
                </div>
              </div>
            </div>

            {/* Right */}
            <div
              className="
                hidden
                h-16
                w-16
                shrink-0
                items-center
                justify-center
                rounded-2xl
                border
                border-white/10
                bg-white/[.03]
                md:flex
              "
            >
              <Building2
                size={25}
                className="
                  text-white/20
                  transition-colors
                  duration-300
                  group-hover:text-cyan-300
                "
              />
            </div>
          </div>
        </button>
      </div>

      {/* =====================================================
          SYSTEM STATUS
      ===================================================== */}

      <div
        className="
          mt-6
          rounded-2xl
          border
          border-white/10
          bg-white/[.03]
          p-6
        "
      >
        <div className="flex items-center justify-between">
          <div>
            <h2 className="font-semibold text-white">
              System status
            </h2>

            <p className="mt-1 text-xs text-white/30">
              Current CinePass platform services
            </p>
          </div>

          <Activity
            size={17}
            className="text-cyan-300"
          />
        </div>

        <div className="mt-5 grid gap-3 sm:grid-cols-3">
          <Status
            label="Database"
            text="Connected"
          />

          <Status
            label="Booking API"
            text="Operational"
          />

          <Status
            label="Authentication"
            text="JWT secured"
          />
        </div>
      </div>
    </>
  );
}

function Status({ label, text }) {
  return (
    <div
      className="
        flex
        items-center
        justify-between
        rounded-xl
        border
        border-white/5
        bg-white/[.03]
        px-4
        py-3
      "
    >
      <span className="text-sm text-white/45">
        {label}
      </span>

      <span className="flex items-center gap-2 text-xs text-green-300">
        <span className="h-1.5 w-1.5 rounded-full bg-green-300" />
        {text}
      </span>
    </div>
  );
}