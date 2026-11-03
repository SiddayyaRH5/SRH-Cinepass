import React from "react";
import {
  Check,
  ChevronDown,
  Loader2,
  MapPin,
  Search,
  X,
} from "lucide-react";
import { useLocationContext } from "../lib/LocationContext";
import { getLocations } from "../lib/api";

export default function LocationSelector() {
  const { selectedLocation, selectLocation } =
    useLocationContext();

  const [locations, setLocations] = React.useState([]);
  const [open, setOpen] = React.useState(false);
  const [loading, setLoading] = React.useState(true);
  const [query, setQuery] = React.useState("");

  React.useEffect(() => {
    let active = true;

    getLocations()
      .then((data) => {
        if (active) {
          setLocations(
            Array.isArray(data) ? data : []
          );
        }
      })
      .catch((err) => {
        console.error(err);
      })
      .finally(() => {
        if (active) {
          setLoading(false);
        }
      });

    return () => {
      active = false;
    };
  }, []);

  const filtered = locations.filter((x) =>
    `${x.city} ${x.state}`
      .toLowerCase()
      .includes(query.toLowerCase())
  );

  const handleSelect = (location) => {
    selectLocation(location);
    setOpen(false);
    setQuery("");
  };

  return (
    <div className="relative">

      {/* =====================================================
          LOCATION BUTTON
      ===================================================== */}
      <button
        type="button"
        onClick={() => setOpen((v) => !v)}
        aria-expanded={open}
        className="
          group
          flex
          h-11
          items-center
          gap-2
          rounded-full
          border
          border-white/15
          bg-white/[.06]
          px-3.5
          text-sm
          transition
          hover:border-cyan-400/40
          hover:bg-white/[.1]

          max-sm:px-3
        "
      >
        <MapPin
          size={17}
          className="
            shrink-0
            text-cyan-300
            transition
            group-hover:scale-110
          "
        />

        {/* City name */}
        <span
          className="
            max-w-28
            truncate
            font-medium
            text-white/85

            max-sm:max-w-[100px]
          "
        >
          {selectedLocation?.city || "Location"}
        </span>

        <ChevronDown
          size={15}
          className={`
            shrink-0
            text-white/60
            transition
            ${
              open
                ? "rotate-180"
                : ""
            }
          `}
        />
      </button>

      {/* =====================================================
          LOCATION POPUP
      ===================================================== */}
      {open && (
        <>
          {/* Background overlay */}
          <button
            type="button"
            className="
              fixed
              inset-0
              z-40
              cursor-default
            "
            onClick={() => setOpen(false)}
            aria-label="Close location selector"
          />

          {/* =================================================
              POPUP
          ================================================= */}
          <div
            className="
              absolute
              right-0
              top-[calc(100%+10px)]
              z-50
              w-[340px]
              overflow-hidden
              rounded-2xl
              border
              border-white/10
              bg-[#0d1119]/95
              shadow-2xl
              backdrop-blur-2xl
              animate-cine-scale

              max-sm:fixed
              max-sm:left-1/2
              max-sm:right-auto
              max-sm:top-[78px]
              max-sm:w-[300px]
              max-sm:max-w-[calc(100vw-32px)]
              max-sm:-translate-x-1/2
            "
          >

            {/* =================================================
                HEADER
            ================================================= */}
            <div
              className="
                flex
                items-center
                justify-between
                border-b
                border-white/10
                px-4
                py-3.5
              "
            >
              <div className="min-w-0 pr-3">

                <p
                  className="
                    text-base
                    font-semibold
                    text-white
                  "
                >
                  Choose your city
                </p>

                <p
                  className="
                    mt-0.5
                    text-[11px]
                    text-white/45
                  "
                >
                  Discover theatres and shows near you
                </p>

              </div>

              <button
                type="button"
                onClick={() => setOpen(false)}
                className="
                  grid
                  h-8
                  w-8
                  shrink-0
                  place-items-center
                  rounded-full
                  bg-white/[.05]
                  text-white/50
                  transition
                  hover:bg-white/10
                  hover:text-white
                "
              >
                <X size={17} />
              </button>
            </div>

            {/* =================================================
                SEARCH
            ================================================= */}
            <div className="px-3 py-3">
              <div className="relative">

                <Search
                  size={15}
                  className="
                    absolute
                    left-3
                    top-1/2
                    -translate-y-1/2
                    text-white/35
                  "
                />

                <input
                  value={query}
                  onChange={(e) =>
                    setQuery(e.target.value)
                  }
                  placeholder="Search city..."
                  className="
                    h-10
                    w-full
                    rounded-xl
                    border
                    border-white/10
                    bg-white/[.05]
                    pl-9
                    pr-3
                    text-sm
                    text-white
                    outline-none
                    placeholder:text-white/30
                    focus:border-cyan-400/50
                  "
                />

              </div>
            </div>

            {/* =================================================
                CITY LIST
            ================================================= */}
            <div
              className="
                max-h-[330px]
                overflow-y-auto
                px-2
                pb-2

                max-sm:max-h-[320px]
              "
            >

              {loading ? (
                <div
                  className="
                    flex
                    items-center
                    justify-center
                    gap-2
                    py-8
                    text-sm
                    text-white/45
                  "
                >
                  <Loader2
                    size={16}
                    className="animate-spin"
                  />

                  Loading cities...
                </div>
              ) : filtered.length === 0 ? (
                <div
                  className="
                    py-8
                    text-center
                    text-sm
                    text-white/45
                  "
                >
                  No locations available
                </div>
              ) : (
                filtered.map((location) => {
                  const active =
                    selectedLocation?.id ===
                    location.id;

                  return (
                    <button
                      type="button"
                      key={location.id}
                      onClick={() =>
                        handleSelect(location)
                      }
                      className={`
                        flex
                        w-full
                        items-center
                        justify-between
                        rounded-xl
                        px-3
                        py-2.5
                        text-left
                        transition

                        ${
                          active
                            ? "bg-cyan-400/10 text-cyan-200"
                            : "text-white/80 hover:bg-white/[.06]"
                        }
                      `}
                    >

                      <span className="min-w-0">

                        <span
                          className="
                            block
                            truncate
                            text-[15px]
                            font-medium
                          "
                        >
                          {location.city}
                        </span>

                        <span
                          className="
                            mt-0.5
                            block
                            truncate
                            text-[11px]
                            text-white/35
                          "
                        >
                          {location.state},{" "}
                          {location.country}
                        </span>

                      </span>

                      {active && (
                        <Check
                          size={16}
                          className="
                            ml-3
                            shrink-0
                            text-cyan-300
                          "
                        />
                      )}

                    </button>
                  );
                })
              )}

            </div>
          </div>
        </>
      )}
    </div>
  );
}