import React from "react";
import { AlertCircle, ChevronDown, Filter, RefreshCw, Search, SlidersHorizontal, X } from "lucide-react";
import { useQuery } from "@tanstack/react-query";
import { useSearchParams } from "react-router-dom";
import { getMovies } from "../lib/api";
import { queryKeys } from "../lib/queryKeys";
import MovieCard from "../components/MovieCard";
import Loading from "../components/Loading";

const SORT_OPTIONS = [
  { value: "latest", label: "Latest" },
  { value: "rated", label: "Top rated" },
  { value: "alpha", label: "A–Z" },
  { value: "recent", label: "Recently added" },
];

const RATING_OPTIONS = [
  { value: 0, label: "Any rating" },
  { value: 5, label: "5+ stars" },
  { value: 6, label: "6+ stars" },
  { value: 7, label: "7+ stars" },
  { value: 8, label: "8+ stars" },
];

const AVAILABILITY_OPTIONS = [
  { value: "all", label: "All" },
  { value: "released", label: "Released" },
  { value: "upcoming", label: "Upcoming" },
];

function FilterChip({ label, onRemove }) {
  return (
    <button
      type="button"
      onClick={onRemove}
      className="flex items-center gap-1.5 rounded-full border border-cyan-300/30 bg-cyan-300/10 px-3 py-1 text-xs text-cyan-200 transition hover:bg-cyan-300/15"
    >
      {label}
      <X size={12} />
    </button>
  );
}

function CompactSelect({ value, onChange, options, label }) {
  return (
    <div className="relative">
      <select
        value={value}
        onChange={(e) => onChange(e.target.value)}
        aria-label={label}
        className="h-9 appearance-none rounded-full border border-white/10 bg-white/[.04] pl-3 pr-8 text-xs text-white/70 outline-none transition hover:border-white/20 focus:border-cyan-300/40"
      >
        {options.map((opt) => (
          <option key={opt.value} value={opt.value} className="bg-[#0a0e14]">
            {opt.label}
          </option>
        ))}
      </select>
      <ChevronDown
        size={12}
        className="pointer-events-none absolute right-2.5 top-1/2 -translate-y-1/2 text-white/40"
      />
    </div>
  );
}

export default function Movies() {
  const [params, setParams] = useSearchParams();
  const [query, setQuery] = React.useState(params.get("search") || "");
  const [genre, setGenre] = React.useState(params.get("genre") || "All");
  const [language, setLanguage] = React.useState(params.get("language") || "All");
  const [year, setYear] = React.useState(params.get("year") || "All");
  const [minRating, setMinRating] = React.useState(Number(params.get("rating") || 0));
  const [availability, setAvailability] = React.useState(params.get("availability") || "all");
  const [sortBy, setSortBy] = React.useState(params.get("sort") || "latest");

  const [drawerOpen, setDrawerOpen] = React.useState(false);

  const { data: moviesData, isLoading, isError, error: queryError, refetch } = useQuery({
    queryKey: queryKeys.movies.all(),
    queryFn: async () => {
      const data = await getMovies();
      return Array.isArray(data) ? data : [];
    },
    staleTime: 1000 * 60 * 5,
  });

  const movies = React.useMemo(() => moviesData || [], [moviesData]);
  const loading = isLoading;
  const error = isError ? (queryError?.message || "Unable to load movies.") : "";

  React.useEffect(() => {
    const next = {};
    if (query) next.search = query;
    if (genre !== "All") next.genre = genre;
    if (language !== "All") next.language = language;
    if (year !== "All") next.year = year;
    if (minRating > 0) next.rating = String(minRating);
    if (availability !== "all") next.availability = availability;
    if (sortBy !== "latest") next.sort = sortBy;
    setParams(next, { replace: true });
  }, [query, genre, language, year, minRating, availability, sortBy, setParams]);

  const genres = React.useMemo(() => {
    const set = new Set();
    movies.forEach((m) => {
      if (m.genre) {
        String(m.genre)
          .split(",")
          .map((x) => x.trim())
          .filter(Boolean)
          .forEach((g) => set.add(g));
      }
      if (Array.isArray(m.genres)) {
        m.genres.forEach((g) => g?.name && set.add(g.name));
      }
    });
    return ["All", ...Array.from(set).sort()];
  }, [movies]);

  const languages = React.useMemo(() => {
    const set = new Set();
    movies.forEach((m) => {
      const lang = m.language || m.original_language;
      if (lang) set.add(String(lang).trim());
    });
    return ["All", ...Array.from(set).sort()];
  }, [movies]);

  const years = React.useMemo(() => {
    const set = new Set();
    movies.forEach((m) => {
      const d = m.releaseDate || m.release_date;
      if (d) {
        const y = String(d).slice(0, 4);
        if (/^\d{4}$/.test(y)) set.add(y);
      }
    });
    return ["All", ...Array.from(set).sort().reverse()];
  }, [movies]);

  const filtered = React.useMemo(() => {
    const today = new Date().toISOString().slice(0, 10);
    let result = movies.filter((m) => {
      const title = (m.title || "").toLowerCase().includes(query.toLowerCase());
      if (!title) return false;

      // Hide movies without a valid poster path. If no poster field exists at all
      // (e.g. simplified test data), allow the movie through.
      const hasPosterField = "posterUrl" in m || "poster_path" in m || "poster" in m;
      if (hasPosterField) {
        const poster = m.posterUrl || m.poster_path || m.poster;
        if (!poster || String(poster).trim() === "") return false;
      }

      const g =
        genre === "All" ||
        String(m.genre || "").toLowerCase().includes(genre.toLowerCase()) ||
        (Array.isArray(m.genres) && m.genres.some((x) => x.name === genre));
      if (!g) return false;

      const lang = m.language || m.original_language;
      const l =
        language === "All" || String(lang || "").toLowerCase() === language.toLowerCase();
      if (!l) return false;

      const d = m.releaseDate || m.release_date;
      const y = d ? String(d).slice(0, 4) : "";
      if (year !== "All" && y !== year) return false;

      const r = Number(m.rating ?? m.vote_average ?? 0);
      if (minRating > 0 && r < minRating) return false;

      if (availability !== "all") {
        const released = !d || d <= today;
        if (availability === "released" && !released) return false;
        if (availability === "upcoming" && released) return false;
      }

      return true;
    });

    result = [...result].sort((a, b) => {
      if (sortBy === "latest") {
        const da = a.releaseDate || a.release_date || "";
        const db = b.releaseDate || b.release_date || "";
        return db.localeCompare(da);
      }
      if (sortBy === "rated") {
        return (
          Number(b.rating ?? b.vote_average ?? 0) -
          Number(a.rating ?? a.vote_average ?? 0)
        );
      }
      if (sortBy === "alpha") {
        return (a.title || "").localeCompare(b.title || "");
      }
      if (sortBy === "recent") {
        return (b.id ?? 0) - (a.id ?? 0);
      }
      return 0;
    });

    return result;
  }, [movies, query, genre, language, year, minRating, availability, sortBy]);

  const activeChips = [];
  if (query) activeChips.push({ label: `Search: ${query}`, clear: () => setQuery("") });
  if (genre !== "All") activeChips.push({ label: `Genre: ${genre}`, clear: () => setGenre("All") });
  if (language !== "All") activeChips.push({ label: `Language: ${language}`, clear: () => setLanguage("All") });
  if (year !== "All") activeChips.push({ label: `Year: ${year}`, clear: () => setYear("All") });
  if (minRating > 0) activeChips.push({ label: `Rating: ${minRating}+`, clear: () => setMinRating(0) });
  if (availability !== "all") {
    const label = AVAILABILITY_OPTIONS.find((o) => o.value === availability)?.label || availability;
    activeChips.push({ label, clear: () => setAvailability("all") });
  }

  const clearAll = () => {
    setQuery("");
    setGenre("All");
    setLanguage("All");
    setYear("All");
    setMinRating(0);
    setAvailability("all");
    setSortBy("latest");
  };

  const hasActiveFilters = activeChips.length > 0 || sortBy !== "latest";

  const FilterRow = (
    <div className="flex flex-wrap items-center gap-2">
      <CompactSelect
        label="Genre"
        value={genre}
        onChange={setGenre}
        options={genres.map((g) => ({ value: g, label: g }))}
      />
      <CompactSelect
        label="Language"
        value={language}
        onChange={setLanguage}
        options={languages.map((l) => ({ value: l, label: l }))}
      />
      <CompactSelect
        label="Year"
        value={year}
        onChange={setYear}
        options={years.map((y) => ({ value: y, label: y }))}
      />
      <CompactSelect
        label="Rating"
        value={minRating}
        onChange={(v) => setMinRating(Number(v))}
        options={RATING_OPTIONS}
      />
      <CompactSelect
        label="Availability"
        value={availability}
        onChange={setAvailability}
        options={AVAILABILITY_OPTIONS}
      />
      <CompactSelect
        label="Sort"
        value={sortBy}
        onChange={setSortBy}
        options={SORT_OPTIONS}
      />
    </div>
  );

  return (
    <main className="min-h-screen px-5 pb-12 pt-24 sm:px-8 lg:px-12">
      <div className="mx-auto max-w-[1440px]">
        <div className="animate-cine-rise">
          <p className="text-xs font-semibold uppercase tracking-[.25em] text-cyan-300">
            Discover
          </p>
          <h1 className="mt-1 text-4xl font-semibold sm:text-5xl">Movies</h1>
          <p className="mt-2 max-w-xl text-sm leading-6 text-white/40">
            Find your next favourite story and book the seats that feel just
            right.
          </p>
        </div>

        {/* Search bar */}
        <div className="mt-4 flex flex-col gap-3 sm:flex-row">
          <div className="relative flex-1">
            <Search
              size={18}
              className="absolute left-4 top-1/2 -translate-y-1/2 text-white/30"
            />
            <input
              value={query}
              onChange={(e) => setQuery(e.target.value)}
              placeholder="Search by movie title..."
              className="h-11 w-full rounded-xl border border-white/10 bg-white/[.04] pl-11 pr-11 text-sm outline-none transition focus:border-cyan-300/40 focus:bg-white/[.06]"
            />
            {query && (
              <button
                onClick={() => setQuery("")}
                className="absolute right-3 top-1/2 -translate-y-1/2 rounded-full p-1.5 text-white/40 hover:text-white"
                aria-label="Clear search"
              >
                <X size={16} />
              </button>
            )}
          </div>

          <button
            type="button"
            onClick={() => setDrawerOpen(true)}
            className="flex h-11 items-center gap-2 rounded-xl border border-white/10 bg-white/[.04] px-4 text-sm text-white/60 transition hover:border-cyan-300/30 hover:text-white sm:hidden"
          >
            <Filter size={16} />
            Filters
            {activeChips.length > 0 && (
              <span className="grid h-5 w-5 place-items-center rounded-full bg-cyan-300/20 text-[10px] text-cyan-200">
                {activeChips.length}
              </span>
            )}
          </button>
        </div>

        {/* Desktop compact filter bar */}
        <div className="mt-4 hidden sm:block">
          <div className="flex flex-wrap items-center gap-2 rounded-xl border border-white/[.06] bg-white/[.02] px-3 py-2.5">
            {FilterRow}
            {hasActiveFilters && (
              <button
                type="button"
                onClick={clearAll}
                className="ml-auto text-xs text-cyan-300/70 transition hover:text-cyan-200"
              >
                Clear all
              </button>
            )}
          </div>
        </div>

        {/* Mobile filter drawer */}
        {drawerOpen && (
          <div className="fixed inset-0 z-50 sm:hidden">
            <div
              className="absolute inset-0 bg-black/60 backdrop-blur-sm"
              onClick={() => setDrawerOpen(false)}
            />
            <div className="absolute bottom-0 left-0 right-0 max-h-[80vh] overflow-y-auto rounded-t-2xl bg-[#0a0e14] p-5 shadow-2xl">
              <div className="mb-4 flex items-center justify-between">
                <h2 className="text-base font-semibold">Filters &amp; sorting</h2>
                <button
                  type="button"
                  onClick={() => setDrawerOpen(false)}
                  className="grid h-8 w-8 place-items-center rounded-full border border-white/10 text-white/50 hover:text-white"
                >
                  <X size={16} />
                </button>
              </div>
              {FilterRow}
              <div className="mt-5 flex gap-2">
                {hasActiveFilters && (
                  <button
                    type="button"
                    onClick={clearAll}
                    className="flex-1 rounded-xl border border-white/10 bg-white/[.03] px-4 py-2.5 text-sm text-white/60 transition hover:text-white"
                  >
                    Clear all
                  </button>
                )}
                <button
                  type="button"
                  onClick={() => setDrawerOpen(false)}
                  className="flex-1 rounded-xl bg-cyan-400 px-4 py-2.5 text-sm font-semibold text-slate-950 transition hover:bg-cyan-300"
                >
                  Apply
                </button>
              </div>
            </div>
          </div>
        )}

        {/* Active filter chips */}
        {activeChips.length > 0 && (
          <div className="mt-3 flex flex-wrap gap-2">
            {activeChips.map((chip, i) => (
              <FilterChip key={i} label={chip.label} onRemove={chip.clear} />
            ))}
          </div>
        )}

        {/* Results */}
        {loading ? (
          <Loading label="Loading movies..." />
        ) : error ? (
          <div className="mt-6 flex flex-wrap items-center justify-between gap-3 rounded-xl border border-red-400/25 bg-red-400/[.07] px-4 py-3 text-sm text-red-200">
            <span className="flex items-center gap-2">
              <AlertCircle size={15} className="shrink-0" />
              {error}
            </span>
            <button
              type="button"
              onClick={() => refetch()}
              className="flex items-center gap-1.5 rounded-lg border border-red-300/30 px-2.5 py-1 text-xs text-red-100 transition hover:bg-red-300/10"
            >
              <RefreshCw size={12} />
              Retry
            </button>
          </div>
        ) : (
          <>
            <div className="mb-4 mt-5 text-sm text-white/35">
              {filtered.length} movie{filtered.length !== 1 ? "s" : ""} found
            </div>
            {filtered.length > 0 ? (
              <div className="grid grid-cols-2 gap-4 sm:grid-cols-3 lg:grid-cols-5 xl:grid-cols-6">
                {filtered.map((m, i) => (
                  <MovieCard key={m.id ?? m._id} movie={m} index={i} />
                ))}
              </div>
            ) : (
              <div className="rounded-3xl border border-white/10 bg-white/[.03] p-16 text-center text-white/40">
                No movies match your search or filters.
              </div>
            )}
          </>
        )}
      </div>
    </main>
  );
}
