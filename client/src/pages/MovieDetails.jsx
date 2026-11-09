import React from "react";
import { Link, useNavigate, useParams } from "react-router-dom";
import {
  ArrowLeft,
  CalendarDays,
  Clock3,
  MapPin,
  Play,
  Star,
} from "lucide-react";

import { getMovie, getShows } from "../lib/api";
import Loading from "../components/Loading";
import { formatDate, formatTime, money } from "../lib/formatters";

export default function MovieDetails() {
  const { id } = useParams();
  const navigate = useNavigate();

  const [movie, setMovie] = React.useState(null);
  const [shows, setShows] = React.useState([]);
  const [loading, setLoading] = React.useState(true);

  React.useEffect(() => {
    Promise.all([
      getMovie(id),
      getShows(),
    ])
      .then(([m, s]) => {
        setMovie(m);

        setShows(
          (Array.isArray(s) ? s : []).filter(
            (x) =>
              String(x.movie?.id ?? x.movieId) === String(id)
          )
        );
      })
      .catch(console.error)
      .finally(() => {
        setLoading(false);
      });
  }, [id]);

  if (loading) {
    return (
      <main className="pt-28">
        <Loading label="Loading movie..." />
      </main>
    );
  }

  if (!movie) {
    return (
      <main className="px-5 pt-40 text-center text-white/50">
        Movie not found.
      </main>
    );
  }

  const backdrop =
    movie.backdropUrl || movie.posterUrl;

  const genres =
    movie.genre ||
    movie.genres
      ?.map((g) => g.name)
      .join(" • ");

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

        {/* ===================================================
            BACKDROP IMAGE
        =================================================== */}

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

        {/* ===================================================
            DARK OVERLAYS
        =================================================== */}

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


        {/* ===================================================
            CONTENT
        =================================================== */}

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

          {/* =================================================
              BACK BUTTON
          ================================================= */}

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


          {/* =================================================
              MOBILE POSTER
          ================================================= */}

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
                  className="
                    h-full
                    w-full
                    object-cover
                  "
                />
              ) : (
                <div
                  className="
                    grid
                    h-full
                    place-items-center
                    text-xs
                    text-white/25
                  "
                >
                  No poster
                </div>
              )}

              <div
                className="
                  absolute
                  inset-0
                  bg-gradient-to-t
                  from-black/40
                  via-transparent
                  to-transparent
                "
              />

            </div>
          </div>


          {/* =================================================
              DESKTOP CONTENT GRID
          ================================================= */}

          <div
            className="
              grid
              w-full
              gap-8
              md:grid-cols-[210px_1fr]
              md:items-end
            "
          >

            {/* =================================================
                DESKTOP POSTER
            ================================================= */}

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
                  className="
                    h-full
                    w-full
                    object-cover
                  "
                />
              ) : (
                <div
                  className="
                    grid
                    h-full
                    place-items-center
                    text-white/20
                  "
                >
                  No poster
                </div>
              )}

            </div>


            {/* =================================================
                MOVIE INFORMATION
            ================================================= */}

            <div className="animate-cine-rise">

              {/* RATING + GENRE */}

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
                  <Star
                    size={13}
                    className="fill-yellow-300"
                  />

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


              {/* TITLE */}

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


              {/* TAGLINE */}

              <p
                className="
                  mt-3
                  text-sm
                  italic
                  text-cyan-200/70
                  sm:text-base
                "
              >
                {movie.tagline ||
                  "Experience it on the big screen."}
              </p>


              {/* DESCRIPTION */}

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
                {movie.description ||
                  movie.overview}
              </p>


              {/* MOVIE INFORMATION */}

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

                <span
                  className="
                    flex
                    items-center
                    gap-2
                  "
                >
                  <CalendarDays size={15} />

                  {formatDate(
                    movie.releaseDate ||
                      movie.release_date
                  )}
                </span>


                <span
                  className="
                    flex
                    items-center
                    gap-2
                  "
                >
                  <Clock3 size={15} />

                  {movie.duration ||
                    movie.runtime ||
                    "—"}{" "}
                  min
                </span>


                <span>
                  {movie.language ||
                    movie.original_language ||
                    "—"}
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
          AVAILABLE SHOWS
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

        <div className="mb-6 sm:mb-7">

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
            Choose your experience
          </p>

          <h2
            className="
              mt-2
              text-2xl
              font-semibold
            "
          >
            Available shows
          </h2>

        </div>


        {/* ===================================================
            SHOW LIST
        =================================================== */}

        {shows.length ? (

          <div
            className="
              grid
              gap-3
              md:grid-cols-2
              md:gap-4
            "
          >

            {shows.map((show) => (

              <Link
                key={show.id}
                to={`/show/${show.id}`}
                className="
                  group
                  rounded-2xl
                  border
                  border-white/10
                  bg-white/[.03]
                  p-4
                  transition
                  hover:-translate-y-1
                  hover:border-cyan-300/30
                  hover:bg-white/[.05]
                  sm:p-5
                "
              >

                <div
                  className="
                    flex
                    items-center
                    justify-between
                    gap-4
                  "
                >

                  {/* THEATRE */}

                  <div className="min-w-0">

                    <p className="truncate font-semibold">
                      {show.theatre?.name ||
                        "Theatre"}
                    </p>

                    <p
                      className="
                        mt-1
                        flex
                        items-center
                        gap-2
                        truncate
                        text-xs
                        text-white/40
                      "
                    >

                      <MapPin
                        size={13}
                        className="shrink-0"
                      />

                      {show.theatre?.location
                        ?.city ||
                        "Your city"}

                      {" · "}

                      {formatDate(
                        show.showDate
                      )}

                    </p>

                  </div>


                  {/* SHOW TIME */}

                  <div
                    className="
                      shrink-0
                      text-right
                    "
                  >

                    <p
                      className="
                        flex
                        items-center
                        justify-end
                        gap-2
                        text-base
                        font-semibold
                        text-cyan-200
                        sm:text-lg
                      "
                    >

                      <Clock3 size={16} />

                      {formatTime(
                        show.showTime
                      )}

                    </p>

                    <p
                      className="
                        mt-1
                        text-[11px]
                        text-white/35
                        sm:text-xs
                      "
                    >
                      {money(
                        show.ticketPrice
                      )}{" "}
                      / ticket
                    </p>

                  </div>

                </div>


                {/* PICK SEATS */}

                <div
                  className="
                    mt-4
                    flex
                    items-center
                    justify-between
                    border-t
                    border-white/10
                    pt-3
                    text-xs
                    text-cyan-300
                  "
                >

                  <span>
                    Pick seats
                  </span>

                  <Play
                    size={14}
                    className="
                      transition
                      group-hover:translate-x-1
                    "
                  />

                </div>

              </Link>

            ))}

          </div>

        ) : (

          <div
            className="
              rounded-2xl
              border
              border-white/10
              bg-white/[.03]
              p-10
              text-center
              text-sm
              text-white/40
            "
          >
            No shows available for this movie yet.
          </div>

        )}

      </section>

    </main>
  );
}