import React from "react";
import { Heart } from "lucide-react";

import { getMovies } from "../lib/api";
import MovieCard from "../components/MovieCard";
import Loading from "../components/Loading";

export default function Favorite() {
  const [movies, setMovies] = React.useState([]);
  const [loading, setLoading] = React.useState(true);

  React.useEffect(() => {
    getMovies()
      .then((data) => {
        setMovies(
          Array.isArray(data)
            ? data.slice(0, 6)
            : []
        );
      })
      .catch(console.error)
      .finally(() => {
        setLoading(false);
      });
  }, []);

  return (
    <main
      className="
        min-h-screen
        bg-[#05070b]
        px-4
        pb-16
        pt-28
        sm:px-8
        sm:pt-32
        sm:pb-20
        lg:px-12
      "
    >
      <div className="mx-auto max-w-[1440px]">

        {/* ==================================================
            PAGE HEADER
        ================================================== */}

        <div
          className="
            mb-7
            animate-cine-rise
            sm:mb-10
          "
        >

          {/* COLLECTION LABEL */}

          <p
            className="
              flex
              items-center
              gap-2
              text-[11px]
              font-semibold
              uppercase
              tracking-[.25em]
              text-cyan-300
              sm:text-xs
            "
          >
            <Heart
              size={14}
              className="shrink-0"
            />

            Your collection
          </p>


          {/* TITLE */}

          <h1
            className="
              mt-1.5
              text-3xl
              font-semibold
              leading-tight
              sm:mt-2
              sm:text-4xl
            "
          >
            Favorites
          </h1>


          {/* DESCRIPTION */}

          <p
            className="
              mt-2
              max-w-xl
              text-sm
              leading-6
              text-white/40
            "
          >
            A cinematic shelf for the movies
            you don't want to forget.
          </p>

        </div>


        {/* ==================================================
            CONTENT
        ================================================== */}

        {loading ? (

          <div className="mt-8">
            <Loading
              label="Loading your collection..."
            />
          </div>

        ) : movies.length ? (

          <>
            {/* =================================================
                MOVIE GRID
            ================================================= */}

            <div
              className="
                grid
                grid-cols-2
                gap-x-3
                gap-y-6
                sm:grid-cols-3
                sm:gap-4
                lg:grid-cols-5
                xl:grid-cols-6
              "
            >
              {movies.map((movie, index) => (
                <MovieCard
                  key={
                    movie.id ??
                    movie._id
                  }
                  movie={movie}
                  index={index}
                />
              ))}
            </div>


            {/* =================================================
                FULL WIDTH BLUE SEPARATION LINE
            ================================================= */}

            <div
              className="
                relative
                left-1/2
                mt-12
                h-[3px]
                w-screen
                -translate-x-1/2
                bg-gradient-to-r
                from-cyan-400
                via-cyan-300
                to-blue-500
                shadow-[0_0_10px_rgba(0,207,255,.75)]
                sm:mt-16
              "
            />

          </>

        ) : (

          /* =================================================
             EMPTY STATE
          ================================================= */

          <div
            className="
              rounded-2xl
              border
              border-white/10
              bg-white/[.03]
              px-6
              py-12
              text-center
              sm:rounded-3xl
              sm:p-14
            "
          >

            <Heart
              size={32}
              className="
                mx-auto
                text-cyan-300/50
              "
            />

            <h2
              className="
                mt-4
                text-lg
                font-semibold
              "
            >
              Your collection is empty
            </h2>

            <p
              className="
                mt-2
                text-sm
                text-white/40
              "
            >
              Add your favourite movies
              to see them here.
            </p>

          </div>

        )}

      </div>
    </main>
  );
}