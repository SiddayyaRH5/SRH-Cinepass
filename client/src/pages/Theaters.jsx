import React from "react";
import {
  Building2,
  ChevronRight,
  MapPin,
  Armchair,
} from "lucide-react";
import { useNavigate } from "react-router-dom";
import { getTheatresByLocation } from "../lib/api";
import { useLocationContext } from "../lib/LocationContext";
import Loading from "../components/Loading";

export default function Theaters() {
  const { selectedLocation } = useLocationContext();

  const [data, setData] = React.useState([]);
  const [loading, setLoading] = React.useState(false);
  const [error, setError] = React.useState("");

  const navigate = useNavigate();

  /* ==========================================================
     LOAD THEATRES
  ========================================================== */

  React.useEffect(() => {
    if (!selectedLocation) {
      setData([]);
      return;
    }

    setLoading(true);
    setError("");

    getTheatresByLocation(selectedLocation.id)
      .then((d) => {
        setData(Array.isArray(d) ? d : []);
      })
      .catch((e) => {
        setError(
          e.message || "Unable to load theatres"
        );
      })
      .finally(() => {
        setLoading(false);
      });
  }, [selectedLocation]);

  return (
    <main
      className="
        min-h-screen
        px-4
        pb-16
        pt-28

        sm:px-8
        sm:pb-20
        sm:pt-32

        lg:px-12
      "
    >
      <div className="mx-auto max-w-[1200px]">

        {/* ====================================================
            PAGE HEADER
        ==================================================== */}

        <div
          className="
            mb-7
            animate-cine-rise

            sm:mb-10
          "
        >
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
            Cinema near you
          </p>

          <h1
            className="
              mt-1.5
              text-3xl
              font-semibold
              leading-tight

              sm:mt-2
              sm:text-5xl
            "
          >
            Theatres
          </h1>

          <p
            className="
              mt-2
              flex
              items-center
              gap-2
              text-sm
              text-white/40

              sm:mt-3
            "
          >
            <MapPin
              size={15}
              className="shrink-0 text-cyan-300"
            />

            {selectedLocation
              ? `Showing theatres in ${selectedLocation.city}`
              : "Choose a location to discover theatres"}
          </p>
        </div>

        {/* ====================================================
            NO LOCATION
        ==================================================== */}

        {!selectedLocation ? (
          <div
            className="
              rounded-2xl
              border
              border-white/10
              bg-white/[.03]
              p-10
              text-center

              sm:rounded-3xl
              sm:p-14
            "
          >
            <MapPin
              size={34}
              className="mx-auto text-cyan-300"
            />

            <h2
              className="
                mt-5
                text-xl
                font-semibold
              "
            >
              Pick your city
            </h2>

            <p
              className="
                mx-auto
                mt-2
                max-w-md
                text-sm
                leading-6
                text-white/40
              "
            >
              Use the location button in the
              navigation bar to find nearby
              theatres.
            </p>
          </div>

        ) : loading ? (

          /* ==================================================
             LOADING
          ================================================== */

          <Loading label="Finding theatres..." />

        ) : error ? (

          /* ==================================================
             ERROR
          ================================================== */

          <div
            className="
              rounded-2xl
              border
              border-red-400/20
              bg-red-400/5
              p-10
              text-center
              text-sm
              text-red-300
            "
          >
            {error}
          </div>

        ) : data.length ? (

          /* ==================================================
             THEATRE GRID
          ================================================== */

          <div
            className="
              grid
              gap-4

              md:grid-cols-2
              md:gap-5
            "
          >
            {data.map((theatre, index) => (

              <button
                key={theatre.id}
                onClick={() =>
                  navigate(
                    `/theatre/${theatre.id}`
                  )
                }
                className="
                  group
                  cine-shine
                  w-full
                  rounded-2xl
                  border
                  border-white/10
                  bg-white/[.03]
                  p-5
                  text-left
                  transition
                  duration-500

                  hover:-translate-y-1
                  hover:border-cyan-300/30
                  hover:bg-white/[.05]

                  sm:p-6

                  animate-cine-rise
                "
                style={{
                  animationDelay:
                    `${index * 70}ms`,
                }}
              >

                {/* =================================================
                    THEATRE HEADER
                ================================================= */}

                <div
                  className="
                    flex
                    items-center
                    justify-between
                    gap-3
                  "
                >

                  {/* LEFT */}

                  <div
                    className="
                      flex
                      min-w-0
                      items-center
                      gap-4
                    "
                  >

                    {/* BUILDING ICON */}

                    <span
                      className="
                        grid
                        h-12
                        w-12
                        shrink-0
                        place-items-center
                        rounded-xl
                        bg-cyan-300/10
                        text-cyan-300

                        sm:h-13
                        sm:w-13
                      "
                    >
                      <Building2
                        size={25}
                      />
                    </span>

                    {/* NAME + LOCATION */}

                    <div className="min-w-0">

                      <h2
                        className="
                          truncate
                          text-lg
                          font-semibold

                          sm:text-xl
                        "
                      >
                        {theatre.name}
                      </h2>

                      <p
                        className="
                          mt-1
                          flex
                          items-center
                          gap-1
                          truncate
                          text-xs
                          text-white/35
                        "
                      >
                        <MapPin
                          size={12}
                          className="shrink-0"
                        />

                        {theatre.location?.city ||
                          selectedLocation.city}
                      </p>

                    </div>
                  </div>

                  {/* ARROW */}

                  <ChevronRight
                    size={22}
                    className="
                      shrink-0
                      text-white/20
                      transition

                      group-hover:translate-x-1
                      group-hover:text-cyan-300
                    "
                  />
                </div>

                {/* =================================================
                    DIVIDER
                ================================================= */}

                <div
                  className="
                    mt-5
                    border-t
                    border-white/10
                    pt-4

                    sm:mt-6
                  "
                >

                  {/* =================================================
                      BOTTOM INFO
                  ================================================= */}

                  <div
                    className="
                      flex
                      items-center
                      justify-between
                      gap-3
                      text-xs
                    "
                  >

                    {/* SEATS */}

                    <span
                      className="
                        flex
                        items-center
                        gap-2
                        text-white/40
                      "
                    >
                      <Armchair
                        size={14}
                        className="text-cyan-300"
                      />

                      {theatre.totalSeats || 0}
                      {" "}
                      seats
                    </span>

                    {/* VIEW SHOWS */}

                    <span
                      className="
                        flex
                        items-center
                        gap-1
                        text-cyan-300
                      "
                    >
                      View shows

                      <ChevronRight
                        size={14}
                        className="
                          transition
                          group-hover:translate-x-1
                        "
                      />
                    </span>

                  </div>
                </div>

              </button>
            ))}
          </div>

        ) : (

          /* ==================================================
             EMPTY STATE
          ================================================== */

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

              sm:rounded-3xl
              sm:p-14
            "
          >
            No theatres are available in{" "}
            {selectedLocation.city} yet.
          </div>
        )}
      </div>
    </main>
  );
}