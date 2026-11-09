import React from "react";
import {
  CalendarDays,
  Clock3,
  Film,
  Pencil,
  Trash2,
  X,
  Building2,
  AlertTriangle,
  Save,
} from "lucide-react";

import {
  getShows,
  deleteShow,
  updateShow,
  getMovies,
  getLocations,
  getTheatresByLocation,
} from "../../lib/api";

import {
  formatDate,
  formatTime,
  money,
} from "../../lib/formatters";

import Title from "../../components/Admin/Title";

export default function ListShows() {
  const [shows, setShows] = React.useState([]);
  const [movies, setMovies] = React.useState([]);
  const [locations, setLocations] = React.useState([]);
  const [theatres, setTheatres] = React.useState([]);

  const [loading, setLoading] = React.useState(true);
  const [saving, setSaving] = React.useState(false);
  const [deletingId, setDeletingId] = React.useState(null);

  const [showToDelete, setShowToDelete] = React.useState(null);
  const [showToEdit, setShowToEdit] = React.useState(null);

  const [error, setError] = React.useState("");

  const [editMovieId, setEditMovieId] = React.useState("");
  const [editLocationId, setEditLocationId] = React.useState("");
  const [editTheatreId, setEditTheatreId] = React.useState("");
  const [editDate, setEditDate] = React.useState("");
  const [editTime, setEditTime] = React.useState("");
  const [editPrice, setEditPrice] = React.useState("");

  const loadShows = async () => {
    try {
      setLoading(true);
      setError("");

      const data = await getShows();

      setShows(Array.isArray(data) ? data : []);
    } catch (err) {
      console.error(err);
      setError(err.message || "Failed to load shows");
    } finally {
      setLoading(false);
    }
  };

  const loadInitialData = async () => {
    try {
      const [movieData, locationData] = await Promise.all([
        getMovies(),
        getLocations(),
      ]);

      setMovies(
        Array.isArray(movieData) ? movieData : []
      );

      setLocations(
        Array.isArray(locationData) ? locationData : []
      );
    } catch (err) {
      console.error(err);
      setError(
        err.message || "Failed to load movie information"
      );
    }
  };

  React.useEffect(() => {
    loadShows();
    loadInitialData();
  }, []);

  const openEdit = async (show) => {
    setError("");
    setShowToEdit(show);

    setEditMovieId(
      String(show.movie?.id || "")
    );

    setEditDate(
      show.showDate || ""
    );

    setEditTime(
      show.showTime
        ? show.showTime.slice(0, 5)
        : ""
    );

    setEditPrice(
      String(show.ticketPrice ?? "")
    );

    const locationId = show.theatre?.location?.id;

    setEditLocationId(
      locationId ? String(locationId) : ""
    );

    setEditTheatreId(
      show.theatre?.id
        ? String(show.theatre.id)
        : ""
    );

    if (locationId) {
      try {
        const theatreData =
          await getTheatresByLocation(locationId);

        setTheatres(
          Array.isArray(theatreData)
            ? theatreData
            : []
        );
      } catch (err) {
        console.error(err);
        setTheatres([]);
      }
    } else {
      setTheatres([]);
    }
  };

  const handleLocationChange = async (value) => {
    setEditLocationId(value);
    setEditTheatreId("");
    setTheatres([]);

    if (!value) return;

    try {
      const data =
        await getTheatresByLocation(value);

      setTheatres(
        Array.isArray(data) ? data : []
      );
    } catch (err) {
      console.error(err);
      setError(
        err.message ||
        "Failed to load theatres"
      );
    }
  };

  const handleUpdate = async (e) => {
    e.preventDefault();

    if (!showToEdit) return;

    if (
      !editMovieId ||
      !editTheatreId ||
      !editDate ||
      !editTime ||
      !editPrice
    ) {
      setError(
        "Please fill all show details"
      );
      return;
    }

    try {
      setSaving(true);
      setError("");

      const updated = await updateShow(
        showToEdit.id,
        {
          movie: {
            id: Number(editMovieId),
          },

          theatre: {
            id: Number(editTheatreId),
          },

          showDate: editDate,

          showTime:
            editTime.length === 5
              ? `${editTime}:00`
              : editTime,

          ticketPrice:
            Number(editPrice),
        }
      );

      setShows((current) =>
        current.map((item) =>
          item.id === showToEdit.id
            ? updated
            : item
        )
      );

      setShowToEdit(null);

    } catch (err) {
      console.error(err);

      setError(
        err.message ||
        "Unable to update show"
      );
    } finally {
      setSaving(false);
    }
  };

  const handleDelete = async () => {
    if (!showToDelete) return;

    try {
      setDeletingId(showToDelete.id);
      setError("");

      await deleteShow(
        showToDelete.id
      );

      setShows((current) =>
        current.filter(
          (item) =>
            item.id !== showToDelete.id
        )
      );

      setShowToDelete(null);

    } catch (err) {
      console.error(err);

      setError(
        err.message ||
        "Unable to delete this show"
      );
    } finally {
      setDeletingId(null);
    }
  };

  return (
    <>
      <Title
        text1="Management"
        text2="Shows"
      />

      {error && (
        <div className="mt-6 flex items-center justify-between rounded-xl border border-red-400/20 bg-red-400/10 px-4 py-3 text-sm text-red-200">
          <span>{error}</span>

          <button
            onClick={() => setError("")}
            className="text-red-200/60 hover:text-red-200"
          >
            <X size={17} />
          </button>
        </div>
      )}

      <div className="mt-8 overflow-hidden rounded-2xl border border-white/10 bg-white/[.03]">

        {loading ? (
          <div className="p-10 text-center text-white/40">
            Loading shows...
          </div>
        ) : shows.length ? (

          <div className="overflow-x-auto">

            <table className="w-full min-w-[900px] text-left text-sm">

              <thead className="border-b border-white/10 bg-white/[.03] text-xs uppercase tracking-wider text-white/30">

                <tr>
                  <th className="px-5 py-4">
                    Movie
                  </th>

                  <th className="px-5 py-4">
                    Theatre
                  </th>

                  <th className="px-5 py-4">
                    Date
                  </th>

                  <th className="px-5 py-4">
                    Time
                  </th>

                  <th className="px-5 py-4">
                    Price
                  </th>

                  <th className="px-5 py-4 text-right">
                    Actions
                  </th>
                </tr>

              </thead>

              <tbody>

                {shows.map((show) => (

                  <tr
                    key={show.id}
                    className="border-b border-white/[.06] transition hover:bg-white/[.025]"
                  >

                    <td className="px-5 py-4">

                      <div className="flex items-center gap-3">

                        <span className="grid h-9 w-9 place-items-center rounded-lg bg-cyan-300/10 text-cyan-300">
                          <Film size={16} />
                        </span>

                        <div>
                          <p className="font-medium text-white">
                            {show.movie?.title ||
                              "Movie"}
                          </p>

                          <p className="text-xs text-white/30">
                            Show #{show.id}
                          </p>
                        </div>

                      </div>

                    </td>

                    <td className="px-5 py-4 text-white/60">
                      {show.theatre?.name ||
                        "—"}
                    </td>

                    <td className="px-5 py-4 text-white/55">

                      <CalendarDays
                        className="mr-1 inline"
                        size={14}
                      />

                      {formatDate(
                        show.showDate
                      )}

                    </td>

                    <td className="px-5 py-4 text-white/55">

                      <Clock3
                        className="mr-1 inline"
                        size={14}
                      />

                      {formatTime(
                        show.showTime
                      )}

                    </td>

                    <td className="px-5 py-4 font-medium text-cyan-200">
                      {money(
                        show.ticketPrice
                      )}
                    </td>

                    <td className="px-5 py-4">

                      <div className="flex justify-end gap-2">

                        <button
                          type="button"
                          onClick={() =>
                            openEdit(show)
                          }
                          className="grid h-9 w-9 place-items-center rounded-lg border border-white/10 bg-white/[.03] text-white/50 transition hover:border-cyan-300/20 hover:bg-cyan-300/10 hover:text-cyan-200"
                          title="Edit show"
                        >
                          <Pencil size={16} />
                        </button>

                        <button
                          type="button"
                          onClick={() =>
                            setShowToDelete(show)
                          }
                          disabled={
                            deletingId === show.id
                          }
                          className="grid h-9 w-9 place-items-center rounded-lg border border-white/10 bg-white/[.03] text-white/50 transition hover:border-red-400/20 hover:bg-red-400/10 hover:text-red-300 disabled:cursor-not-allowed disabled:opacity-40"
                          title="Delete show"
                        >
                          <Trash2 size={16} />
                        </button>

                      </div>

                    </td>

                  </tr>

                ))}

              </tbody>

            </table>

          </div>

        ) : (

          <div className="p-12 text-center text-white/40">
            No shows found.
          </div>

        )}

      </div>

      {/* EDIT SHOW MODAL */}

      {showToEdit && (
        <div
          className="fixed inset-0 z-[100] flex items-center justify-center bg-black/75 p-5 backdrop-blur-md"
          onClick={() => {
            if (!saving) {
              setShowToEdit(null);
            }
          }}
        >

          <div
            onClick={(e) =>
              e.stopPropagation()
            }
            className="w-full max-w-xl max-h-[90vh] overflow-y-auto rounded-2xl border border-white/10 bg-[#0b0f15] shadow-[0_25px_80px_rgba(0,0,0,.65)]"
          >

            <div className="sticky top-0 z-10 border-b border-white/[.07] bg-[#0b0f15] px-6 py-5">

              <div className="flex items-center justify-between">

                <div>
                  <p className="text-[10px] font-semibold uppercase tracking-[.22em] text-cyan-300/60">
                    Management
                  </p>

                  <h2 className="mt-1 text-xl font-semibold text-white">
                    Edit Show
                  </h2>
                </div>

                <button
                  type="button"
                  disabled={saving}
                  onClick={() =>
                    setShowToEdit(null)
                  }
                  className="grid h-9 w-9 place-items-center rounded-lg text-white/30 transition hover:bg-white/5 hover:text-white"
                >
                  <X size={18} />
                </button>

              </div>

            </div>

            <form
              onSubmit={handleUpdate}
              className="space-y-5 p-6"
            >

              {/* MOVIE */}

              <div>
                <label className="mb-2 block text-xs font-medium uppercase tracking-wider text-white/35">
                  Movie
                </label>

                <select
                  value={editMovieId}
                  onChange={(e) =>
                    setEditMovieId(
                      e.target.value
                    )
                  }
                  className="w-full rounded-xl border border-white/10 bg-[#10151d] px-4 py-3 text-sm text-white outline-none transition focus:border-cyan-300/40"
                >

                  <option value="">
                    Select movie
                  </option>

                  {movies.map((movie) => (
                    <option
                      key={movie.id}
                      value={movie.id}
                    >
                      {movie.title}
                    </option>
                  ))}

                </select>
              </div>

              {/* LOCATION */}

              <div>
                <label className="mb-2 block text-xs font-medium uppercase tracking-wider text-white/35">
                  City
                </label>

                <select
                  value={editLocationId}
                  onChange={(e) =>
                    handleLocationChange(
                      e.target.value
                    )
                  }
                  className="w-full rounded-xl border border-white/10 bg-[#10151d] px-4 py-3 text-sm text-white outline-none transition focus:border-cyan-300/40"
                >

                  <option value="">
                    Select city
                  </option>

                  {locations.map((location) => (
                    <option
                      key={location.id}
                      value={location.id}
                    >
                      {location.city}
                    </option>
                  ))}

                </select>
              </div>

              {/* THEATRE */}

              <div>
                <label className="mb-2 block text-xs font-medium uppercase tracking-wider text-white/35">
                  Theatre
                </label>

                <select
                  value={editTheatreId}
                  onChange={(e) =>
                    setEditTheatreId(
                      e.target.value
                    )
                  }
                  disabled={!editLocationId}
                  className="w-full rounded-xl border border-white/10 bg-[#10151d] px-4 py-3 text-sm text-white outline-none transition focus:border-cyan-300/40 disabled:cursor-not-allowed disabled:opacity-40"
                >

                  <option value="">
                    Select theatre
                  </option>

                  {theatres.map((theatre) => (
                    <option
                      key={theatre.id}
                      value={theatre.id}
                    >
                      {theatre.name} —{" "}
                      {theatre.totalSeats} seats
                    </option>
                  ))}

                </select>
              </div>

              {/* DATE + TIME */}

              <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">

                <div>
                  <label className="mb-2 block text-xs font-medium uppercase tracking-wider text-white/35">
                    Date
                  </label>

                  <input
                    type="date"
                    value={editDate}
                    min={
                      new Date()
                        .toISOString()
                        .split("T")[0]
                    }
                    onChange={(e) =>
                      setEditDate(
                        e.target.value
                      )
                    }
                    className="w-full rounded-xl border border-white/10 bg-[#10151d] px-4 py-3 text-sm text-white outline-none focus:border-cyan-300/40"
                  />
                </div>

                <div>
                  <label className="mb-2 block text-xs font-medium uppercase tracking-wider text-white/35">
                    Time
                  </label>

                  <input
                    type="time"
                    value={editTime}
                    onChange={(e) =>
                      setEditTime(
                        e.target.value
                      )
                    }
                    className="w-full rounded-xl border border-white/10 bg-[#10151d] px-4 py-3 text-sm text-white outline-none focus:border-cyan-300/40"
                  />
                </div>

              </div>

              {/* PRICE */}

              <div>

                <label className="mb-2 block text-xs font-medium uppercase tracking-wider text-white/35">
                  Ticket Price
                </label>

                <div className="relative">

                  <span className="absolute left-4 top-1/2 -translate-y-1/2 text-white/30">
                    ₹
                  </span>

                  <input
                    type="number"
                    min="1"
                    max="10000"
                    value={editPrice}
                    onChange={(e) =>
                      setEditPrice(
                        e.target.value
                      )
                    }
                    className="w-full rounded-xl border border-white/10 bg-[#10151d] py-3 pl-9 pr-4 text-sm text-white outline-none focus:border-cyan-300/40"
                    placeholder="Ticket price"
                  />

                </div>

              </div>

              {/* BUTTONS */}

              <div className="flex gap-3 border-t border-white/[.07] pt-5">

                <button
                  type="button"
                  disabled={saving}
                  onClick={() =>
                    setShowToEdit(null)
                  }
                  className="flex-1 rounded-xl border border-white/10 bg-white/[.03] px-4 py-3 text-sm font-medium text-white/60 transition hover:bg-white/[.06] hover:text-white"
                >
                  Cancel
                </button>

                <button
                  type="submit"
                  disabled={saving}
                  className="flex flex-1 items-center justify-center gap-2 rounded-xl bg-cyan-300 px-4 py-3 text-sm font-semibold text-[#061014] transition hover:bg-cyan-200 disabled:cursor-not-allowed disabled:opacity-50"
                >

                  {saving ? (
                    <>
                      <span className="h-4 w-4 animate-spin rounded-full border-2 border-black/20 border-t-black" />
                      Saving...
                    </>
                  ) : (
                    <>
                      <Save size={16} />
                      Save Changes
                    </>
                  )}

                </button>

              </div>

            </form>

          </div>

        </div>
      )}

      {/* DELETE SHOW MODAL */}

      {showToDelete && (
        <div
          className="fixed inset-0 z-[110] flex items-center justify-center bg-black/75 p-5 backdrop-blur-md"
          onClick={() => {
            if (!deletingId) {
              setShowToDelete(null);
            }
          }}
        >

          <div
            onClick={(e) =>
              e.stopPropagation()
            }
            className="w-full max-w-md overflow-hidden rounded-2xl border border-white/10 bg-[#0b0f15] shadow-[0_25px_80px_rgba(0,0,0,.65)]"
          >

            <div className="border-b border-white/[.07] px-6 py-5">

              <div className="flex items-start justify-between">

                <div className="flex items-center gap-3">

                  <div className="grid h-11 w-11 place-items-center rounded-xl bg-red-400/10 text-red-300">
                    <Trash2 size={19} />
                  </div>

                  <div>
                    <h2 className="text-lg font-semibold text-white">
                      Delete Show
                    </h2>

                    <p className="mt-0.5 text-xs text-white/35">
                      This action cannot be undone
                    </p>
                  </div>

                </div>

                <button
                  type="button"
                  disabled={Boolean(deletingId)}
                  onClick={() =>
                    setShowToDelete(null)
                  }
                  className="grid h-8 w-8 place-items-center rounded-lg text-white/30 hover:bg-white/5 hover:text-white"
                >
                  <X size={17} />
                </button>

              </div>

            </div>

            <div className="px-6 py-5">

              <div className="rounded-xl border border-white/[.07] bg-white/[.025] p-4">

                <div className="flex items-center gap-3">

                  <div className="grid h-10 w-10 place-items-center rounded-lg bg-cyan-300/10 text-cyan-300">
                    <Film size={17} />
                  </div>

                  <div className="min-w-0">

                    <p className="truncate text-sm font-semibold text-white">
                      {showToDelete.movie?.title ||
                        "Movie"}
                    </p>

                    <p className="mt-0.5 text-xs text-white/35">
                      Show #{showToDelete.id}
                    </p>

                  </div>

                </div>

                <div className="mt-4 grid grid-cols-2 gap-2">

                  <div className="rounded-lg border border-white/[.05] bg-black/10 p-3">
                    <div className="flex items-center gap-2 text-white/30">
                      <Building2 size={13} />
                      <span className="text-[10px] uppercase tracking-wider">
                        Theatre
                      </span>
                    </div>

                    <p className="mt-1.5 truncate text-sm text-white/75">
                      {showToDelete.theatre?.name ||
                        "—"}
                    </p>
                  </div>

                  <div className="rounded-lg border border-white/[.05] bg-black/10 p-3">
                    <div className="flex items-center gap-2 text-white/30">
                      <CalendarDays size={13} />
                      <span className="text-[10px] uppercase tracking-wider">
                        Date
                      </span>
                    </div>

                    <p className="mt-1.5 text-sm text-white/75">
                      {formatDate(
                        showToDelete.showDate
                      )}
                    </p>
                  </div>

                  <div className="rounded-lg border border-white/[.05] bg-black/10 p-3">
                    <div className="flex items-center gap-2 text-white/30">
                      <Clock3 size={13} />
                      <span className="text-[10px] uppercase tracking-wider">
                        Time
                      </span>
                    </div>

                    <p className="mt-1.5 text-sm text-white/75">
                      {formatTime(
                        showToDelete.showTime
                      )}
                    </p>
                  </div>

                  <div className="rounded-lg border border-white/[.05] bg-black/10 p-3">
                    <div className="flex items-center gap-2 text-white/30">
                      <span className="text-[11px]">
                        ₹
                      </span>

                      <span className="text-[10px] uppercase tracking-wider">
                        Ticket
                      </span>
                    </div>

                    <p className="mt-1.5 text-sm text-cyan-200">
                      {money(
                        showToDelete.ticketPrice
                      )}
                    </p>
                  </div>

                </div>

              </div>

              <div className="mt-4 flex gap-3 rounded-xl border border-red-400/10 bg-red-400/[.045] p-4">

                <div className="mt-0.5 shrink-0 text-red-300">
                  <AlertTriangle size={17} />
                </div>

                <div>

                  <p className="text-sm font-medium text-red-200">
                    Delete this show?
                  </p>

                  <p className="mt-1 text-xs leading-5 text-red-200/45">
                    All seats created for this show
                    will also be removed. Shows with
                    existing bookings cannot be deleted.
                  </p>

                </div>

              </div>

            </div>

            <div className="flex gap-3 border-t border-white/[.07] bg-white/[.015] px-6 py-4">

              <button
                type="button"
                disabled={Boolean(deletingId)}
                onClick={() =>
                  setShowToDelete(null)
                }
                className="flex-1 rounded-xl border border-white/10 bg-white/[.03] px-4 py-3 text-sm font-medium text-white/60 hover:bg-white/[.06] hover:text-white"
              >
                Cancel
              </button>

              <button
                type="button"
                disabled={Boolean(deletingId)}
                onClick={handleDelete}
                className="flex flex-1 items-center justify-center gap-2 rounded-xl bg-red-500/90 px-4 py-3 text-sm font-semibold text-white hover:bg-red-500 disabled:opacity-50"
              >

                {deletingId ? (
                  <>
                    <span className="h-4 w-4 animate-spin rounded-full border-2 border-white/30 border-t-white" />
                    Deleting...
                  </>
                ) : (
                  <>
                    <Trash2 size={15} />
                    Delete Show
                  </>
                )}

              </button>

            </div>

          </div>

        </div>
      )}
    </>
  );
}
