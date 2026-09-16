import React from "react";
import { Link } from "react-router-dom";
import {
  ArrowRight,
  ChevronRight,
  MapPin,
  Play,
  ShieldCheck,
  Ticket,
  Users,
  Zap,
} from "lucide-react";

import { getMovies, getShows } from "../lib/api";
import MovieCard from "../components/MovieCard";
import Loading from "../components/Loading";
import { useLocationContext } from "../lib/LocationContext";

export default function Home() {
  const { selectedLocation } = useLocationContext();

  const [movies, setMovies] = React.useState([]);
  const [shows, setShows] = React.useState([]);
  const [loading, setLoading] = React.useState(true);
  const [hero, setHero] = React.useState(null);

  React.useEffect(() => {
    Promise.allSettled([getMovies(), getShows()])
      .then(([m, s]) => {
        const movieList =
          m.status === "fulfilled" && Array.isArray(m.value)
            ? m.value
            : [];

        const showList =
          s.status === "fulfilled" && Array.isArray(s.value)
            ? s.value
            : [];

        setMovies(movieList);
        setShows(showList);
        setHero(movieList[0] || null);
      })
      .finally(() => setLoading(false));
  }, []);

  if (loading) {
    return (
      <div className="min-h-screen pt-28">
        <Loading label="Preparing your cinema..." />
      </div>
    );
  }

  const poster = hero?.backdropUrl || hero?.posterUrl;

  return (
    <main className="min-h-screen bg-[#05070b]">

      {/* =========================================================
          HERO
      ========================================================= */}

      <section
        className="
          relative
          overflow-hidden
          pt-[115px]
          sm:pt-[120px]
          lg:pt-[125px]
        "
      >

        {/* Background Image */}

        {poster && (
          <img
            src={poster}
            alt=""
            className="
              absolute
              inset-0
              h-full
              w-full
              scale-105
              object-cover
              opacity-55
            "
          />
        )}

        {/* Dark Overlay */}

        <div
          className="
            absolute
            inset-0
            bg-gradient-to-r
            from-[#05070b]
            via-[#05070b]/75
            to-[#05070b]/20
          "
        />

        <div
          className="
            absolute
            inset-0
            bg-gradient-to-t
            from-[#05070b]
            via-transparent
            to-[#05070b]/60
          "
        />

        <div className="cine-noise absolute inset-0" />


        {/* =====================================================
            HERO CONTENT
        ===================================================== */}

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

          <div
            className="
              w-full
              max-w-2xl
              animate-cine-rise
            "
          >

            {/* Badge */}

            <div
              className="
                mb-5
                inline-flex
                items-center
                rounded-full
                border
                border-cyan-300/20
                bg-cyan-300/10
                px-3.5
                py-2
                text-xs
                font-medium
                text-cyan-200
                backdrop-blur-md
              "
            >
              Your next story starts here
            </div>


            {/* Brand */}

            <p
              className="
                mb-3
                text-sm
                font-semibold
                uppercase
                tracking-[.3em]
                text-cyan-300
              "
            >
              SRH CinePass
            </p>


            {/* Movie Title */}

            <h1
              className="
                text-5xl
                font-bold
                leading-[.95]
                tracking-tight
                sm:text-6xl
                lg:text-8xl
              "
            >
              {hero?.title || "Cinema, reimagined."}
            </h1>


            {/* Description */}

            <p
              className="
                mt-6
                max-w-xl
                text-base
                leading-7
                text-white/60
                sm:text-lg
              "
            >
              {hero?.description ||
                "Discover movies, choose your theatre, pick your seats and make every night a movie night."}
            </p>


            {/* =================================================
                BUTTONS
            ================================================= */}

            <div
              className="
                mt-7
                flex
                w-full
                flex-row
                items-center
                gap-3
              "
            >

              {/* Book Tickets */}

              <Link
                to={hero ? `/movie/${hero.id}` : "/movies"}
                className="
                  group
                  flex
                  min-w-0
                  flex-1
                  items-center
                  justify-center
                  gap-1.5
                  rounded-full
                  bg-cyan-400
                  px-3
                  py-3
                  text-[12px]
                  font-semibold
                  text-slate-950
                  transition
                  duration-300
                  hover:bg-cyan-300
                  hover:shadow-[0_0_35px_rgba(18,207,232,.2)]
                  sm:flex-none
                  sm:gap-2
                  sm:px-6
                  sm:py-3.5
                  sm:text-sm
                "
              >
                <Ticket
                  size={16}
                  className="shrink-0 sm:h-[18px] sm:w-[18px]"
                />

                <span className="whitespace-nowrap">
                  Book tickets
                </span>

                <ArrowRight
                  size={16}
                  className="
                    shrink-0
                    transition
                    duration-300
                    group-hover:translate-x-1
                    sm:h-[17px]
                    sm:w-[17px]
                  "
                />
              </Link>


              {/* Explore Movies */}

              <Link
                to="/movies"
                className="
                  flex
                  min-w-0
                  flex-1
                  items-center
                  justify-center
                  gap-1.5
                  rounded-full
                  border
                  border-white/15
                  bg-white/[.05]
                  px-3
                  py-3
                  text-[12px]
                  text-white/85
                  backdrop-blur-md
                  transition
                  duration-300
                  hover:bg-white/[.1]
                  sm:flex-none
                  sm:gap-2
                  sm:px-6
                  sm:py-3.5
                  sm:text-sm
                "
              >
                <Play
                  size={15}
                  className="shrink-0 sm:h-4 sm:w-4"
                />

                <span className="whitespace-nowrap">
                  Explore movies
                </span>
              </Link>

            </div>


            {/* =================================================
                FEATURES
                SMALL GAP BEFORE BLUE LINE
            ================================================= */}

            <div
              className="
                mt-8
                flex
                flex-wrap
                gap-x-6
                gap-y-3
                pb-6
                text-xs
                text-white/45
                sm:pb-8
              "
            >

              <span className="flex items-center gap-2">
                <ShieldCheck
                  size={15}
                  className="text-cyan-300"
                />
                Secure booking
              </span>

              <span className="flex items-center gap-2">
                <Zap
                  size={15}
                  className="text-cyan-300"
                />
                Fast checkout
              </span>

              <span className="flex items-center gap-2">
                <Users
                  size={15}
                  className="text-cyan-300"
                />
                Live seats
              </span>

            </div>

          </div>

        </div>

      </section>


      {/* =========================================================
          BLUE SEPARATION LINE
      ========================================================= */}

      <div
        className="
          h-[3px]
          w-full
          bg-gradient-to-r
          from-cyan-400
          via-cyan-300
          to-blue-500
          shadow-[0_0_10px_rgba(0,207,255,.75)]
        "
      />


      {/* =========================================================
          NOW SHOWING
      ========================================================= */}

      <section
        className="
          mx-auto
          max-w-[1440px]
          px-5
          pt-10
          pb-16
          sm:px-8
          lg:px-12
        "
      >

        <div
          className="
            mb-8
            flex
            items-end
            justify-between
            gap-5
          "
        >

          <div>

            <p
              className="
                text-xs
                font-semibold
                uppercase
                tracking-[.25em]
                text-cyan-300
              "
            >
              Curated for you
            </p>

            <h2
              className="
                mt-2
                text-3xl
                font-semibold
                sm:text-4xl
              "
            >
              Now showing
            </h2>

            <p className="mt-2 text-sm text-white/40">
              Big stories. Better seats. One place.
            </p>

          </div>


          <Link
            to="/movies"
            className="
              hidden
              items-center
              gap-1
              text-sm
              text-cyan-300
              transition
              hover:text-cyan-200
              sm:flex
            "
          >
            See all
            <ChevronRight size={17} />
          </Link>

        </div>


        {/* Movie Grid */}

        {movies.length ? (
          <div
            className="
              grid
              grid-cols-2
              gap-4
              sm:grid-cols-3
              lg:grid-cols-5
            "
          >
            {movies.slice(0, 10).map((movie, index) => (
              <MovieCard
                key={movie.id ?? movie._id}
                movie={movie}
                index={index}
              />
            ))}
          </div>
        ) : (
          <Empty text="No movies are available right now." />
        )}

      </section>


      {/* =========================================================
          THEATRE PROMOTION
      ========================================================= */}

      <section
        className="
          mx-auto
          max-w-[1440px]
          px-5
          pb-20
          sm:px-8
          lg:px-12
        "
      >

        <div
          className="
            relative
            overflow-hidden
            rounded-3xl
            border
            border-white/10
            bg-gradient-to-br
            from-cyan-400/10
            via-white/[.03]
            to-blue-500/10
            p-7
            sm:p-10
          "
        >

          <div
            className="
              absolute
              -right-20
              -top-20
              h-64
              w-64
              rounded-full
              bg-cyan-300/10
              blur-3xl
            "
          />

          <div
            className="
              relative
              flex
              flex-col
              items-start
              justify-between
              gap-7
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
                  tracking-[.25em]
                  text-cyan-300
                "
              >
                Your city, your screen
              </p>

              <h2
                className="
                  mt-2
                  text-2xl
                  font-semibold
                  sm:text-3xl
                "
              >
                Find the best theatre near you.
              </h2>

              <p
                className="
                  mt-2
                  flex
                  items-center
                  gap-2
                  text-sm
                  text-white/45
                "
              >
                <MapPin
                  size={15}
                  className="text-cyan-300"
                />

                {selectedLocation?.city ||
                  "Choose a location to begin"}
              </p>

            </div>


            <Link
              to="/theaters"
              className="
                rounded-full
                border
                border-white/15
                bg-white/[.06]
                px-6
                py-3
                text-sm
                font-medium
                transition
                hover:bg-white/[.12]
              "
            >
              Explore theatres

              <ArrowRight
                className="ml-2 inline"
                size={16}
              />
            </Link>

          </div>

        </div>

      </section>

    </main>
  );
}


/* =========================================================
   EMPTY STATE
========================================================= */

function Empty({ text }) {
  return (
    <div
      className="
        rounded-2xl
        border
        border-white/10
        bg-white/[.03]
        p-12
        text-center
        text-white/40
      "
    >
      {text}
    </div>
  );
}