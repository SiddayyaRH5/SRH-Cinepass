import React from "react";
import {
  Building2,
  CalendarDays,
  Edit3,
  MapPin,
  Plus,
  RefreshCw,
  Trash2,
  Users,
  X,
} from "lucide-react";
import toast from "react-hot-toast";
import { useNavigate } from "react-router-dom";

import Title from "../../components/Admin/Title";

import {
  getAllTheatres,
  getLocations,
  updateTheatre,
  deleteTheatre,
} from "../../lib/api";


export default function ManageTheatres() {

  const navigate = useNavigate();

  const [theatres, setTheatres] =
    React.useState([]);

  const [locations, setLocations] =
    React.useState([]);

  const [loading, setLoading] =
    React.useState(true);

  const [refreshing, setRefreshing] =
    React.useState(false);

  const [editing, setEditing] =
    React.useState(null);

  const [deleting, setDeleting] =
    React.useState(null);

  const [saving, setSaving] =
    React.useState(false);


  /* =====================================================
     LOAD DATA
  ===================================================== */

  const loadData = async (
    showRefresh = false
  ) => {

    try {

      if (showRefresh) {
        setRefreshing(true);
      } else {
        setLoading(true);
      }

      const [theatreData, locationData] =
        await Promise.all([
          getAllTheatres(),
          getLocations(),
        ]);

      setTheatres(
        Array.isArray(theatreData)
          ? theatreData
          : []
      );

      setLocations(
        Array.isArray(locationData)
          ? locationData
          : []
      );

    } catch (error) {

      toast.error(
        error.message ||
        "Unable to load theatres"
      );

    } finally {

      setLoading(false);
      setRefreshing(false);
    }
  };


  React.useEffect(() => {
    loadData();
  }, []);


  /* =====================================================
     EDIT
  ===================================================== */

  const openEdit = (theatre) => {

    setEditing({
      id: theatre.id,
      name: theatre.name || "",
      totalSeats: theatre.totalSeats || "",
      locationId:
        theatre.location?.id
          ? String(theatre.location.id)
          : "",
    });
  };


  const saveEdit = async (e) => {

    e.preventDefault();

    if (!editing.name.trim()) {
      return toast.error(
        "Enter theatre name"
      );
    }

    const seats =
      Number(editing.totalSeats);

    if (
      !Number.isInteger(seats) ||
      seats < 10 ||
      seats > 2000
    ) {
      return toast.error(
        "Seats must be between 10 and 2000"
      );
    }

    if (!editing.locationId) {
      return toast.error(
        "Select a city"
      );
    }

    try {

      setSaving(true);

      await updateTheatre(
        editing.id,
        {
          name: editing.name.trim(),
          totalSeats: seats,
          location: {
            id: Number(
              editing.locationId
            ),
          },
        }
      );

      toast.success(
        "Theatre updated successfully"
      );

      setEditing(null);

      await loadData(true);

    } catch (error) {

      toast.error(
        error.message ||
        "Unable to update theatre"
      );

    } finally {

      setSaving(false);
    }
  };


  /* =====================================================
     DELETE
  ===================================================== */

  const confirmDelete = async () => {

    if (!deleting) return;

    try {

      setSaving(true);

      await deleteTheatre(
        deleting.id
      );

      toast.success(
        "Theatre deleted successfully"
      );

      setDeleting(null);

      await loadData(true);

    } catch (error) {

      toast.error(
        error.message ||
        "Unable to delete theatre. It may contain shows."
      );

    } finally {

      setSaving(false);
    }
  };


  /* =====================================================
     LOADING
  ===================================================== */

  if (loading) {

    return (
      <>
        <Title
          text1="Management"
          text2="Theatres"
        />

        <div
          className="
            mt-8
            rounded-2xl
            border border-white/10
            bg-white/[.03]
            p-12
            text-center
          "
        >

          <div
            className="
              mx-auto
              h-7
              w-7
              animate-spin
              rounded-full
              border-2
              border-white/10
              border-t-cyan-300
            "
          />

          <p className="mt-4 text-sm text-white/35">
            Loading theatres...
          </p>

        </div>
      </>
    );
  }


  return (
    <>
      <Title
        text1="Management"
        text2="Theatres"
      />


      {/* =================================================
          TOP BAR
      ================================================= */}

      <div
        className="
          mt-8
          flex
          flex-col
          gap-3
          sm:flex-row
          sm:items-center
          sm:justify-between
        "
      >

        <div>

          <p className="text-sm text-white/40">
            {theatres.length} theatre
            {theatres.length !== 1
              ? "s"
              : ""} registered
          </p>

        </div>


        <div className="flex gap-2">

          <button
            type="button"
            onClick={() => loadData(true)}
            disabled={refreshing}
            className="
              flex
              items-center
              justify-center
              gap-2
              rounded-xl
              border border-white/10
              bg-white/[.03]
              px-4
              py-2.5
              text-xs
              text-white/55
              transition
              hover:bg-white/[.06]
              hover:text-white
              disabled:opacity-40
            "
          >

            <RefreshCw
              size={15}
              className={
                refreshing
                  ? "animate-spin"
                  : ""
              }
            />

            Refresh

          </button>


          <button
            type="button"
            onClick={() =>
              navigate(
                "/admin/add-theatre"
              )
            }
            className="
              flex
              items-center
              justify-center
              gap-2
              rounded-xl
              bg-cyan-300
              px-4
              py-2.5
              text-xs
              font-semibold
              text-black
              transition
              hover:bg-cyan-200
            "
          >

            <Plus size={15} />

            Add Theatre

          </button>

        </div>

      </div>


      {/* =================================================
          EMPTY
      ================================================= */}

      {theatres.length === 0 ? (

        <div
          className="
            mt-6
            rounded-2xl
            border border-white/10
            bg-white/[.03]
            p-12
            text-center
          "
        >

          <div
            className="
              mx-auto
              grid
              h-14
              w-14
              place-items-center
              rounded-2xl
              bg-cyan-300/10
              text-cyan-300
            "
          >

            <Building2 size={25} />

          </div>

          <h2 className="mt-5 text-lg font-semibold">
            No theatres yet
          </h2>

          <p className="mx-auto mt-2 max-w-md text-sm leading-6 text-white/35">
            Add your first theatre before
            creating movie shows.
          </p>

          <button
            type="button"
            onClick={() =>
              navigate(
                "/admin/add-theatre"
              )
            }
            className="
              mt-6
              rounded-xl
              bg-cyan-300
              px-5
              py-3
              text-sm
              font-semibold
              text-black
              hover:bg-cyan-200
            "
          >
            Add Theatre
          </button>

        </div>

      ) : (

        /* =================================================
           THEATRE GRID
        ================================================= */

        <div
          className="
            mt-6
            grid
            gap-4
            md:grid-cols-2
            xl:grid-cols-3
          "
        >

          {theatres.map((theatre) => (

            <div
              key={theatre.id}
              className="
                group
                rounded-2xl
                border border-white/10
                bg-white/[.03]
                p-5
                transition
                hover:border-cyan-300/20
                hover:bg-white/[.045]
              "
            >

              {/* HEADER */}

              <div
                className="
                  flex
                  items-start
                  justify-between
                  gap-3
                "
              >

                <div className="flex min-w-0 gap-3">

                  <div
                    className="
                      grid
                      h-11
                      w-11
                      shrink-0
                      place-items-center
                      rounded-xl
                      bg-cyan-300/10
                      text-cyan-300
                    "
                  >
                    <Building2 size={20} />
                  </div>


                  <div className="min-w-0">

                    <h2 className="truncate text-base font-semibold">
                      {theatre.name}
                    </h2>

                    <p className="mt-1 flex items-center gap-1 text-xs text-white/35">
                      <MapPin size={12} />

                      {theatre.location?.city ||
                        "Unknown city"}
                    </p>

                  </div>

                </div>


                <span
                  className="
                    shrink-0
                    rounded-full
                    bg-cyan-300/10
                    px-2.5
                    py-1
                    text-[10px]
                    font-medium
                    text-cyan-200
                  "
                >
                  #{theatre.id}
                </span>

              </div>


              {/* DETAILS */}

              <div
                className="
                  mt-5
                  grid
                  grid-cols-2
                  gap-2
                "
              >

                <div
                  className="
                    rounded-xl
                    bg-white/[.03]
                    p-3
                  "
                >

                  <div className="flex items-center gap-2 text-white/30">

                    <Users size={14} />

                    <span className="text-[10px] uppercase tracking-wider">
                      Seats
                    </span>

                  </div>

                  <p className="mt-2 text-sm font-semibold text-cyan-200">
                    {theatre.totalSeats}
                  </p>

                </div>


                <div
                  className="
                    rounded-xl
                    bg-white/[.03]
                    p-3
                  "
                >

                  <div className="flex items-center gap-2 text-white/30">

                    <MapPin size={14} />

                    <span className="text-[10px] uppercase tracking-wider">
                      City
                    </span>

                  </div>

                  <p className="mt-2 truncate text-sm font-semibold">
                    {theatre.location?.city ||
                      "—"}
                  </p>

                </div>

              </div>


              {/* ACTIONS */}

              <div
                className="
                  mt-5
                  grid
                  grid-cols-2
                  gap-2
                "
              >

                <button
                  type="button"
                  onClick={() =>
                    openEdit(theatre)
                  }
                  className="
                    flex
                    items-center
                    justify-center
                    gap-2
                    rounded-xl
                    border border-white/10
                    bg-white/[.03]
                    py-2.5
                    text-xs
                    text-white/55
                    transition
                    hover:border-cyan-300/20
                    hover:bg-cyan-300/[.05]
                    hover:text-cyan-200
                  "
                >

                  <Edit3 size={14} />

                  Edit

                </button>


                <button
                  type="button"
                  onClick={() =>
                    setDeleting(theatre)
                  }
                  className="
                    flex
                    items-center
                    justify-center
                    gap-2
                    rounded-xl
                    border border-red-400/10
                    bg-red-400/[.03]
                    py-2.5
                    text-xs
                    text-red-300/70
                    transition
                    hover:border-red-400/20
                    hover:bg-red-400/[.08]
                    hover:text-red-300
                  "
                >

                  <Trash2 size={14} />

                  Delete

                </button>

              </div>

            </div>

          ))}

        </div>
      )}


      {/* =================================================
          EDIT MODAL
      ================================================= */}

      {editing && (

        <div
          className="
            fixed
            inset-0
            z-[100]
            flex
            items-center
            justify-center
            bg-black/70
            p-5
            backdrop-blur-sm
          "
        >

          <div
            className="
              w-full
              max-w-lg
              rounded-2xl
              border border-white/10
              bg-[#0c1017]
              p-6
              shadow-2xl
            "
          >

            <div
              className="
                flex
                items-center
                justify-between
              "
            >

              <div>

                <p className="text-[10px] font-semibold uppercase tracking-[.25em] text-cyan-300">
                  Management
                </p>

                <h2 className="mt-2 text-xl font-semibold">
                  Edit Theatre
                </h2>

              </div>


              <button
                type="button"
                onClick={() =>
                  setEditing(null)
                }
                className="
                  grid
                  h-9
                  w-9
                  place-items-center
                  rounded-lg
                  text-white/35
                  hover:bg-white/[.05]
                  hover:text-white
                "
              >

                <X size={18} />

              </button>

            </div>


            <form
              onSubmit={saveEdit}
              className="mt-6 grid gap-5"
            >

              {/* NAME */}

              <label>

                <span className="mb-2 block text-xs text-white/45">
                  Theatre name
                </span>

                <input
                  value={editing.name}
                  onChange={(e) =>
                    setEditing({
                      ...editing,
                      name: e.target.value,
                    })
                  }
                  className="
                    h-12
                    w-full
                    rounded-xl
                    border border-white/10
                    bg-white/[.04]
                    px-4
                    text-sm
                    text-white
                    outline-none
                    placeholder:text-white/20
                    focus:border-cyan-300/40
                  "
                />

              </label>


              {/* CITY */}

              <label>

                <span className="mb-2 flex items-center gap-2 text-xs text-white/45">

                  <MapPin
                    size={14}
                    className="text-cyan-300"
                  />

                  City

                </span>

                <select
                  value={
                    editing.locationId
                  }
                  onChange={(e) =>
                    setEditing({
                      ...editing,
                      locationId:
                        e.target.value,
                    })
                  }
                  className="
                    h-12
                    w-full
                    rounded-xl
                    border border-white/10
                    bg-[#10151e]
                    px-4
                    text-sm
                    text-white
                    outline-none
                    focus:border-cyan-300/40
                  "
                >

                  <option value="">
                    Select city
                  </option>

                  {locations.map(
                    (location) => (
                      <option
                        key={location.id}
                        value={location.id}
                      >
                        {location.city}
                      </option>
                    )
                  )}

                </select>

              </label>


              {/* SEATS */}

              <label>

                <span className="mb-2 flex items-center gap-2 text-xs text-white/45">

                  <Users
                    size={14}
                    className="text-cyan-300"
                  />

                  Total seats

                </span>

                <input
                  type="number"
                  min="10"
                  max="2000"
                  value={
                    editing.totalSeats
                  }
                  onChange={(e) =>
                    setEditing({
                      ...editing,
                      totalSeats:
                        e.target.value,
                    })
                  }
                  className="
                    h-12
                    w-full
                    rounded-xl
                    border border-white/10
                    bg-white/[.04]
                    px-4
                    text-sm
                    text-white
                    outline-none
                    focus:border-cyan-300/40
                  "
                />

              </label>


              {/* WARNING */}

              <div
                className="
                  rounded-xl
                  border border-yellow-300/10
                  bg-yellow-300/[.04]
                  p-3
                  text-xs
                  leading-5
                  text-white/40
                "
              >
                Changing theatre capacity does not
                automatically recreate seats for existing
                shows.
              </div>


              {/* BUTTONS */}

              <div className="mt-2 grid grid-cols-2 gap-3">

                <button
                  type="button"
                  onClick={() =>
                    setEditing(null)
                  }
                  className="
                    rounded-xl
                    border border-white/10
                    bg-white/[.03]
                    py-3
                    text-sm
                    text-white/50
                    hover:bg-white/[.06]
                    hover:text-white
                  "
                >
                  Cancel
                </button>

                <button
                  type="submit"
                  disabled={saving}
                  className="
                    flex
                    items-center
                    justify-center
                    gap-2
                    rounded-xl
                    bg-cyan-300
                    py-3
                    text-sm
                    font-semibold
                    text-black
                    hover:bg-cyan-200
                    disabled:opacity-40
                  "
                >

                  {saving ? (
                    <>
                      <span
                        className="
                          h-4
                          w-4
                          animate-spin
                          rounded-full
                          border-2
                          border-black/30
                          border-t-black
                        "
                      />

                      Saving...
                    </>
                  ) : (
                    <>
                      <Edit3 size={15} />
                      Save changes
                    </>
                  )}

                </button>

              </div>

            </form>

          </div>

        </div>
      )}


      {/* =================================================
          DELETE MODAL
      ================================================= */}

      {deleting && (

        <div
          className="
            fixed
            inset-0
            z-[100]
            flex
            items-center
            justify-center
            bg-black/70
            p-5
            backdrop-blur-sm
          "
        >

          <div
            className="
              w-full
              max-w-md
              rounded-2xl
              border border-white/10
              bg-[#0c1017]
              p-6
              shadow-2xl
            "
          >

            <div
              className="
                mx-auto
                grid
                h-12
                w-12
                place-items-center
                rounded-xl
                bg-red-400/10
                text-red-300
              "
            >

              <Trash2 size={20} />

            </div>

            <h2 className="mt-5 text-center text-lg font-semibold">
              Delete theatre?
            </h2>

            <p className="mt-2 text-center text-sm leading-6 text-white/35">
              You are about to delete
              <span className="font-medium text-white/70">
                {" "}
                {deleting.name}
              </span>
              .
            </p>

            <p className="mt-3 text-center text-xs text-red-300/60">
              If this theatre has existing shows,
              the backend may reject the deletion.
            </p>


            <div className="mt-6 grid grid-cols-2 gap-3">

              <button
                type="button"
                onClick={() =>
                  setDeleting(null)
                }
                className="
                  rounded-xl
                  border border-white/10
                  bg-white/[.03]
                  py-3
                  text-sm
                  text-white/50
                  hover:bg-white/[.06]
                  hover:text-white
                "
              >
                Cancel
              </button>

              <button
                type="button"
                onClick={confirmDelete}
                disabled={saving}
                className="
                  flex
                  items-center
                  justify-center
                  gap-2
                  rounded-xl
                  bg-red-400
                  py-3
                  text-sm
                  font-semibold
                  text-black
                  hover:bg-red-300
                  disabled:opacity-40
                "
              >

                {saving ? (
                  "Deleting..."
                ) : (
                  <>
                    <Trash2 size={15} />
                    Delete
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