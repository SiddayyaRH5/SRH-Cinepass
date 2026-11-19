import React from "react";
import { useQuery } from "@tanstack/react-query";
import { Link, useNavigate, useParams } from "react-router-dom";
import {
  AlertCircle,
  ArrowLeft,
  CalendarDays,
  Clock3,
  Globe,
  Loader2,
  MapPin,
  Play,
  RefreshCw,
  Star,
} from "lucide-react";

import {
  getMovie,
  getMovies,
  getMovieCities,
  getMovieShowDates,
  getMovieShowTimes,
  getMovieTheatres,
} from "../lib/api";
import Loading from "../components/Loading";
import { useLocationContext } from "../lib/LocationContext";
import { formatDate, formatDuration, formatFullDate, formatLanguage, formatTime, money } from "../lib/formatters";

const MOVIE_DETAILS_QUERY_KEY = ["movies", "detail"];

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

  const { data: movie, isLoading: movieLoading, isError: movieError, refetch: refetchMovie } = useQuery({
    queryKey: ["movies", "detail", id],
    queryFn: async () => {
      const data = await getMovie(id);
      return data;
    },
    staleTime: 1000 * 60 * 5,
    enabled: !!id,
  });

  const [archived, setArchived] = React.useState(false);
  const [showAllCast, setShowAllCast] = React.useState(false);
  const [playingTrailer, setPlayingTrailer] = React.useState(false);
  const [recommendations, setRecommendations] = React.useState([]);
  const [recsLoading, setRecsLoading] = React.useState(false);

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
    refetchMovie();
  }, [refetchMovie]);

  const movieLoaded = movieLoading === false;

  React.useEffect(() => {
    if (!movie) return;
    setArchived(movie?.status === "ARCHIVED");
  }, [movie]);

    React.useEffect(() => {
      let active = true;
      setRecsLoading(true);

    getMovies()
      .then((data) => {
        if (!active) return;

        const list = Array.isArray(data) ? data : [];
        const currentId = movie.id;
        const currentGenres = new Set(
          (movie.genre || "")
            .split(",")
            .map((g) => g.trim().toLowerCase())
            .filter(Boolean)
        );
        const currentLang = (movie.language || movie.original_language || "")
          .toLowerCase();

        const scored = list
          .filter((m) => m.id !== currentId)
          .map((m) => {
            const genres = (m.genre || "")
              .split(",")
              .map((g) => g.trim().toLowerCase())
              .filter(Boolean);
            const genreOverlap = genres.filter((g) =>
              currentGenres.has(g)
            ).length;
            const langMatch =
              (m.language || m.original_language || "").toLowerCase() ===
              currentLang
                ? 2
                : 0;
            const score = genreOverlap + langMatch;
            return { movie: m, score };
          })
          .filter((s) => s.score > 0)
          .sort((a, b) => {
            if (b.score !== a.score) return b.score - a.score;
            return (
              Number(b.movie.rating || b.movie.vote_average || 0) -
              Number(a.movie.rating || a.movie.vote_average || 0)
            );
          })
          .slice(0, 12)
          .map((s) => s.movie);

        setRecommendations(scored);
      })
      .catch(() => {
        if (active) setRecommendations([]);
      })
      .finally(() => {
        if (active) setRecsLoading(false);
      });

    return () => {
      active = false;
    };
  }, [movieLoaded, movie]);

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
  }, [id, selectedLocation, refetchMovie, archived, movieLoaded]);

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
  }, [id, selectedCityId, refetchMovie]);
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
  }, [id, selectedTheatreId, refetchMovie]);

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
  }, [id, selectedTheatreId, selectedDate, refetchMovie]);

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
          pt-20
          pb-6
          sm:pt-24
          sm:pb-8
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
              mb-4
              flex
              items-center
              gap-2
              text-sm
              text-white/55
              transition
              hover:text-white
              sm:mb-6
              lg:mb-8
            "
          >
            <ArrowLeft size={18} />
            Back
          </button>

          <div
            className="
              mb-4
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
              gap-6
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
                  mt-3
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

              <p className="mt-2 text-sm italic text-cyan-200/70 sm:text-base">
                {movie.tagline || "Experience it on the big screen."}
              </p>

              <p
                className="
                  mt-3
                  max-w-3xl
                  text-sm
                  leading-6
                  text-white/55
                  sm:mt-4
                  sm:leading-7
                "
              >
                {movie.description || movie.overview}
              </p>

              <div
                className="
                  mt-3
                  flex
                  flex-wrap
                  items-center
                  gap-x-4
                  gap-y-2
                  text-sm
                  text-white/55
                "
              >
                <span className="flex items-center gap-2">
                  <CalendarDays
                    size={16}
                    className="shrink-0 text-cyan-300/70"
                  />
                  <span className="text-white/70">
                    {formatFullDate(movie.releaseDate || movie.release_date)}
                  </span>
                </span>

                <span className="hidden h-4 w-px bg-white/10 sm:block" />

                <span className="flex items-center gap-2">
                  <Clock3
                    size={16}
                    className="shrink-0 text-cyan-300/70"
                  />
                  <span className="text-white/70">
                    {formatDuration(movie.duration || movie.runtime)}
                  </span>
                </span>

                {(movie.language || movie.original_language) && (
                  <>
                    <span className="hidden h-4 w-px bg-white/10 sm:block" />

                    <span className="flex items-center gap-2">
                      <Globe
                        size={16}
                        className="shrink-0 text-cyan-300/70"
                      />
                      <span className="text-white/70">
                        {formatLanguage(movie.language || movie.original_language)}
                      </span>
                    </span>
                  </>
                )}
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
          TRAILER
      ===================================================== */}

      {movie.trailerKey && (
        <section className="mx-auto max-w-[1000px] px-5 pt-2 pb-4 sm:px-8">
          <div className="mb-3">
            <p className="text-[11px] font-semibold uppercase tracking-[.25em] text-cyan-300 sm:text-xs">
              Trailer
            </p>
            <h2 className="mt-1 text-2xl font-semibold">Watch trailer</h2>
          </div>

          <div className="relative aspect-video w-full overflow-hidden rounded-2xl border border-white/10 bg-black shadow-2xl">
            {playingTrailer ? (
              <iframe
                src={`https://www.youtube-nocookie.com/embed/${movie.trailerKey}?autoplay=1&rel=0`}
                title={`${movie.title} trailer`}
                allow="accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture"
                allowFullScreen
                className="absolute inset-0 h-full w-full"
              />
            ) : (
              <button
                type="button"
                onClick={() => setPlayingTrailer(true)}
                className="group absolute inset-0 flex items-center justify-center"
                aria-label={`Play ${movie.title} trailer`}
              >
                <img
                  src={`https://img.youtube.com/vi/${movie.trailerKey}/maxresdefault.jpg`}
                  alt={`${movie.title} trailer thumbnail`}
                  className="absolute inset-0 h-full w-full object-cover opacity-70 transition group-hover:opacity-90"
                  onError={(e) => {
                    e.currentTarget.src = `https://img.youtube.com/vi/${movie.trailerKey}/hqdefault.jpg`;
                  }}
                />
                <div className="absolute inset-0 bg-gradient-to-t from-black/60 via-transparent to-transparent" />
                <span className="relative grid h-16 w-16 place-items-center rounded-full bg-cyan-400/90 text-slate-950 shadow-[0_0_30px_rgba(34,211,238,.5)] transition group-hover:scale-110 sm:h-20 sm:w-20">
                  <Play size={28} className="ml-1 fill-slate-950" />
                </span>
              </button>
            )}
          </div>

          <p className="mt-3 text-xs text-white/35">
            Trailer provided by TMDB.{" "}
            <a
              href={`https://www.youtube.com/watch?v=${movie.trailerKey}`}
              target="_blank"
              rel="noopener noreferrer"
              className="text-cyan-300/70 underline hover:text-cyan-200"
            >
              Open on YouTube
            </a>
          </p>
        </section>
      )}

      {/* =====================================================
          CAST
      ===================================================== */}

      {(() => {
        let cast = [];
        try {
          cast = movie.castJson ? JSON.parse(movie.castJson) : [];
        } catch {
          cast = [];
        }
        if (!Array.isArray(cast) || cast.length === 0) return null;
        const visible = showAllCast ? cast : cast.slice(0, 8);
        return (
          <section className="mx-auto max-w-[1200px] px-5 pt-2 pb-6 sm:px-8">
            <div className="mb-3 flex items-end justify-between gap-3">
              <div>
                <p className="text-[11px] font-semibold uppercase tracking-[.25em] text-cyan-300 sm:text-xs">
                  Cast
                </p>
                <h2 className="mt-1 text-xl font-semibold sm:text-2xl">
                  Top billed cast
                </h2>
              </div>
              {cast.length > 8 && (
                <button
                  type="button"
                  onClick={() => setShowAllCast((v) => !v)}
                  className="text-xs text-cyan-300/70 transition hover:text-cyan-200"
                >
                  {showAllCast ? "Show less" : `Show all (${cast.length})`}
                </button>
              )}
            </div>

            <div className="grid grid-cols-2 gap-2 sm:grid-cols-3 md:grid-cols-4 lg:grid-cols-6 xl:grid-cols-8">
              {visible.map((member, index) => {
                const photo = member.profilePath
                  ? `https://image.tmdb.org/t/p/w185${member.profilePath}`
                  : null;
                return (
                  <div
                    key={`${member.name}-${index}`}
                    className="overflow-hidden rounded-xl border border-white/10 bg-white/[.03]"
                  >
                    <div className="flex h-[180px] w-full items-center justify-center overflow-hidden bg-black/20 sm:h-[200px]">
                      {photo ? (
                        <img
                          src={photo}
                          alt={member.name}
                          loading="lazy"
                          className="h-full w-full object-contain"
                          onError={(e) => {
                            e.currentTarget.style.display = "none";
                          }}
                        />
                      ) : (
                        <div className="grid h-full place-items-center text-[10px] text-white/25">
                          No photo
                        </div>
                      )}
                    </div>
                    <div className="p-2">
                      <p className="truncate text-xs font-semibold text-white/85">
                        {member.name}
                      </p>
                      <p className="truncate text-[10px] text-white/40">
                        {member.character || "—"}
                      </p>
                    </div>
                  </div>
                );
              })}
            </div>
          </section>
        );
      })()}

      {/* =====================================================
          RECOMMENDATIONS
      ===================================================== */}

      <section className="mx-auto max-w-[1440px] px-5 pt-4 pb-10 sm:px-8">
        <div className="mb-5 flex items-end justify-between gap-3">
          <div>
            <p className="text-[11px] font-semibold uppercase tracking-[.25em] text-cyan-300 sm:text-xs">
              Discover
            </p>
            <h2 className="mt-1 text-2xl font-semibold">
              You May Also Like
            </h2>
            <p className="mt-1 text-xs text-white/35">
              More movies worth watching.
            </p>
          </div>
        </div>

        {recsLoading ? (
          <div className="flex gap-4 overflow-hidden">
            {Array.from({ length: 6 }).map((_, i) => (
              <div
                key={i}
                className="w-36 shrink-0 animate-pulse sm:w-40"
              >
                <div className="aspect-[2/3] rounded-xl bg-white/[.06]" />
                <div className="mt-2 h-3 rounded bg-white/[.06]" />
                <div className="mt-1 h-2 w-2/3 rounded bg-white/[.04]" />
              </div>
            ))}
          </div>
        ) : recommendations.length === 0 ? (
          <div className="rounded-2xl border border-white/10 bg-white/[.03] px-5 py-8 text-center text-sm text-white/40">
            No recommendations available right now.
          </div>
        ) : (
          <div className="relative">
            <div
              className="flex gap-4 overflow-x-auto pb-4 scroll-smooth snap-x snap-mandatory"
              style={{ scrollbarWidth: "thin" }}
            >
              {recommendations.map((m) => {
                const poster = m.posterUrl || m.poster_path;
                const rating = Number(m.rating ?? m.vote_average ?? 0);
                const year = (m.releaseDate || m.release_date || "")
                  .slice(0, 4);
                const genre =
                  m.genre ||
                  (Array.isArray(m.genres)
                    ? m.genres.map((g) => g.name).join(", ")
                    : "");
                const language = m.language || m.original_language || "";

                return (
                  <Link
                    key={m.id ?? m._id}
                    to={`/movie/${m.id ?? m._id}`}
                    className="group w-36 shrink-0 snap-start sm:w-40"
                  >
                    <article className="overflow-hidden rounded-xl border border-white/[.08] bg-white/[.03] transition-all duration-300 group-hover:-translate-y-1 group-hover:border-cyan-300/30 group-hover:shadow-[0_12px_30px_rgba(0,0,0,.35)]">
                      <div className="relative aspect-[2/3] overflow-hidden bg-white/[.04]">
                        {poster ? (
                          <img
                            src={poster}
                            alt={m.title}
                            loading="lazy"
                            className="h-full w-full object-cover transition duration-500 group-hover:scale-105"
                          />
                        ) : (
                          <div className="grid h-full place-items-center text-[10px] text-white/20">
                            No poster
                          </div>
                        )}
                        <div className="absolute inset-0 bg-gradient-to-t from-black/80 via-transparent to-transparent" />
                        {rating > 0 && (
                          <div className="absolute left-2 top-2 flex items-center gap-1 rounded-full bg-black/55 px-2 py-0.5 text-[10px] font-semibold backdrop-blur-md">
                            <Star size={10} className="fill-yellow-300 text-yellow-300" />
                            <span className="text-yellow-200">
                              {rating.toFixed(1)}
                            </span>
                          </div>
                        )}
                      </div>
                      <div className="p-2.5">
                        <h3 className="truncate text-xs font-semibold text-white/90 sm:text-sm">
                          {m.title}
                        </h3>
                        <p className="mt-0.5 truncate text-[10px] text-white/40">
                          {genre || language || "Cinema"}
                        </p>
                        <p className="mt-1.5 flex items-center justify-between text-[10px] text-white/30">
                          <span>{year || "—"}</span>
                          {language && (
                            <span className="truncate text-cyan-300/60">
                              {language}
                            </span>
                          )}
                        </p>
                      </div>
                    </article>
                  </Link>
                );
              })}
            </div>
          </div>
        )}
      </section>

      {/* =====================================================
          BOOKING FLOW: CITY -> THEATRE -> DATE -> SHOWTIME
      ===================================================== */}

      <section
        className="
          mx-auto
          max-w-[1100px]
          px-5
          pt-6
          pb-6
          sm:px-8
          sm:pt-8
          sm:pb-8
        "
      >

        <div className="mb-4 flex flex-wrap items-end justify-between gap-3 sm:mb-6">
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
        <div className="space-y-4">

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
                          rounded-2xl border p-3 text-left transition
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
