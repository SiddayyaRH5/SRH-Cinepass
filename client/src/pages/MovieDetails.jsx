import React from "react";
import { useNavigate, useParams } from "react-router-dom";
import {
  AlertCircle,
  ArrowLeft,
  CalendarDays,
  Clock3,
  Loader2,
  MapPin,
  Play,
  RefreshCw,
  Star,
} from "lucide-react";

import {
  getMovie,
  getMovieCities,
  getMovieShowDates,
  getMovieShowTimes,
  getMovieTheatres,
} from "../lib/api";
import Loading from "../components/Loading";
import { useLocationContext } from "../lib/LocationContext";
import { formatDate, formatTime, money } from "../lib/formatters";

// ---------------------------------------------------------------------------
// Small presentational helpers. Kept local so the booking panel stays readable
// and reuses the existing SRH CinePass dark/cyan visual language.
// ---------------------------------------------------------------------------

function StepHeader({ number, title, hint }) {
  return (
    <div className="mb-3 flex items-center gap-3">
      <span className="grid h-7 w-7 shrink-0 place-items-center rounded-full bg-cyan-300/10 text-[11px] font-semibold text-cyan-300">
        {number}
      </span>

      <div className="min-w-0">
        <h3 className="text-sm font-semibold text-white/85">{title}</h3>

        {hint && (
          <p className="mt-0.5 text-[11px] text-white/35">{hint}</p>
        )}
      </div>
    </div>
  );
}

function InlineLoading({ label }) {
  return (
    <div className="flex items-center gap-2 rounded-xl border border-white/10 bg-white/[.03] px-4 py-3 text-sm text-white/45">
      <Loader2 size={15} className="animate-spin" />
      {label}
    </div>
  );
}

function EmptyState({ children }) {
  return (
    <div className="rounded-xl border border-white/10 bg-white/[.03] px-4 py-5 text-sm text-white/40">
      {children}
    </div>
  );
}

function ErrorState({ message, onRetry }) {
  return (
    <div className="flex flex-wrap items-center justify-between gap-3 rounded-xl border border-red-400/25 bg-red-400/[.07] px-4 py-3 text-sm text-red-200">
      <span className="flex items-center gap-2">
        <AlertCircle size={15} className="shrink-0" />
        {message || "Something went wrong."}
      </span>

      {onRetry && (
        <button
          type="button"
          onClick={onRetry}
          className="flex items-center gap-1.5 rounded-lg border border-red-300/30 px-2.5 py-1 text-xs text-red-100 transition hover:bg-red-300/10"
        >
          <RefreshCw size={12} />
          Retry
        </button>
      )}
    </div>
  );
}

function SelectChip({ active, onClick, children }) {
  return (
    <button
      type="button"
      onClick={onClick}
      aria-pressed={active}
      className={`
        rounded-full border px-4 py-2 text-sm transition
        ${
          active
            ? "border-cyan-300/50 bg-cyan-300/15 text-cyan-100"
            : "border-white/10 bg-white/[.03] text-white/65 hover:border-cyan-300/30 hover:text-white"
        }
      `}
    >
      {children}
    </button>
  );
}

/**
 * Shown instead of the booking flow when a movie has been retired from the
 * active catalogue. The movie page itself still works, so an old bookmark or
 * a shared link explains what happened instead of failing.
 */
function ArchivedNotice() {
  return (
    <div
      role="status"
      className="rounded-2xl border border-amber-300/25 bg-amber-300/[.07] px-5 py-6 text-sm text-amber-100/80"
    >
      <p className="font-semibold text-amber-100">
        This movie is no longer in theatres.
      </p>

      <p className="mt-2 leading-6 text-amber-100/60">
        It has been retired from the active catalogue, so new bookings are
        closed. Existing tickets, bookings and payment records are unaffected.
      </p>
    </div>
  );
}

export default function MovieDetails() {
  const { id } = useParams();
  const navigate = useNavigate();
  const { selectedLocation } = useLocationContext();

  const [movie, setMovie] = React.useState(null);
  const [movieLoading, setMovieLoading] = React.useState(true);
  const [movieError, setMovieError] = React.useState("");
  const [archived, setArchived] = React.useState(false);
  const [movieLoaded, setMovieLoaded] = React.useState(false);

  const [retryToken, setRetryToken] = React.useState(0);

  const [cities, setCities] = React.useState([]);
  const [citiesLoading, setCitiesLoading] = React.useState(true);
  const [citiesError, setCitiesError] = React.useState("");
  const [selectedCityId, setSelectedCityId] = React.useState(null);

  const [theatres, setTheatres] = React.useState([]);
  const [theatresLoading, setTheatresLoading] = React.useState(false);
  const [theatresError, setTheatresError] = React.useState("");
  const [selectedTheatreId, setSelectedTheatreId] = React.useState(null);

  const [dates, setDates] = React.useState([]);
  const [datesLoading, setDatesLoading] = React.useState(false);
  const [datesError, setDatesError] = React.useState("");
  const [selectedDate, setSelectedDate] = React.useState(null);

  const [showtimes, setShowtimes] = React.useState([]);
  const [showtimesLoading, setShowtimesLoading] = React.useState(false);
  const [showtimesError, setShowtimesError] = React.useState("");

  const retry = React.useCallback(() => {
    setRetryToken((value) => value + 1);
  }, []);

  // Movie details.
  React.useEffect(() => {
    let active = true;

    setMovieLoading(true);
    setMovieError("");

    getMovie(id)
      .then((data) => {
        if (!active) return;

        setMovie(data);
        setArchived(data?.status === "ARCHIVED");
      })
      .catch((error) => {
        if (active) {
          setMovie(null);
          setArchived(false);
          setMovieError(error.message || "Unable to load this movie.");
        }
      })
      .finally(() => {
        if (!active) return;

        setMovieLoading(false);
        setMovieLoaded(true);
      });

    return () => {
      active = false;
    };
  }, [id, retryToken]);

  // Cities that currently have bookable shows for this movie.
  React.useEffect(() => {
    let active = true;

    // The lifecycle state decides whether a discovery request is made at
    // all, so the movie has to be loaded first.
    if (!movieLoaded) {
      return undefined;
    }

    // A retired movie never has bookable shows, so no discovery request is
    // made for it at all.
    if (archived) {
      setCities([]);
      setSelectedCityId(null);
      setCitiesError("");
      setCitiesLoading(false);
      return undefined;
    }

    setCitiesLoading(true);
    setCitiesError("");

    getMovieCities(id)
      .then((data) => {
        if (!active) return;

        const list = Array.isArray(data) ? data : [];
        setCities(list);

        const preferred =
          selectedLocation &&
          list.find(
            (city) => String(city.id) === String(selectedLocation.id)
          );

        setSelectedCityId(preferred ? preferred.id : list[0]?.id ?? null);
      })
      .catch((error) => {
        if (!active) return;

        setCities([]);
        setSelectedCityId(null);
        setCitiesError(error.message || "Unable to load cities.");
      })
      .finally(() => {
        if (active) setCitiesLoading(false);
      });

    return () => {
      active = false;
    };
  }, [id, selectedLocation, retryToken, archived, movieLoaded]);

  // Theatres for the selected city.
  React.useEffect(() => {
    let active = true;

    if (!selectedCityId) {
      setTheatres([]);
      setSelectedTheatreId(null);
      setTheatresError("");
      setTheatresLoading(false);
      return undefined;
    }

    setTheatresLoading(true);
    setTheatresError("");

    getMovieTheatres(id, selectedCityId)
      .then((data) => {
        if (!active) return;

        const list = Array.isArray(data) ? data : [];
        setTheatres(list);

        setSelectedTheatreId((previous) =>
          list.some((theatre) => String(theatre.id) === String(previous))
            ? previous
            : list[0]?.id ?? null
        );
      })
      .catch((error) => {
        if (!active) return;

        setTheatres([]);
        setSelectedTheatreId(null);
        setTheatresError(error.message || "Unable to load theatres.");
      })
      .finally(() => {
        if (active) setTheatresLoading(false);
      });

    return () => {
      active = false;
    };
  }, [id, selectedCityId, retryToken]);

  // Dates that still have bookable shows at the selected theatre.
  React.useEffect(() => {
    let active = true;

    if (!selectedTheatreId) {
      setDates([]);
      setSelectedDate(null);
      setDatesError("");
      setDatesLoading(false);
      return undefined;
    }

    setDatesLoading(true);
    setDatesError("");

    getMovieShowDates(id, selectedTheatreId)
      .then((data) => {
        if (!active) return;

        const list = Array.isArray(data) ? data : [];
        setDates(list);

        setSelectedDate((previous) =>
          list.some((entry) => entry.date === previous)
            ? previous
            : list[0]?.date ?? null
        );
      })
      .catch((error) => {
        if (!active) return;

        setDates([]);
        setSelectedDate(null);
        setDatesError(error.message || "Unable to load dates.");
      })
      .finally(() => {
        if (active) setDatesLoading(false);
      });

    return () => {
      active = false;
    };
  }, [id, selectedTheatreId, retryToken]);

  // Showtimes for the selected date.
  React.useEffect(() => {
    let active = true;

    if (!selectedTheatreId || !selectedDate) {
      setShowtimes([]);
      setShowtimesError("");
      setShowtimesLoading(false);
      return undefined;
    }

    setShowtimesLoading(true);
    setShowtimesError("");

    getMovieShowTimes(id, selectedTheatreId, selectedDate)
      .then((data) => {
        if (active) {
          setShowtimes(Array.isArray(data) ? data : []);
        }
      })
      .catch((error) => {
        if (!active) return;

        setShowtimes([]);
        setShowtimesError(error.message || "Unable to load showtimes.");
      })
      .finally(() => {
        if (active) setShowtimesLoading(false);
      });

    return () => {
      active = false;
    };
  }, [id, selectedTheatreId, selectedDate, retryToken]);

  if (movieLoading) {
    return (
      <main className="pt-28">
        <Loading label="Loading movie..." />
      </main>
    );
  }

  if (!movie) {
    return (
      <main className="px-5 pt-40 text-center text-white/50">
        {movieError || "Movie not found."}

        <div className="mt-5">
          <button
            type="button"
            onClick={retry}
            className="rounded-full border border-cyan-300/30 px-4 py-2 text-sm text-cyan-200 transition hover:bg-cyan-300/10"
          >
            Try again
          </button>
        </div>
      </main>
    );
  }

  const backdrop = movie.backdropUrl || movie.posterUrl;

  const genres =
    movie.genre ||
    movie.genres?.map((g) => g.name).join(" • ");

  return (
    <main className="min-h-screen bg-[#05070b]">

      {/* =====================================================
          HERO / MOVIE DETAILS
      ===================================================== */}

      <section
        className="
          relative
          overflow-hidden
          min-h-0
          pt-24
          pb-8
          sm:min-h-[620px]
          sm:pb-10
          lg:min-h-[700px]
        "
      >

        {backdrop && (
          <img
            src={backdrop}
            alt=""
            className="
              absolute
              inset-0
              h-full
              w-full
              object-cover
              opacity-30
              sm:opacity-40
            "
          />
        )}

        <div
          className="
            absolute
            inset-0
            bg-gradient-to-r
            from-[#05070b]
            via-[#05070b]/85
            to-[#05070b]/45
          "
        />

        <div
          className="
            absolute
            inset-0
            bg-gradient-to-t
            from-[#05070b]
            via-[#05070b]/35
            to-[#05070b]/70
          "
        />

        <div
          className="
            relative
            mx-auto
            max-w-[1440px]
            px-5
            sm:px-8
            lg:px-12
          "
        >

          <button
            onClick={() => navigate(-1)}
            className="
              mb-7
              flex
              items-center
              gap-2
              text-sm
              text-white/55
              transition
              hover:text-white
              sm:mb-10
              lg:mb-12
            "
          >
            <ArrowLeft size={18} />
            Back
          </button>

          <div
            className="
              mb-7
              flex
              justify-center
              md:hidden
            "
          >
            <div
              className="
                relative
                h-[260px]
                w-[174px]
                overflow-hidden
                rounded-2xl
                border
                border-white/10
                bg-white/[.04]
                shadow-[0_25px_60px_rgba(0,0,0,.5)]
              "
            >
              {movie.posterUrl ? (
                <img
                  src={movie.posterUrl}
                  alt={movie.title}
                  className="h-full w-full object-cover"
                />
              ) : (
                <div className="grid h-full place-items-center text-xs text-white/25">
                  No poster
                </div>
              )}

              <div className="absolute inset-0 bg-gradient-to-t from-black/40 via-transparent to-transparent" />
            </div>
          </div>

          <div
            className="
              grid
              w-full
              gap-8
              md:grid-cols-[210px_1fr]
              md:items-end
            "
          >

            <div
              className="
                hidden
                aspect-[2/3]
                overflow-hidden
                rounded-2xl
                border
                border-white/10
                shadow-2xl
                md:block
              "
            >
              {movie.posterUrl ? (
                <img
                  src={movie.posterUrl}
                  alt={movie.title}
                  className="h-full w-full object-cover"
                />
              ) : (
                <div className="grid h-full place-items-center text-white/20">
                  No poster
                </div>
              )}
            </div>

            <div className="animate-cine-rise">

              <div className="flex flex-wrap gap-2">

                <span
                  className="
                    flex
                    items-center
                    gap-1
                    rounded-full
                    bg-yellow-300/10
                    px-3
                    py-1.5
                    text-xs
                    text-yellow-200
                  "
                >
                  <Star size={13} className="fill-yellow-300" />
                  {Number(movie.rating || 0).toFixed(1)}
                </span>

                {genres && (
                  <span
                    className="
                      max-w-[220px]
                      truncate
                      rounded-full
                      bg-white/[.08]
                      px-3
                      py-1.5
                      text-xs
                      text-white/60
                    "
                  >
                    {genres}
                  </span>
                )}

              </div>

              <h1
                className="
                  mt-4
                  text-3xl
                  font-bold
                  leading-[1.05]
                  tracking-tight
                  sm:text-6xl
                  lg:text-7xl
                "
              >
                {movie.title}
              </h1>

              <p className="mt-3 text-sm italic text-cyan-200/70 sm:text-base">
                {movie.tagline || "Experience it on the big screen."}
              </p>

              <p
                className="
                  mt-4
                  max-w-3xl
                  text-sm
                  leading-6
                  text-white/55
                  sm:mt-5
                  sm:leading-7
                "
              >
                {movie.description || movie.overview}
              </p>

              <div
                className="
                  mt-5
                  flex
                  flex-wrap
                  gap-x-5
                  gap-y-3
                  text-xs
                  text-white/45
                "
              >
                <span className="flex items-center gap-2">
                  <CalendarDays size={15} />
                  {formatDate(movie.releaseDate || movie.release_date)}
                </span>

                <span className="flex items-center gap-2">
                  <Clock3 size={15} />
                  {movie.duration || movie.runtime || "—"} min
                </span>

                <span>
                  {movie.language || movie.original_language || "—"}
                </span>
              </div>

            </div>
          </div>
        </div>
      </section>

      {/* =====================================================
          FULL WIDTH BLUE SEPARATION LINE
      ===================================================== */}

      <div
        className="
          relative
          left-1/2
          h-[3px]
          w-screen
          -translate-x-1/2
          bg-gradient-to-r
          from-cyan-400
          via-cyan-300
          to-blue-500
          shadow-[0_0_10px_rgba(0,207,255,.75)]
        "
      />

      {/* =====================================================
          BOOKING FLOW: CITY -> THEATRE -> DATE -> SHOWTIME
      ===================================================== */}

      <section
        className="
          mx-auto
          max-w-[1200px]
          px-5
          pt-9
          pb-10
          sm:px-8
          sm:pt-12
          sm:pb-14
        "
      >

        <div className="mb-7 flex flex-wrap items-end justify-between gap-3 sm:mb-9">
          <div>
            <p
              className="
                text-[11px]
                font-semibold
                uppercase
                tracking-[.25em]
                text-cyan-300
                sm:text-xs
              "
            >
              {archived ? "Retired" : "Choose your experience"}
            </p>

            <h2 className="mt-2 text-2xl font-semibold">
              {archived ? "No longer bookable" : "Book tickets"}
            </h2>
          </div>

          <button
            type="button"
            onClick={retry}
            className="flex items-center gap-2 rounded-full border border-white/10 bg-white/[.03] px-3.5 py-2 text-xs text-white/55 transition hover:border-cyan-300/30 hover:text-white"
          >
            <RefreshCw size={13} />
            Refresh
          </button>
        </div>

        {archived ? (
          <ArchivedNotice />
        ) : (
        <div className="space-y-7">

          {/* CITY */}

          <div>
            <StepHeader
              number="1"
              title="Select city"
              hint="Cities with bookable shows for this movie"
            />

            {citiesLoading ? (
              <InlineLoading label="Finding cities..." />
            ) : citiesError ? (
              <ErrorState message={citiesError} onRetry={retry} />
            ) : cities.length === 0 ? (
              <EmptyState>
                No shows are scheduled for this movie yet. Please check back
                soon.
              </EmptyState>
            ) : (
              <div className="flex flex-wrap gap-2">
                {cities.map((city) => (
                  <SelectChip
                    key={city.id}
                    active={String(selectedCityId) === String(city.id)}
                    onClick={() => setSelectedCityId(city.id)}
                  >
                    {city.city}
                  </SelectChip>
                ))}
              </div>
            )}
          </div>

          {/* THEATRE */}

          {selectedCityId && (
            <div>
              <StepHeader
                number="2"
                title="Select theatre"
                hint="Theatres offering this movie in the selected city"
              />

              {theatresLoading ? (
                <InlineLoading label="Finding theatres..." />
              ) : theatresError ? (
                <ErrorState message={theatresError} onRetry={retry} />
              ) : theatres.length === 0 ? (
                <EmptyState>
                  No theatres in this city have bookable shows right now.
                </EmptyState>
              ) : (
                <div className="grid gap-3 sm:grid-cols-2">
                  {theatres.map((theatre) => {
                    const active =
                      String(selectedTheatreId) === String(theatre.id);

                    return (
                      <button
                        type="button"
                        key={theatre.id}
                        onClick={() => setSelectedTheatreId(theatre.id)}
                        aria-pressed={active}
                        className={`
                          rounded-2xl border p-4 text-left transition
                          ${
                            active
                              ? "border-cyan-300/40 bg-cyan-300/[.07]"
                              : "border-white/10 bg-white/[.03] hover:border-cyan-300/25 hover:bg-white/[.05]"
                          }
                        `}
                      >
                        <p className="truncate font-semibold text-white/90">
                          {theatre.name}
                        </p>

                        <p className="mt-1 flex items-center gap-2 truncate text-xs text-white/40">
                          <MapPin size={13} className="shrink-0" />
                          {theatre.city || "City"}
                          {theatre.totalSeats
                            ? ` · ${theatre.totalSeats} seats`
                            : ""}
                        </p>
                      </button>
                    );
                  })}
                </div>
              )}
            </div>
          )}

          {/* DATE */}

          {selectedTheatreId && (
            <div>
              <StepHeader
                number="3"
                title="Select date"
                hint="Dates with at least one bookable showtime"
              />

              {datesLoading ? (
                <InlineLoading label="Finding dates..." />
              ) : datesError ? (
                <ErrorState message={datesError} onRetry={retry} />
              ) : dates.length === 0 ? (
                <EmptyState>
                  No bookable dates remain for this theatre. Try another
                  theatre.
                </EmptyState>
              ) : (
                <div className="flex flex-wrap gap-2">
                  {dates.map((entry) => (
                    <SelectChip
                      key={entry.date}
                      active={selectedDate === entry.date}
                      onClick={() => setSelectedDate(entry.date)}
                    >
                      {formatDate(entry.date)}
                    </SelectChip>
                  ))}
                </div>
              )}
            </div>
          )}

          {/* SHOWTIME */}

          {selectedDate && (
            <div>
              <StepHeader
                number="4"
                title="Select showtime"
                hint="Expired and cancelled shows are never listed"
              />

              {showtimesLoading ? (
                <InlineLoading label="Finding showtimes..." />
              ) : showtimesError ? (
                <ErrorState message={showtimesError} onRetry={retry} />
              ) : showtimes.length === 0 ? (
                <EmptyState>
                  No bookable showtimes are left for this date. Pick another
                  date or theatre.
                </EmptyState>
              ) : (
                <div className="grid gap-3 sm:grid-cols-2 lg:grid-cols-3">
                  {showtimes.map((show) => {
                    const soldOut =
                      Number(show.availableSeats || 0) === 0;

                    return (
                      <button
                        type="button"
                        key={show.id}
                        disabled={soldOut}
                        onClick={() => navigate(`/show/${show.id}`)}
                        className={`
                          group rounded-2xl border p-4 text-left transition
                          ${
                            soldOut
                              ? "cursor-not-allowed border-white/5 bg-white/[.02] text-white/30"
                              : "border-white/10 bg-white/[.03] hover:-translate-y-0.5 hover:border-cyan-300/40 hover:bg-cyan-300/[.06]"
                          }
                        `}
                      >
                        <div className="flex items-center justify-between gap-3">
                          <p
                            className={`
                              flex items-center gap-2 text-base font-semibold
                              ${soldOut ? "text-white/30" : "text-cyan-200"}
                            `}
                          >
                            <Clock3 size={15} />
                            {formatTime(show.showTime)}
                          </p>

                          <span
                            className={`
                              rounded-full px-2 py-0.5 text-[10px] font-semibold uppercase tracking-wide
                              ${
                                soldOut
                                  ? "bg-white/10 text-white/40"
                                  : "bg-emerald-400/10 text-emerald-200"
                              }
                            `}
                          >
                            {soldOut
                              ? "Sold out"
                              : `${show.availableSeats} left`}
                          </span>
                        </div>

                        <p className="mt-3 flex items-center justify-between border-t border-white/10 pt-3 text-[11px] text-white/40">
                          <span>
                            {money(show.ticketPrice)} / ticket
                          </span>

                          {!soldOut && (
                            <span className="flex items-center gap-1 text-cyan-300">
                              Pick seats
                              <Play
                                size={12}
                                className="transition group-hover:translate-x-0.5"
                              />
                            </span>
                          )}
                        </p>
                      </button>
                    );
                  })}
                </div>
              )}
            </div>
          )}

        </div>
        )}
      </section>

    </main>
  );
}
