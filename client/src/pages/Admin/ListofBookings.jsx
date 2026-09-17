import React from "react";
import {
  CalendarDays,
  Clock3,
  Film,
  Building2,
  Ticket,
  Search,
  RefreshCw,
  X,
  Eye,
  Users,
  IndianRupee,
} from "lucide-react";

import { getAllBookings } from "../../lib/api";
import {
  formatDate,
  formatTime,
  money,
} from "../../lib/formatters";

import Title from "../../components/Admin/Title";

export default function ListofBookings() {

  const [bookings, setBookings] = React.useState([]);
  const [loading, setLoading] = React.useState(true);
  const [refreshing, setRefreshing] = React.useState(false);

  const [search, setSearch] = React.useState("");
  const [statusFilter, setStatusFilter] = React.useState("ALL");

  const [selectedBooking, setSelectedBooking] =
    React.useState(null);

  const [error, setError] = React.useState("");

  const loadBookings = async (isRefresh = false) => {

    try {

      if (isRefresh) {
        setRefreshing(true);
      } else {
        setLoading(true);
      }

      setError("");

      const data = await getAllBookings();

      setBookings(
        Array.isArray(data) ? data : []
      );

    } catch (err) {

      console.error(err);

      setError(
        err.message ||
        "Failed to load bookings"
      );

    } finally {

      setLoading(false);
      setRefreshing(false);
    }
  };

  React.useEffect(() => {
    loadBookings();
  }, []);

  // =========================================================
  // FILTER BOOKINGS
  // =========================================================

  const filteredBookings = React.useMemo(() => {

    const query = search
      .trim()
      .toLowerCase();

    return bookings.filter((booking) => {

      const movie =
        booking.show?.movie?.title || "";

      const theatre =
        booking.show?.theatre?.name || "";

      const email =
        booking.userEmail || "";

      const id =
        String(booking.id || "");

      const status =
        String(
          booking.status || ""
        ).toUpperCase();

      const matchesSearch =
        !query ||
        movie.toLowerCase().includes(query) ||
        theatre.toLowerCase().includes(query) ||
        email.toLowerCase().includes(query) ||
        id.includes(query);

      const matchesStatus =
        statusFilter === "ALL" ||
        status === statusFilter;

      return (
        matchesSearch &&
        matchesStatus
      );
    });

  }, [bookings, search, statusFilter]);

  // =========================================================
  // SUMMARY
  // =========================================================

  const totalRevenue = bookings.reduce(
    (sum, booking) =>
      sum +
      Number(booking.totalAmount || 0),
    0
  );

  const totalTickets = bookings.reduce(
    (sum, booking) =>
      sum +
      Number(booking.numberOfSeats || 0),
    0
  );

  const uniqueCustomers =
    new Set(
      bookings
        .map((booking) =>
          booking.userEmail
        )
        .filter(Boolean)
    ).size;

  // =========================================================
  // STATUS
  // =========================================================

  const getStatus = (booking) => {

    const status =
      String(
        booking.status || "CONFIRMED"
      ).toUpperCase();

    return status;
  };

  const statusClass = (status) => {

    switch (status) {

      case "CANCELLED":
      case "CANCELED":
        return "border-red-400/15 bg-red-400/10 text-red-300";

      case "PENDING":
        return "border-yellow-400/15 bg-yellow-400/10 text-yellow-300";

      case "COMPLETED":
        return "border-blue-400/15 bg-blue-400/10 text-blue-300";

      default:
        return "border-green-400/15 bg-green-400/10 text-green-300";
    }
  };

  return (
    <>
      <Title
        text1="Management"
        text2="Bookings"
      />

      {/* ERROR */}

      {error && (
        <div className="mt-6 flex items-center justify-between rounded-xl border border-red-400/20 bg-red-400/10 px-4 py-3 text-sm text-red-200">

          <span>{error}</span>

          <button
            onClick={() => setError("")}
            className="text-red-200/60 hover:text-red-200"
          >
            <X size={17} />
          </button>

        </div>
      )}

      {/* SUMMARY CARDS */}

      <div className="mt-8 grid grid-cols-1 gap-4 sm:grid-cols-2 xl:grid-cols-4">

        <div className="rounded-2xl border border-white/10 bg-white/[.03] p-5">

          <div className="flex items-center justify-between">

            <span className="grid h-10 w-10 place-items-center rounded-xl bg-cyan-300/10 text-cyan-300">
              <Ticket size={18} />
            </span>

            <span className="text-[10px] font-semibold uppercase tracking-[.18em] text-white/25">
              Bookings
            </span>

          </div>

          <p className="mt-5 text-2xl font-semibold text-white">
            {bookings.length}
          </p>

          <p className="mt-1 text-xs text-white/30">
            Total reservations
          </p>

        </div>

        <div className="rounded-2xl border border-white/10 bg-white/[.03] p-5">

          <div className="flex items-center justify-between">

            <span className="grid h-10 w-10 place-items-center rounded-xl bg-green-400/10 text-green-300">
              <IndianRupee size={18} />
            </span>

            <span className="text-[10px] font-semibold uppercase tracking-[.18em] text-white/25">
              Revenue
            </span>

          </div>

          <p className="mt-5 text-2xl font-semibold text-white">
            {money(totalRevenue)}
          </p>

          <p className="mt-1 text-xs text-white/30">
            From all bookings
          </p>

        </div>

        <div className="rounded-2xl border border-white/10 bg-white/[.03] p-5">

          <div className="flex items-center justify-between">

            <span className="grid h-10 w-10 place-items-center rounded-xl bg-purple-400/10 text-purple-300">
              <Users size={18} />
            </span>

            <span className="text-[10px] font-semibold uppercase tracking-[.18em] text-white/25">
              Customers
            </span>

          </div>

          <p className="mt-5 text-2xl font-semibold text-white">
            {uniqueCustomers}
          </p>

          <p className="mt-1 text-xs text-white/30">
            Unique customers
          </p>

        </div>

        <div className="rounded-2xl border border-white/10 bg-white/[.03] p-5">

          <div className="flex items-center justify-between">

            <span className="grid h-10 w-10 place-items-center rounded-xl bg-orange-400/10 text-orange-300">
              <Ticket size={18} />
            </span>

            <span className="text-[10px] font-semibold uppercase tracking-[.18em] text-white/25">
              Tickets
            </span>

          </div>

          <p className="mt-5 text-2xl font-semibold text-white">
            {totalTickets}
          </p>

          <p className="mt-1 text-xs text-white/30">
            Seats booked
          </p>

        </div>

      </div>

      {/* SEARCH + FILTER */}

      <div className="mt-8 flex flex-col gap-3 lg:flex-row lg:items-center lg:justify-between">

        <div className="relative w-full lg:max-w-md">

          <Search
            size={17}
            className="absolute left-4 top-1/2 -translate-y-1/2 text-white/25"
          />

          <input
            type="text"
            value={search}
            onChange={(e) =>
              setSearch(e.target.value)
            }
            placeholder="Search booking, customer, movie..."
            className="w-full rounded-xl border border-white/10 bg-white/[.03] py-3 pl-11 pr-4 text-sm text-white outline-none placeholder:text-white/20 focus:border-cyan-300/30"
          />

        </div>

        <div className="flex gap-2">

          <select
            value={statusFilter}
            onChange={(e) =>
              setStatusFilter(e.target.value)
            }
            className="rounded-xl border border-white/10 bg-[#0b0f15] px-4 py-3 text-sm text-white/65 outline-none focus:border-cyan-300/30"
          >

            <option value="ALL">
              All Status
            </option>

            <option value="CONFIRMED">
              Confirmed
            </option>

            <option value="PENDING">
              Pending
            </option>

            <option value="COMPLETED">
              Completed
            </option>

            <option value="CANCELLED">
              Cancelled
            </option>

          </select>

          <button
            type="button"
            onClick={() => loadBookings(true)}
            disabled={refreshing}
            className="grid h-[46px] w-[46px] place-items-center rounded-xl border border-white/10 bg-white/[.03] text-white/45 transition hover:bg-white/[.06] hover:text-white disabled:opacity-40"
            title="Refresh bookings"
          >

            <RefreshCw
              size={17}
              className={
                refreshing
                  ? "animate-spin"
                  : ""
              }
            />

          </button>

        </div>

      </div>

      {/* TABLE */}

      <div className="mt-5 overflow-hidden rounded-2xl border border-white/10 bg-white/[.03]">

        {loading ? (

          <div className="p-12 text-center text-white/40">
            Loading bookings...
          </div>

        ) : filteredBookings.length ? (

          <div className="overflow-x-auto">

            <table className="w-full min-w-[1150px] text-left text-sm">

              <thead className="border-b border-white/10 bg-white/[.03] text-[10px] uppercase tracking-[.14em] text-white/30">

                <tr>

                  <th className="px-5 py-4">
                    Booking
                  </th>

                  <th className="px-5 py-4">
                    Customer
                  </th>

                  <th className="px-5 py-4">
                    Movie
                  </th>

                  <th className="px-5 py-4">
                    Theatre
                  </th>

                  <th className="px-5 py-4">
                    Show
                  </th>

                  <th className="px-5 py-4">
                    Seats
                  </th>

                  <th className="px-5 py-4">
                    Amount
                  </th>

                  <th className="px-5 py-4">
                    Status
                  </th>

                  <th className="px-5 py-4 text-right">
                    View
                  </th>

                </tr>

              </thead>

              <tbody>

                {filteredBookings.map(
                  (booking) => {

                    const status =
                      getStatus(booking);

                    return (
                      <tr
                        key={booking.id}
                        className="border-b border-white/[.06] transition hover:bg-white/[.025]"
                      >

                        {/* BOOKING */}

                        <td className="px-5 py-4">

                          <div className="flex items-center gap-3">

                            <span className="grid h-9 w-9 place-items-center rounded-lg bg-cyan-300/10 text-cyan-300">
                              <Ticket size={16} />
                            </span>

                            <div>

                              <p className="font-medium text-white">
                                #{booking.id}
                              </p>

                              <p className="text-[11px] text-white/25">
                                Booking ID
                              </p>

                            </div>

                          </div>

                        </td>

                        {/* CUSTOMER */}

                        <td className="max-w-[190px] px-5 py-4">

                          <p className="truncate text-white/70">
                            {booking.userEmail ||
                              "Unknown customer"}
                          </p>

                        </td>

                        {/* MOVIE */}

                        <td className="px-5 py-4">

                          <div className="flex items-center gap-2">

                            <Film
                              size={14}
                              className="shrink-0 text-white/25"
                            />

                            <span className="max-w-[160px] truncate text-white/70">
                              {booking.show?.movie?.title ||
                                "Movie"}
                            </span>

                          </div>

                        </td>

                        {/* THEATRE */}

                        <td className="px-5 py-4">

                          <div className="flex items-center gap-2">

                            <Building2
                              size={14}
                              className="shrink-0 text-white/25"
                            />

                            <span className="max-w-[130px] truncate text-white/55">
                              {booking.show?.theatre?.name ||
                                "—"}
                            </span>

                          </div>

                        </td>

                        {/* SHOW */}

                        <td className="px-5 py-4">

                          <div>

                            <p className="text-white/60">
                              {booking.show?.showDate
                                ? formatDate(
                                    booking.show.showDate
                                  )
                                : "—"}
                            </p>

                            <p className="mt-1 flex items-center gap-1 text-xs text-white/30">

                              <Clock3 size={12} />

                              {booking.show?.showTime
                                ? formatTime(
                                    booking.show.showTime
                                  )
                                : "—"}

                            </p>

                          </div>

                        </td>

                        {/* SEATS */}

                        <td className="px-5 py-4">

                          <div>

                            <p className="font-medium text-white/75">
                              {booking.numberOfSeats ||
                                0} seats
                            </p>

                            <p className="mt-1 max-w-[150px] truncate text-xs text-cyan-200/60">
                              {Array.isArray(
                                booking.seatNumbers
                              )
                                ? booking.seatNumbers.join(
                                    ", "
                                  )
                                : "—"}
                            </p>

                          </div>

                        </td>

                        {/* AMOUNT */}

                        <td className="px-5 py-4 font-semibold text-cyan-200">

                          {money(
                            booking.totalAmount
                          )}

                        </td>

                        {/* STATUS */}

                        <td className="px-5 py-4">

                          <span
                            className={`inline-flex rounded-full border px-2.5 py-1 text-[10px] font-semibold uppercase tracking-wider ${statusClass(
                              status
                            )}`}
                          >
                            {status}
                          </span>

                        </td>

                        {/* VIEW */}

                        <td className="px-5 py-4">

                          <div className="flex justify-end">

                            <button
                              type="button"
                              onClick={() =>
                                setSelectedBooking(
                                  booking
                                )
                              }
                              className="grid h-9 w-9 place-items-center rounded-lg border border-white/10 bg-white/[.03] text-white/45 transition hover:border-cyan-300/20 hover:bg-cyan-300/10 hover:text-cyan-200"
                              title="View booking"
                            >
                              <Eye size={16} />
                            </button>

                          </div>

                        </td>

                      </tr>
                    );
                  }
                )}

              </tbody>

            </table>

          </div>

        ) : (

          <div className="p-12 text-center">

            <div className="mx-auto grid h-12 w-12 place-items-center rounded-xl bg-white/[.04] text-white/20">
              <Ticket size={20} />
            </div>

            <p className="mt-4 text-sm text-white/40">
              No bookings found
            </p>

            <p className="mt-1 text-xs text-white/20">
              Try changing your search or filter.
            </p>

          </div>

        )}

      </div>

      {/* BOOKING DETAILS MODAL */}

      {selectedBooking && (

        <div
          className="fixed inset-0 z-[100] flex items-center justify-center bg-black/75 p-5 backdrop-blur-md"
          onClick={() =>
            setSelectedBooking(null)
          }
        >

          <div
            onClick={(e) =>
              e.stopPropagation()
            }
            className="w-full max-w-lg overflow-hidden rounded-2xl border border-white/10 bg-[#0b0f15] shadow-[0_25px_80px_rgba(0,0,0,.65)]"
          >

            {/* HEADER */}

            <div className="flex items-center justify-between border-b border-white/[.07] px-6 py-5">

              <div className="flex items-center gap-3">

                <div className="grid h-10 w-10 place-items-center rounded-xl bg-cyan-300/10 text-cyan-300">
                  <Ticket size={18} />
                </div>

                <div>

                  <p className="text-[10px] uppercase tracking-[.2em] text-cyan-300/60">
                    Booking Details
                  </p>

                  <h2 className="mt-1 text-lg font-semibold text-white">
                    Booking #{selectedBooking.id}
                  </h2>

                </div>

              </div>

              <button
                type="button"
                onClick={() =>
                  setSelectedBooking(null)
                }
                className="grid h-8 w-8 place-items-center rounded-lg text-white/30 hover:bg-white/5 hover:text-white"
              >
                <X size={17} />
              </button>

            </div>

            {/* DETAILS */}

            <div className="space-y-4 p-6">

              <div className="rounded-xl border border-white/[.07] bg-white/[.025] p-4">

                <p className="text-xs uppercase tracking-wider text-white/25">
                  Customer
                </p>

                <p className="mt-2 text-sm text-white/75">
                  {selectedBooking.userEmail ||
                    "Unknown customer"}
                </p>

              </div>

              <div className="rounded-xl border border-white/[.07] bg-white/[.025] p-4">

                <div className="flex items-center gap-3">

                  <div className="grid h-10 w-10 place-items-center rounded-lg bg-cyan-300/10 text-cyan-300">
                    <Film size={17} />
                  </div>

                  <div>

                    <p className="text-sm font-semibold text-white">
                      {selectedBooking.show?.movie?.title ||
                        "Movie"}
                    </p>

                    <p className="mt-1 text-xs text-white/35">
                      {selectedBooking.show?.theatre?.name ||
                        "Theatre"}
                    </p>

                  </div>

                </div>

              </div>

              <div className="grid grid-cols-2 gap-3">

                <div className="rounded-xl border border-white/[.07] bg-white/[.025] p-4">

                  <div className="flex items-center gap-2 text-white/30">
                    <CalendarDays size={14} />
                    <span className="text-[10px] uppercase tracking-wider">
                      Date
                    </span>
                  </div>

                  <p className="mt-2 text-sm text-white/70">
                    {selectedBooking.show?.showDate
                      ? formatDate(
                          selectedBooking.show.showDate
                        )
                      : "—"}
                  </p>

                </div>

                <div className="rounded-xl border border-white/[.07] bg-white/[.025] p-4">

                  <div className="flex items-center gap-2 text-white/30">
                    <Clock3 size={14} />
                    <span className="text-[10px] uppercase tracking-wider">
                      Time
                    </span>
                  </div>

                  <p className="mt-2 text-sm text-white/70">
                    {selectedBooking.show?.showTime
                      ? formatTime(
                          selectedBooking.show.showTime
                        )
                      : "—"}
                  </p>

                </div>

              </div>

              <div className="rounded-xl border border-cyan-300/10 bg-cyan-300/[.03] p-4">

                <p className="text-[10px] uppercase tracking-wider text-white/25">
                  Selected Seats
                </p>

                <div className="mt-3 flex flex-wrap gap-2">

                  {Array.isArray(
                    selectedBooking.seatNumbers
                  ) &&
                  selectedBooking.seatNumbers.length ? (

                    selectedBooking.seatNumbers.map(
                      (seat) => (
                        <span
                          key={seat}
                          className="rounded-lg border border-cyan-300/15 bg-cyan-300/10 px-3 py-1.5 text-xs font-medium text-cyan-200"
                        >
                          {seat}
                        </span>
                      )
                    )

                  ) : (
                    <span className="text-sm text-white/35">
                      No seat information
                    </span>
                  )}

                </div>

              </div>

              <div className="flex items-center justify-between rounded-xl border border-white/[.07] bg-white/[.025] p-4">

                <div>

                  <p className="text-xs text-white/30">
                    Total Amount
                  </p>

                  <p className="mt-1 text-xl font-semibold text-cyan-200">
                    {money(
                      selectedBooking.totalAmount
                    )}
                  </p>

                </div>

                <span
                  className={`rounded-full border px-3 py-1.5 text-[10px] font-semibold uppercase tracking-wider ${statusClass(
                    getStatus(selectedBooking)
                  )}`}
                >
                  {getStatus(selectedBooking)}
                </span>

              </div>

            </div>

            {/* CLOSE */}

            <div className="border-t border-white/[.07] px-6 py-4">

              <button
                type="button"
                onClick={() =>
                  setSelectedBooking(null)
                }
                className="w-full rounded-xl border border-white/10 bg-white/[.03] py-3 text-sm font-medium text-white/60 transition hover:bg-white/[.06] hover:text-white"
              >
                Close
              </button>

            </div>

          </div>

        </div>

      )}

    </>
  );
}
