import React from "react";
import { Link } from "react-router-dom";
import { CalendarDays, Clock3, Star } from "lucide-react";
import { formatDate } from "../lib/formatters";

export default function MovieCard({ movie, index = 0 }) {
  const poster = movie.posterUrl || movie.poster_path;
  const rating = Number(
    movie.rating ?? movie.vote_average ?? 0
  );

  const title = movie.title || "Untitled";
  const id = movie.id ?? movie._id;

  const genre =
    movie.genre ||
    movie.genres
      ?.slice?.(0, 2)
      ?.map((g) => g.name)
      .join(" • ");

  return (
    <Link
      to={`/movie/${id}`}
      className="
        group
        block
        min-w-0
        animate-cine-rise
      "
      style={{
        animationDelay: `${index * 60}ms`,
      }}
    >
      <article
        className="
          cine-shine
          relative
          overflow-hidden
          rounded-xl
          border
          border-white/[.08]
          bg-white/[.03]
          transition
          duration-500

          group-hover:-translate-y-1.5
          group-hover:border-cyan-300/30
          group-hover:shadow-[0_20px_45px_rgba(0,0,0,.35)]

          sm:rounded-2xl
          sm:group-hover:-translate-y-2
        "
      >

        {/* ====================================================
            POSTER
        ==================================================== */}

        <div
          className="
            relative
            aspect-[2/3]
            overflow-hidden
            bg-white/[.04]
          "
        >
          {poster ? (
            <img
              src={poster}
              alt={title}
              loading="lazy"
              className="
                h-full
                w-full
                object-cover
                transition
                duration-700
                group-hover:scale-105
                group-hover:brightness-110
              "
            />
          ) : (
            <div
              className="
                grid
                h-full
                place-items-center
                text-xs
                text-white/20
              "
            >
              No poster
            </div>
          )}

          {/* POSTER GRADIENT */}

          <div
            className="
              absolute
              inset-0
              bg-gradient-to-t
              from-black/90
              via-black/10
              to-transparent
            "
          />

          {/* ==================================================
              RATING
          ================================================== */}

          <div
            className="
              absolute
              left-2
              top-2
              flex
              items-center
              gap-1
              rounded-full
              border
              border-white/10
              bg-black/55
              px-2
              py-1
              text-[10px]
              font-semibold
              backdrop-blur-md

              sm:left-3
              sm:top-3
              sm:px-2.5
              sm:text-xs
            "
          >
            <Star
              size={11}
              className="
                fill-yellow-300
                text-yellow-300

                sm:h-3
                sm:w-3
              "
            />

            {rating
              ? rating.toFixed(1)
              : "New"}
          </div>

          {/* ==================================================
              VIEW MOVIE
          ================================================== */}

          <div
            className="
              absolute
              inset-x-2
              bottom-2
              translate-y-2
              opacity-0
              transition
              duration-300

              group-hover:translate-y-0
              group-hover:opacity-100

              sm:inset-x-3
              sm:bottom-3
            "
          >
            <span
              className="
                block
                rounded-lg
                bg-cyan-400
                py-2
                text-center
                text-xs
                font-semibold
                text-slate-950

                sm:rounded-xl
                sm:py-2.5
                sm:text-sm
              "
            >
              View movie
            </span>
          </div>
        </div>

        {/* ====================================================
            MOVIE INFORMATION
        ==================================================== */}

        <div
          className="
            p-2.5

            sm:p-4
          "
        >

          {/* TITLE */}

          <h3
            className="
              truncate
              text-sm
              font-semibold
              text-white

              sm:text-base
            "
          >
            {title}
          </h3>

          {/* GENRE */}

          <p
            className="
              mt-0.5
              truncate
              text-[10px]
              text-white/40

              sm:mt-1
              sm:text-xs
            "
          >
            {genre ||
              movie.language ||
              movie.original_language ||
              "Cinema"}
          </p>

          {/* ==================================================
              DATE + DURATION
          ================================================== */}

          <div
            className="
              mt-2
              flex
              items-center
              justify-between
              gap-1
              text-[9px]
              text-white/35

              sm:mt-3
              sm:text-[11px]
            "
          >
            {movie.releaseDate ||
            movie.release_date ? (
              <span
                className="
                  flex
                  min-w-0
                  items-center
                  gap-1
                "
              >
                <CalendarDays
                  size={10}
                  className="shrink-0 sm:h-3 sm:w-3"
                />

                <span className="truncate">
                  {formatDate(
                    movie.releaseDate ||
                      movie.release_date
                  )}
                </span>
              </span>
            ) : (
              <span />
            )}

            {movie.duration ||
            movie.runtime ? (
              <span
                className="
                  flex
                  shrink-0
                  items-center
                  gap-1
                "
              >
                <Clock3
                  size={10}
                  className="sm:h-3 sm:w-3"
                />

                {movie.duration ||
                  movie.runtime}{" "}
                min
              </span>
            ) : null}
          </div>
        </div>
      </article>
    </Link>
  );
}