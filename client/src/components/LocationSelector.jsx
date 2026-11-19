import React from "react";
import { createPortal } from "react-dom";
import {
  Check,
  ChevronDown,
  Loader2,
  LocateFixed,
  MapPin,
  Search,
  X,
} from "lucide-react";
import { useLocationContext } from "../lib/LocationContext";
import { getLocations } from "../lib/api";
import {
  INDIAN_CITIES,
  POPULAR_CITIES,
  searchIndianCities,
  findIndianCity,
} from "../data/indianCities";

const normalize = (value) =>
  String(value || "")
    .toLowerCase()
    .replace(/[^a-z0-9]/g, "");

const POPULAR_SET = new Set(
  POPULAR_CITIES.map((city) => city.toLowerCase())
);

// Initial limit for the "Other Cities" alphabetical list.
// Show only the first 20 cities by default; the rest are accessible via "View all".
const INITIAL_CITY_LIMIT = 20;

/**
 * Merge the curated Indian city catalogue with any cities the backend already
 * knows about. The backend id is preserved whenever a name matches so theatre
 * discovery keeps working exactly as before; curated cities without a backend
 * record are still selectable and simply show an empty theatre list.
 */
function mergeWithBackendLocations(locations) {
  const backendByCity = new Map();

  locations.forEach((location) => {
    if (location?.city) {
      backendByCity.set(location.city.trim().toLowerCase(), location);
    }
  });

  const merged = INDIAN_CITIES.map((entry) => {
    const backend = backendByCity.get(entry.city.toLowerCase());

    return {
      id: backend?.id ?? null,
      city: entry.city,
      state: entry.state,
      country: entry.country || backend?.country || "India",
    };
  });

  const known = new Set(INDIAN_CITIES.map((entry) => entry.city.toLowerCase()));

  locations.forEach((location) => {
    if (location?.city && !known.has(location.city.trim().toLowerCase())) {
      merged.push({
        id: location.id,
        city: location.city,
        state: location.state || "",
        country: location.country || "India",
      });
    }
  });

  return merged;
}

function groupByLetter(cities) {
  const groups = new Map();

  cities.forEach((city) => {
    const letter = city.city.charAt(0).toUpperCase();
    const bucket = groups.get(letter) || [];
    bucket.push(city);
    groups.set(letter, bucket);
  });

  return Array.from(groups.entries()).sort(([a], [b]) =>
    a.localeCompare(b)
  );
}

function CityCard({ city, selected, onSelect }) {
  return (
    <button
      type="button"
      onClick={() => onSelect(city)}
      aria-pressed={selected}
      className={`
        group flex items-center justify-between gap-2 rounded-xl border px-3 py-2.5 text-left transition
        ${
          selected
            ? "border-cyan-300/50 bg-cyan-300/[.12] text-cyan-100"
            : "border-white/10 bg-white/[.03] text-white/75 hover:border-cyan-300/30 hover:bg-white/[.06] hover:text-white"
        }
      `}
    >
      <span className="min-w-0">
        <span className="block truncate text-sm font-medium">{city.city}</span>
        <span className="mt-0.5 block truncate text-[11px] text-white/35">
          {city.state && city.state !== city.city ? city.state : city.country}
        </span>
      </span>

      {selected && (
        <Check size={15} className="shrink-0 text-cyan-300" />
      )}
    </button>
  );
}

export default function LocationSelector() {
  const { selectedLocation, selectLocation } = useLocationContext();

  const [locations, setLocations] = React.useState([]);
  const [open, setOpen] = React.useState(false);
  const [loading, setLoading] = React.useState(true);
  const [query, setQuery] = React.useState("");
  const [expanded, setExpanded] = React.useState(false);
  const [locating, setLocating] = React.useState(false);
  const [locateMessage, setLocateMessage] = React.useState("");

  const searchRef = React.useRef(null);
  const triggerRef = React.useRef(null);

  React.useEffect(() => {
    let active = true;

    getLocations()
      .then((data) => {
        if (active) {
          setLocations(Array.isArray(data) ? data : []);
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

  const cities = React.useMemo(
    () => mergeWithBackendLocations(locations),
    [locations]
  );

  const popularCities = React.useMemo(
    () =>
      POPULAR_CITIES.map((name) =>
        cities.find((city) => city.city.toLowerCase() === name.toLowerCase())
      ).filter(Boolean),
    [cities]
  );

  const otherCities = React.useMemo(
    () => cities.filter((city) => !POPULAR_SET.has(city.city.toLowerCase())),
    [cities]
  );

  const otherGroups = React.useMemo(
    () =>
      groupByLetter(
        expanded ? otherCities : otherCities.slice(0, INITIAL_CITY_LIMIT)
      ),
    [otherCities, expanded]
  );

  const searchResults = React.useMemo(() => {
    const trimmed = query.trim();

    if (!trimmed) {
      return [];
    }

    // Static catalogue search handles aliases and alternate spellings; the
    // direct name/state match keeps backend-only cities searchable too.
    const matchedNames = new Set(
      searchIndianCities(trimmed).map((entry) => entry.city.toLowerCase())
    );
    const needle = normalize(trimmed);

    return cities.filter(
      (city) =>
        matchedNames.has(city.city.toLowerCase()) ||
        normalize(city.city).includes(needle) ||
        normalize(city.state).includes(needle)
    );
  }, [cities, query]);

  const close = React.useCallback(() => {
    setOpen(false);
    setQuery("");
    setLocateMessage("");
  }, []);

  // Close on Escape and lock background scrolling while the dialog is open.
  React.useEffect(() => {
    if (!open) {
      return undefined;
    }

    const onKeyDown = (event) => {
      if (event.key === "Escape") {
        close();
      }
    };

    const previousOverflow = document.body.style.overflow;
    document.body.style.overflow = "hidden";
    document.addEventListener("keydown", onKeyDown);
    searchRef.current?.focus();

    return () => {
      document.body.style.overflow = previousOverflow;
      document.removeEventListener("keydown", onKeyDown);
    };
  }, [open, close]);

  const handleSelect = (location) => {
    selectLocation(location);
    close();
    triggerRef.current?.focus();
  };

  const handleDetectLocation = () => {
    setLocateMessage("");

    if (!navigator.geolocation) {
      setLocateMessage(
        "Location is not supported by this browser. Please select your city manually."
      );
      return;
    }

    setLocating(true);

    navigator.geolocation.getCurrentPosition(
      async (position) => {
        const controller = new AbortController();
        const timeoutId = setTimeout(() => controller.abort(), 8000);

        try {
          const { latitude, longitude } = position.coords;

          const response = await fetch(
            `https://nominatim.openstreetmap.org/reverse?format=jsonv2&lat=${latitude}&lon=${longitude}&accept-language=en`,
            {
              headers: { Accept: "application/json" },
              signal: controller.signal,
            }
          );

          if (!response.ok) {
            throw new Error("Reverse geocoding failed");
          }

          const data = await response.json();
          const address = data?.address || {};
          const candidates = [
            address.city,
            address.town,
            address.village,
            address.municipality,
            address.city_district,
            address.county,
            address.state_district,
            data?.name,
          ].filter(Boolean);

          const matched = candidates
            .map((candidate) => findIndianCity(candidate))
            .find(Boolean);

          if (!matched) {
            setLocateMessage(
              "We couldn't match your location to a supported city. Please select your city manually."
            );
            return;
          }

          const resolved =
            cities.find(
              (city) =>
                city.city.toLowerCase() === matched.city.toLowerCase()
            ) || matched;

          handleSelect(resolved);
        } catch {
          setLocateMessage(
            "We couldn't detect your city right now. Please select your city manually."
          );
        } finally {
          clearTimeout(timeoutId);
          setLocating(false);
        }
      },
      (error) => {
        setLocating(false);

        if (error.code === 1) {
          setLocateMessage(
            "Location permission was denied. Please select your city manually."
          );
        } else if (error.code === 3) {
          setLocateMessage(
            "Location detection timed out. Please select your city manually."
          );
        } else {
          setLocateMessage(
            "Your location is currently unavailable. Please select your city manually."
          );
        }
      },
      { enableHighAccuracy: false, timeout: 10000, maximumAge: 60000 }
    );
  };

  const isSelected = (city) =>
    selectedLocation?.id != null && city.id != null
      ? String(selectedLocation.id) === String(city.id)
      : selectedLocation?.city?.toLowerCase() === city.city.toLowerCase();

  return (
    <div className="relative">
      {/* =====================================================
          TRIGGER
      ===================================================== */}
      <button
        ref={triggerRef}
        type="button"
        onClick={() => setOpen(true)}
        aria-haspopup="dialog"
        aria-expanded={open}
        aria-label={
          selectedLocation
            ? `Change city, currently ${selectedLocation.city}`
            : "Select your city"
        }
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
          focus-visible:outline-none
          focus-visible:ring-2
          focus-visible:ring-cyan-400/60

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
          className="shrink-0 text-white/60 transition"
        />
      </button>

      {/* =====================================================
          CITY SELECTION DIALOG
      ===================================================== */}
      {open &&
        createPortal(
          <div
            className="fixed inset-0 z-[200] flex items-start justify-center overflow-y-auto p-3 sm:p-6"
            role="presentation"
          >
            <button
              type="button"
              onClick={close}
              aria-label="Close city selector"
              className="fixed inset-0 cursor-default bg-black/70 backdrop-blur-sm"
            />

            <div
              role="dialog"
              aria-modal="true"
              aria-labelledby="city-selector-title"
              className="
                relative
                my-auto
                flex
                w-full
                max-w-3xl
                max-h-[86vh]
                flex-col
                overflow-hidden
                rounded-3xl
                border
                border-white/10
                bg-[#0b0f16]/97
                shadow-[0_30px_80px_rgba(0,0,0,.6)]
                animate-cine-scale
              "
            >
              {/* HEADER */}
              <div className="flex items-start justify-between gap-4 border-b border-white/10 px-5 py-5 sm:px-6">
                <div className="flex items-start gap-3">
                  <span className="mt-0.5 grid h-10 w-10 shrink-0 place-items-center rounded-full bg-cyan-400/10 text-cyan-300">
                    <MapPin size={19} />
                  </span>

                  <div>
                    <h2
                      id="city-selector-title"
                      className="text-lg font-semibold text-white sm:text-xl"
                    >
                      Select your city
                    </h2>
                    <p className="mt-1 text-xs text-white/45 sm:text-sm">
                      Choose your city to discover movies and theatres near you.
                    </p>
                  </div>
                </div>

                <button
                  type="button"
                  onClick={close}
                  aria-label="Close city selector"
                  className="grid h-9 w-9 shrink-0 place-items-center rounded-full bg-white/[.05] text-white/50 transition hover:bg-white/10 hover:text-white focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-cyan-400/60"
                >
                  <X size={18} />
                </button>
              </div>

              {/* SEARCH + DETECT */}
              <div className="space-y-3 border-b border-white/10 px-5 py-4 sm:px-6">
                <div className="relative">
                  <Search
                    size={17}
                    className="pointer-events-none absolute left-3.5 top-1/2 -translate-y-1/2 text-white/35"
                  />

                  <input
                    ref={searchRef}
                    type="search"
                    value={query}
                    onChange={(event) => setQuery(event.target.value)}
                    placeholder="Search for your city..."
                    aria-label="Search cities"
                    className="
                      h-11
                      w-full
                      rounded-xl
                      border
                      border-white/10
                      bg-white/[.05]
                      pl-10
                      pr-10
                      text-sm
                      text-white
                      outline-none
                      placeholder:text-white/30
                      focus:border-cyan-400/50
                      focus:ring-2
                      focus:ring-cyan-400/20
                    "
                  />

                  {query && (
                    <button
                      type="button"
                      onClick={() => {
                        setQuery("");
                        searchRef.current?.focus();
                      }}
                      aria-label="Clear search"
                      className="absolute right-2 top-1/2 grid h-7 w-7 -translate-y-1/2 place-items-center rounded-full text-white/40 transition hover:bg-white/10 hover:text-white"
                    >
                      <X size={14} />
                    </button>
                  )}
                </div>

                <button
                  type="button"
                  onClick={handleDetectLocation}
                  disabled={locating}
                  className="
                    flex
                    w-full
                    items-center
                    gap-3
                    rounded-xl
                    border
                    border-cyan-400/25
                    bg-cyan-400/[.06]
                    px-3.5
                    py-3
                    text-left
                    transition
                    hover:border-cyan-300/40
                    hover:bg-cyan-400/[.1]
                    disabled:cursor-wait
                    disabled:opacity-70
                    focus-visible:outline-none
                    focus-visible:ring-2
                    focus-visible:ring-cyan-400/60
                  "
                >
                  <span className="grid h-8 w-8 shrink-0 place-items-center rounded-full bg-cyan-400/15 text-cyan-300">
                    {locating ? (
                      <Loader2 size={16} className="animate-spin" />
                    ) : (
                      <LocateFixed size={16} />
                    )}
                  </span>

                  <span className="min-w-0">
                    <span className="block text-sm font-medium text-cyan-100">
                      {locating ? "Detecting your location..." : "Detect my location"}
                    </span>
                    <span className="block text-[11px] text-white/40">
                      Use your device location to find your city
                    </span>
                  </span>
                </button>

                {locateMessage && (
                  <p
                    role="status"
                    aria-live="polite"
                    className="rounded-lg border border-amber-300/20 bg-amber-300/[.06] px-3 py-2 text-xs text-amber-100/80"
                  >
                    {locateMessage}
                  </p>
                )}
              </div>

              {/* SCROLLABLE CONTENT */}
              <div className="min-h-0 flex-1 overflow-y-auto px-5 py-5 sm:px-6">
                {loading ? (
                  <div className="flex items-center justify-center gap-2 py-10 text-sm text-white/45">
                    <Loader2 size={16} className="animate-spin" />
                    Loading cities...
                  </div>
                ) : query.trim() ? (
                  <section aria-label="Search results">
                    <div className="mb-3 flex items-center justify-between gap-3">
                      <p className="text-[11px] font-semibold uppercase tracking-[.2em] text-white/40">
                        Search results
                      </p>
                      <p className="text-[11px] text-white/35">
                        {searchResults.length}{" "}
                        {searchResults.length === 1 ? "city" : "cities"}
                      </p>
                    </div>

                    {searchResults.length === 0 ? (
                      <div className="rounded-2xl border border-white/10 bg-white/[.03] px-5 py-10 text-center">
                        <p className="text-sm text-white/55">
                          No cities match &ldquo;{query.trim()}&rdquo;.
                        </p>
                        <p className="mt-1 text-xs text-white/35">
                          Try a different spelling or check the city name.
                        </p>
                        <button
                          type="button"
                          onClick={() => {
                            setQuery("");
                            searchRef.current?.focus();
                          }}
                          aria-label="Clear search and show all cities"
                          className="mt-4 rounded-full border border-cyan-300/30 px-4 py-1.5 text-xs text-cyan-200 transition hover:bg-cyan-300/10"
                        >
                          Clear search
                        </button>
                      </div>
                    ) : (
                      <div className="grid grid-cols-1 gap-2 sm:grid-cols-2">
                        {searchResults.map((city) => (
                          <CityCard
                            key={`${city.city}-${city.state}`}
                            city={city}
                            selected={isSelected(city)}
                            onSelect={handleSelect}
                          />
                        ))}
                      </div>
                    )}
                  </section>
                ) : (
                  <div className="space-y-7">
                    {/* POPULAR */}
                    <section aria-label="Popular cities">
                      <div className="mb-3 flex items-center justify-between gap-3">
                        <h3 className="text-[11px] font-semibold uppercase tracking-[.2em] text-cyan-300">
                          Popular cities
                        </h3>
                        <span className="text-[11px] text-white/30">
                          {popularCities.length} cities
                        </span>
                      </div>

                      <div className="grid grid-cols-2 gap-2 sm:grid-cols-3 md:grid-cols-4">
                        {popularCities.map((city) => (
                          <CityCard
                            key={`${city.city}-${city.state}`}
                            city={city}
                            selected={isSelected(city)}
                            onSelect={handleSelect}
                          />
                        ))}
                      </div>
                    </section>

                    {/* ALL CITIES */}
                    <section aria-label="All cities">
                      <div className="mb-3 flex items-center justify-between gap-3">
                        <h3 className="text-[11px] font-semibold uppercase tracking-[.2em] text-cyan-300">
                          All cities
                        </h3>
                        <span className="text-[11px] text-white/30">
                          {cities.length} cities
                        </span>
                      </div>

                      {cities.length > INITIAL_CITY_LIMIT && (
                        <button
                          type="button"
                          onClick={() => setExpanded((previous) => !previous)}
                          aria-expanded={expanded}
                          className="mb-3 rounded-full border border-cyan-400/30 px-3 py-1.5 text-xs text-cyan-200 transition hover:bg-cyan-300/10 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-cyan-400/60"
                        >
                          {expanded ? (
                            <span className="flex items-center gap-1">
                              <ChevronUp size={12} />
                              Hide all cities
                            </span>
                          ) : (
                            <span className="flex items-center gap-1">
                              <ChevronDown size={12} />
                              View all cities
                            </span>
                          )}
                        </button>
                      )}

                      <div className="space-y-4">
                        {otherGroups.map(([letter, group]) => (
                          <div
                            key={letter}
                            className="grid grid-cols-[28px_1fr] gap-3"
                          >
                            <span
                              className="mt-1 text-sm font-semibold text-cyan-300/70"
                              aria-hidden="true"
                            >
                              {letter}
                            </span>

                            <div className="flex flex-wrap gap-2">
                              {group.map((city) => {
                                const selected = isSelected(city);

                                return (
                                  <button
                                    key={`${city.city}-${city.state}`}
                                    type="button"
                                    onClick={() => handleSelect(city)}
                                    aria-pressed={selected}
                                    className={`
                                      rounded-full border px-3 py-1.5 text-xs transition
                                      ${
                                        selected
                                          ? "border-cyan-300/50 bg-cyan-300/15 text-cyan-100"
                                          : "border-white/10 bg-white/[.03] text-white/65 hover:border-cyan-300/30 hover:bg-white/[.07] hover:text-white"
                                      }
                                    `}
                                  >
                                    {selected && (
                                      <Check
                                        size={12}
                                        className="mr-1 inline align-[-2px] text-cyan-300"
                                      />
                                    )}
                                    {city.city}
                                  </button>
                                );
                              })}
                            </div>
                          </div>
                        ))}
                      </div>
                    </section>
                  </div>
                )}
              </div>
            </div>
          </div>,
          document.body
        )}
    </div>
  );
}
