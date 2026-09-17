import React from "react";
import {
  CalendarDays,
  CheckCircle2,
  ChevronDown,
  ChevronLeft,
  ChevronRight,
  Clock3,
  Film,
  MapPin,
  Plus,
  Users,
  X,
} from "lucide-react";
import toast from "react-hot-toast";

import {
  getMovies,
  getTheatresByLocation,
  createShow,
  getLocations,
} from "../../lib/api";

import Title from "../../components/Admin/Title";


/* =========================================================
   DATE HELPERS
========================================================= */

function getTodayString() {
  const now = new Date();

  const year = now.getFullYear();
  const month = String(now.getMonth() + 1).padStart(2, "0");
  const day = String(now.getDate()).padStart(2, "0");

  return `${year}-${month}-${day}`;
}

function parseDate(value) {
  if (!value) return null;

  const [year, month, day] = value
    .split("-")
    .map(Number);

  return new Date(year, month - 1, day);
}

function formatDisplayDate(value) {
  if (!value) return "";

  const date = parseDate(value);

  if (!date) return "";

  return date.toLocaleDateString("en-IN", {
    day: "2-digit",
    month: "short",
    year: "numeric",
  });
}

function getMonthName(date) {
  return date.toLocaleDateString("en-IN", {
    month: "long",
    year: "numeric",
  });
}

function getCalendarDays(monthDate) {
  const year = monthDate.getFullYear();
  const month = monthDate.getMonth();

  const firstDay = new Date(year, month, 1).getDay();
  const daysInMonth = new Date(
    year,
    month + 1,
    0
  ).getDate();

  const previousMonthDays = new Date(
    year,
    month,
    0
  ).getDate();

  const days = [];

  for (let i = firstDay - 1; i >= 0; i--) {
    days.push({
      date: new Date(
        year,
        month - 1,
        previousMonthDays - i
      ),
      currentMonth: false,
    });
  }

  for (let day = 1; day <= daysInMonth; day++) {
    days.push({
      date: new Date(year, month, day),
      currentMonth: true,
    });
  }

  while (days.length < 42) {
    const nextDay = days.length - (firstDay + daysInMonth) + 1;

    days.push({
      date: new Date(year, month + 1, nextDay),
      currentMonth: false,
    });
  }

  return days;
}

function dateToString(date) {
  const year = date.getFullYear();
  const month = String(
    date.getMonth() + 1
  ).padStart(2, "0");
  const day = String(
    date.getDate()
  ).padStart(2, "0");

  return `${year}-${month}-${day}`;
}


/* =========================================================
   TIME HELPERS
========================================================= */

function generateTimeSlots() {
  const slots = [];

  // Cinema operating window:
  // 08:00 AM → 11:45 PM

  for (let hour = 8; hour <= 23; hour++) {
    for (let minute = 0; minute < 60; minute += 15) {

      if (hour === 23 && minute > 45) {
        continue;
      }

      const value =
        `${String(hour).padStart(2, "0")}:` +
        `${String(minute).padStart(2, "0")}`;

      const date = new Date();
      date.setHours(hour, minute, 0, 0);

      const label = date.toLocaleTimeString(
        "en-IN",
        {
          hour: "2-digit",
          minute: "2-digit",
          hour12: true,
        }
      );

      slots.push({
        value,
        label,
      });
    }
  }

  return slots;
}

const TIME_SLOTS = generateTimeSlots();


/* =========================================================
   MAIN COMPONENT
========================================================= */

export default function AddShows() {

  const today = getTodayString();

  const [movies, setMovies] = React.useState([]);
  const [theatres, setTheatres] = React.useState([]);
  const [locations, setLocations] = React.useState([]);

  const [movieId, setMovieId] = React.useState("");
  const [theatreId, setTheatreId] = React.useState("");
  const [locationId, setLocationId] = React.useState("");

  const [date, setDate] = React.useState("");
  const [time, setTime] = React.useState("");
  const [price, setPrice] = React.useState("");

  const [loadingMovies, setLoadingMovies] =
    React.useState(true);

  const [loadingLocations, setLoadingLocations] =
    React.useState(true);

  const [loadingTheatres, setLoadingTheatres] =
    React.useState(false);

  const [saving, setSaving] =
    React.useState(false);

  const [showDatePicker, setShowDatePicker] =
    React.useState(false);

  const [showTimePicker, setShowTimePicker] =
    React.useState(false);

  const [calendarMonth, setCalendarMonth] =
    React.useState(() => {
      const now = new Date();

      return new Date(
        now.getFullYear(),
        now.getMonth(),
        1
      );
    });


  /* =====================================================
     LOAD MOVIES + LOCATIONS
  ===================================================== */

  React.useEffect(() => {

    const loadData = async () => {

      try {

        setLoadingMovies(true);
        setLoadingLocations(true);

        const [moviesData, locationsData] =
          await Promise.all([
            getMovies(),
            getLocations(),
          ]);

        setMovies(
          Array.isArray(moviesData)
            ? moviesData
            : []
        );

        setLocations(
          Array.isArray(locationsData)
            ? locationsData
            : []
        );

      } catch (error) {

        toast.error(
          error.message ||
          "Unable to load show data"
        );

      } finally {

        setLoadingMovies(false);
        setLoadingLocations(false);
      }
    };

    loadData();

  }, []);


  /* =====================================================
     LOAD THEATRES WHEN CITY CHANGES
  ===================================================== */

  React.useEffect(() => {

    setTheatreId("");

    if (!locationId) {
      setTheatres([]);
      return;
    }

    const loadTheatres = async () => {

      try {

        setLoadingTheatres(true);

        const data =
          await getTheatresByLocation(
            locationId
          );

        setTheatres(
          Array.isArray(data)
            ? data
            : []
        );

      } catch (error) {

        setTheatres([]);

        toast.error(
          error.message ||
          "Unable to load theatres"
        );

      } finally {

        setLoadingTheatres(false);
      }
    };

    loadTheatres();

  }, [locationId]);


  /* =====================================================
     SELECTED THEATRE
  ===================================================== */

  const selectedTheatre =
    theatres.find(
      (theatre) =>
        String(theatre.id) ===
        String(theatreId)
    );


  /* =====================================================
     SELECTED MOVIE
  ===================================================== */

  const selectedMovie =
    movies.find(
      (movie) =>
        String(movie.id) ===
        String(movieId)
    );


  /* =====================================================
     DATE SELECTION
  ===================================================== */

  const chooseDate = (selectedDate) => {

    const value =
      dateToString(selectedDate);

    if (value < today) {
      return;
    }

    setDate(value);
    setShowDatePicker(false);
  };


  /* =====================================================
     CREATE SHOW
  ===================================================== */

  const submit = async (e) => {

    e.preventDefault();

    if (!movieId) {
      return toast.error(
        "Select a movie"
      );
    }

    if (!locationId) {
      return toast.error(
        "Select a city"
      );
    }

    if (!theatreId) {
      return toast.error(
        "Select a theatre"
      );
    }

    if (!date) {
      return toast.error(
        "Select the show date"
      );
    }

    if (date < today) {
      return toast.error(
        "Show date cannot be in the past"
      );
    }

    if (!time) {
      return toast.error(
        "Select the show time"
      );
    }

    const numericPrice =
      Number(price);

    if (
      !Number.isFinite(numericPrice) ||
      numericPrice <= 0
    ) {
      return toast.error(
        "Enter a valid ticket price"
      );
    }

    if (numericPrice > 10000) {
      return toast.error(
        "Ticket price cannot exceed ₹10,000"
      );
    }

    try {

      setSaving(true);

      await createShow({
        movie: {
          id: Number(movieId),
        },

        theatre: {
          id: Number(theatreId),
        },

        showDate: date,

        showTime:
          `${time}:00`,

        ticketPrice:
          numericPrice,
      });

      toast.success(
        "Show created and seats generated"
      );

      setDate("");
      setTime("");
      setPrice("");

    } catch (error) {

      toast.error(
        error.message ||
        "Could not create show"
      );

    } finally {

      setSaving(false);
    }
  };


  /* =====================================================
     CALENDAR NAVIGATION
  ===================================================== */

  const previousMonth = () => {

    const previous =
      new Date(calendarMonth);

    previous.setMonth(
      previous.getMonth() - 1
    );

    const currentMonth =
      new Date(
        new Date().getFullYear(),
        new Date().getMonth(),
        1
      );

    if (previous >= currentMonth) {
      setCalendarMonth(previous);
    }
  };

  const nextMonth = () => {

    const next =
      new Date(calendarMonth);

    next.setMonth(
      next.getMonth() + 1
    );

    setCalendarMonth(next);
  };


  const calendarDays =
    getCalendarDays(calendarMonth);


  return (
    <>
      <Title
        text1="Management"
        text2="Add Show"
      />

      <div className="mt-8 grid gap-6 lg:grid-cols-[1fr_330px]">

        {/* =================================================
            FORM
        ================================================= */}

        <form
          onSubmit={submit}
          className="
            rounded-2xl
            border border-white/10
            bg-white/[.03]
            p-6
            sm:p-8
          "
        >

          <div className="mb-7">

            <h2 className="text-xl font-semibold">
              Schedule a screening
            </h2>

            <p className="mt-1 text-sm text-white/35">
              Create a show and automatically generate its seats.
            </p>

          </div>


          <div className="grid gap-5">


            {/* =================================================
                MOVIE
            ================================================= */}

            <Field
              label="Movie"
              icon={Film}
            >

              <select
                value={movieId}
                onChange={(e) =>
                  setMovieId(e.target.value)
                }
              >

                <option value="">
                  {loadingMovies
                    ? "Loading movies..."
                    : "Select movie"}
                </option>

                {movies.map((movie) => (
                  <option
                    key={movie.id}
                    value={movie.id}
                  >
                    {movie.title}
                  </option>
                ))}

              </select>

            </Field>


            {/* =================================================
                CITY
            ================================================= */}

            <Field
              label="City"
              icon={MapPin}
            >

              <select
                value={locationId}
                onChange={(e) =>
                  setLocationId(e.target.value)
                }
              >

                <option value="">
                  {loadingLocations
                    ? "Loading cities..."
                    : "Select city"}
                </option>

                {locations.map((location) => (
                  <option
                    key={location.id}
                    value={location.id}
                  >
                    {location.city}
                  </option>
                ))}

              </select>

            </Field>


            {/* =================================================
                THEATRE
            ================================================= */}

            <Field
              label="Theatre"
              icon={Film}
            >

              <select
                value={theatreId}
                onChange={(e) =>
                  setTheatreId(e.target.value)
                }
                disabled={
                  !locationId ||
                  loadingTheatres
                }
              >

                <option value="">
                  {!locationId
                    ? "Select a city first"
                    : loadingTheatres
                    ? "Loading theatres..."
                    : theatres.length === 0
                    ? "No theatres in this city"
                    : "Select theatre"}
                </option>

                {theatres.map((theatre) => (
                  <option
                    key={theatre.id}
                    value={theatre.id}
                  >
                    {theatre.name}
                  </option>
                ))}

              </select>

            </Field>


            {/* =================================================
                THEATRE CAPACITY
            ================================================= */}

            {selectedTheatre && (
              <div
                className="
                  -mt-2
                  flex
                  items-center
                  justify-between
                  rounded-xl
                  border border-cyan-300/10
                  bg-cyan-300/[.04]
                  px-4
                  py-3
                "
              >

                <div className="flex items-center gap-3">

                  <span
                    className="
                      grid
                      h-9
                      w-9
                      place-items-center
                      rounded-lg
                      bg-cyan-300/10
                      text-cyan-300
                    "
                  >
                    <Users size={16} />
                  </span>

                  <div>

                    <p className="text-xs text-white/35">
                      Theatre capacity
                    </p>

                    <p className="mt-0.5 text-sm text-white/75">
                      {selectedTheatre.name}
                    </p>

                  </div>

                </div>

                <span className="text-sm font-semibold text-cyan-200">
                  {selectedTheatre.totalSeats} seats
                </span>

              </div>
            )}


            {/* =================================================
                DATE + TIME + PRICE
            ================================================= */}

            <div className="grid gap-5 sm:grid-cols-3">


              {/* DATE */}

              <div className="relative">

                <Field
                  label="Date"
                  icon={CalendarDays}
                >

                  <button
                    type="button"
                    onClick={() => {
                      setShowDatePicker(
                        !showDatePicker
                      );
                      setShowTimePicker(false);
                    }}
                    className="
                      flex
                      h-12
                      w-full
                      items-center
                      justify-between
                      rounded-xl
                      border
                      border-white/10
                      bg-white/[.04]
                      px-4
                      text-left
                      text-sm
                      text-white
                      transition
                      hover:border-cyan-300/30
                    "
                  >

                    <span
                      className={
                        date
                          ? "text-white"
                          : "text-white/25"
                      }
                    >
                      {date
                        ? formatDisplayDate(date)
                        : "Select date"}
                    </span>

                    <CalendarDays
                      size={17}
                      className="text-white/35"
                    />

                  </button>

                </Field>


                {/* CUSTOM CALENDAR */}

                {showDatePicker && (
                  <div
                    className="
                      absolute
                      left-0
                      top-[76px]
                      z-50
                      w-[310px]
                      rounded-2xl
                      border
                      border-white/10
                      bg-[#0c1017]
                      p-4
                      shadow-2xl
                      shadow-black/50
                    "
                  >

                    <div className="flex items-center justify-between">

                      <button
                        type="button"
                        onClick={previousMonth}
                        className="
                          grid
                          h-9
                          w-9
                          place-items-center
                          rounded-lg
                          text-white/50
                          transition
                          hover:bg-white/[.05]
                          hover:text-white
                        "
                      >
                        <ChevronLeft size={18} />
                      </button>

                      <span className="text-sm font-semibold">
                        {getMonthName(
                          calendarMonth
                        )}
                      </span>

                      <button
                        type="button"
                        onClick={nextMonth}
                        className="
                          grid
                          h-9
                          w-9
                          place-items-center
                          rounded-lg
                          text-white/50
                          transition
                          hover:bg-white/[.05]
                          hover:text-white
                        "
                      >
                        <ChevronRight size={18} />
                      </button>

                    </div>


                    <div className="mt-4 grid grid-cols-7 text-center">

                      {[
                        "S",
                        "M",
                        "T",
                        "W",
                        "T",
                        "F",
                        "S",
                      ].map((day, index) => (
                        <span
                          key={`${day}-${index}`}
                          className="
                            py-2
                            text-[10px]
                            font-semibold
                            text-white/25
                          "
                        >
                          {day}
                        </span>
                      ))}


                      {calendarDays.map(
                        ({
                          date: calendarDate,
                          currentMonth,
                        }) => {

                          const value =
                            dateToString(
                              calendarDate
                            );

                          const disabled =
                            value < today;

                          const selected =
                            value === date;

                          const todayDate =
                            value === today;

                          return (
                            <button
                              type="button"
                              key={value}
                              disabled={disabled}
                              onClick={() =>
                                chooseDate(
                                  calendarDate
                                )
                              }
                              className={`
                                relative
                                grid
                                h-9
                                place-items-center
                                rounded-lg
                                text-xs
                                transition

                                ${
                                  !currentMonth
                                    ? "text-white/15"
                                    : disabled
                                    ? "cursor-not-allowed text-white/15"
                                    : "text-white/65 hover:bg-cyan-300/10 hover:text-white"
                                }

                                ${
                                  selected
                                    ? "bg-cyan-300 font-semibold text-black hover:bg-cyan-200"
                                    : ""
                                }
                              `}
                            >

                              {calendarDate.getDate()}

                              {todayDate &&
                                !selected && (
                                  <span
                                    className="
                                      absolute
                                      bottom-1
                                      h-1
                                      w-1
                                      rounded-full
                                      bg-cyan-300
                                    "
                                  />
                                )}

                            </button>
                          );
                        }
                      )}

                    </div>


                    <div
                      className="
                        mt-3
                        flex
                        items-center
                        justify-between
                        border-t
                        border-white/[.06]
                        pt-3
                      "
                    >

                      <span className="text-[11px] text-white/25">
                        Past dates unavailable
                      </span>

                      <button
                        type="button"
                        onClick={() => {
                          setDate(today);
                          setCalendarMonth(
                            new Date(
                              new Date().getFullYear(),
                              new Date().getMonth(),
                              1
                            )
                          );
                          setShowDatePicker(false);
                        }}
                        className="
                          text-[11px]
                          font-medium
                          text-cyan-300
                          hover:text-cyan-200
                        "
                      >
                        Today
                      </button>

                    </div>

                  </div>
                )}

              </div>


              {/* TIME */}

              <div className="relative">

                <Field
                  label="Time"
                  icon={Clock3}
                >

                  <button
                    type="button"
                    onClick={() => {
                      setShowTimePicker(
                        !showTimePicker
                      );
                      setShowDatePicker(false);
                    }}
                    className="
                      flex
                      h-12
                      w-full
                      items-center
                      justify-between
                      rounded-xl
                      border
                      border-white/10
                      bg-white/[.04]
                      px-4
                      text-left
                      text-sm
                      text-white
                      transition
                      hover:border-cyan-300/30
                    "
                  >

                    <span
                      className={
                        time
                          ? "text-white"
                          : "text-white/25"
                      }
                    >
                      {time
                        ? TIME_SLOTS.find(
                            (slot) =>
                              slot.value === time
                          )?.label
                        : "Select time"}
                    </span>

                    <ChevronDown
                      size={17}
                      className="text-white/35"
                    />

                  </button>

                </Field>


                {/* CUSTOM TIME PICKER */}

                {showTimePicker && (
                  <div
                    className="
                      absolute
                      left-0
                      top-[76px]
                      z-50
                      w-[290px]
                      rounded-2xl
                      border
                      border-white/10
                      bg-[#0c1017]
                      p-4
                      shadow-2xl
                      shadow-black/50
                    "
                  >

                    <div className="mb-3 flex items-center justify-between">

                      <div>

                        <p className="text-sm font-semibold">
                          Select show time
                        </p>

                        <p className="mt-1 text-[11px] text-white/25">
                          15-minute screening slots
                        </p>

                      </div>

                      <button
                        type="button"
                        onClick={() =>
                          setShowTimePicker(false)
                        }
                        className="
                          grid
                          h-8
                          w-8
                          place-items-center
                          rounded-lg
                          text-white/35
                          hover:bg-white/[.05]
                          hover:text-white
                        "
                      >
                        <X size={15} />
                      </button>

                    </div>


                    <div className="max-h-[260px] overflow-y-auto pr-1">

                      <div className="grid grid-cols-3 gap-2">

                        {TIME_SLOTS.map(
                          (slot) => (

                            <button
                              type="button"
                              key={slot.value}
                              onClick={() => {
                                setTime(
                                  slot.value
                                );
                                setShowTimePicker(
                                  false
                                );
                              }}
                              className={`
                                rounded-lg
                                border
                                px-2
                                py-2.5
                                text-xs
                                transition

                                ${
                                  time ===
                                  slot.value
                                    ? "border-cyan-300/40 bg-cyan-300/15 text-cyan-200"
                                    : "border-white/[.06] bg-white/[.025] text-white/50 hover:border-white/15 hover:bg-white/[.05] hover:text-white"
                                }
                              `}
                            >
                              {slot.label}

                            </button>

                          )
                        )}

                      </div>

                    </div>

                  </div>
                )}

              </div>


              {/* PRICE */}

              <Field
                label="Ticket price"
                icon={Plus}
              >

                <input
                  type="number"
                  min="1"
                  max="10000"
                  step="1"
                  value={price}
                  onChange={(e) =>
                    setPrice(e.target.value)
                  }
                  placeholder="₹ 220"
                />

              </Field>

            </div>

          </div>


          {/* =================================================
              CREATE BUTTON
          ================================================= */}

          <button
            type="submit"
            disabled={saving}
            className="
              mt-7
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
              text-black
              transition
              hover:bg-cyan-200
              hover:shadow-[0_0_35px_rgba(18,207,232,.12)]
              disabled:cursor-not-allowed
              disabled:opacity-40
            "
          >

            {saving ? (
              <>
                <span
                  className="
                    h-4
                    w-4
                    animate-spin
                    rounded-full
                    border-2
                    border-black/30
                    border-t-black
                  "
                />

                Creating...

              </>
            ) : (
              <>
                <Plus size={17} />
                Create show
              </>
            )}

          </button>

        </form>


        {/* =================================================
            INFORMATION PANEL
        ================================================= */}

        <div
          className="
            rounded-2xl
            border border-white/10
            bg-gradient-to-br
            from-cyan-400/10
            to-white/[.02]
            p-6
          "
        >

          <CheckCircle2
            className="text-cyan-300"
          />

          <h3 className="mt-5 font-semibold">
            Automatic seat generation
          </h3>

          <p className="mt-2 text-sm leading-6 text-white/40">
            Every new show uses the theatre capacity
            to generate its available seat inventory
            automatically in the backend.
          </p>


          {selectedMovie && (
            <div className="mt-6 rounded-xl bg-white/[.03] p-4">

              <p className="text-[10px] font-semibold uppercase tracking-[.18em] text-white/25">
                Selected movie
              </p>

              <div className="mt-3 flex items-center gap-3">

                <div
                  className="
                    grid
                    h-10
                    w-10
                    shrink-0
                    place-items-center
                    rounded-lg
                    bg-cyan-300/10
                    text-cyan-300
                  "
                >
                  <Film size={17} />
                </div>

                <div className="min-w-0">

                  <p className="truncate text-sm font-medium">
                    {selectedMovie.title}
                  </p>

                  <p className="mt-1 text-xs text-white/30">
                    Screening
                  </p>

                </div>

              </div>

            </div>
          )}


          {selectedTheatre && (
            <div className="mt-3 rounded-xl bg-white/[.03] p-4">

              <p className="text-[10px] font-semibold uppercase tracking-[.18em] text-white/25">
                Selected theatre
              </p>

              <div className="mt-3 flex items-center justify-between">

                <div className="flex items-center gap-3">

                  <div
                    className="
                      grid
                      h-10
                      w-10
                      shrink-0
                      place-items-center
                      rounded-lg
                      bg-cyan-300/10
                      text-cyan-300
                    "
                  >
                    <Users size={17} />
                  </div>

                  <div>

                    <p className="text-sm font-medium">
                      {selectedTheatre.name}
                    </p>

                    <p className="mt-1 text-xs text-white/30">
                      Theatre capacity
                    </p>

                  </div>

                </div>

                <span className="text-sm font-semibold text-cyan-200">
                  {selectedTheatre.totalSeats}
                </span>

              </div>

            </div>
          )}


          <div className="mt-4 space-y-3">

            <InfoRow
              icon={CalendarDays}
              label="Date"
              value={
                date
                  ? formatDisplayDate(date)
                  : "Not selected"
              }
            />

            <InfoRow
              icon={Clock3}
              label="Time"
              value={
                time
                  ? TIME_SLOTS.find(
                      (slot) =>
                        slot.value === time
                    )?.label
                  : "Not selected"
              }
            />

            <InfoRow
              icon={Plus}
              label="Ticket price"
              value={
                price
                  ? `₹${Number(price).toLocaleString("en-IN")}`
                  : "Not set"
              }
            />

          </div>


          <div
            className="
              mt-5
              rounded-xl
              border border-cyan-300/10
              bg-cyan-300/[.04]
              p-4
            "
          >

            <p className="text-xs font-medium text-cyan-200">
              Multiple screenings allowed
            </p>

            <p className="mt-1 text-[11px] leading-5 text-white/30">
              The same movie can be scheduled multiple
              times in the same theatre on the same day
              as long as the screening time is different.
            </p>

          </div>

        </div>

      </div>
    </>
  );
}


/* =========================================================
   FIELD
========================================================= */

function Field({
  label,
  icon: Icon,
  children,
}) {

  return (
    <label className="block">

      <span
        className="
          mb-2
          flex
          items-center
          gap-2
          text-xs
          text-white/45
        "
      >

        <Icon
          size={14}
          className="text-cyan-300"
        />

        {label}

      </span>

      <div
        className="
          [&_input]:h-12
          [&_input]:w-full
          [&_input]:rounded-xl
          [&_input]:border
          [&_input]:border-white/10
          [&_input]:bg-white/[.04]
          [&_input]:px-4
          [&_input]:text-sm
          [&_input]:text-white
          [&_input]:outline-none
          [&_input]:placeholder:text-white/20
          [&_input]:focus:border-cyan-300/40

          [&_select]:h-12
          [&_select]:w-full
          [&_select]:rounded-xl
          [&_select]:border
          [&_select]:border-white/10
          [&_select]:bg-[#10151e]
          [&_select]:px-4
          [&_select]:text-sm
          [&_select]:text-white
          [&_select]:outline-none
          [&_select]:focus:border-cyan-300/40
          [&_select]:disabled:cursor-not-allowed
          [&_select]:disabled:opacity-40
        "
      >
        {children}
      </div>

    </label>
  );
}


/* =========================================================
   INFO ROW
========================================================= */

function InfoRow({
  icon: Icon,
  label,
  value,
}) {

  return (
    <div
      className="
        flex
        items-center
        justify-between
        rounded-xl
        bg-white/[.03]
        px-4
        py-3
      "
    >

      <span className="flex items-center gap-2 text-xs text-white/40">

        <Icon
          size={14}
          className="text-cyan-300"
        />

        {label}

      </span>

      <span className="max-w-[150px] truncate text-right text-xs text-white/70">
        {value}
      </span>

    </div>
  );
}