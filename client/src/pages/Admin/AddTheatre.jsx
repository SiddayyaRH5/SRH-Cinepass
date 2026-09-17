import React from "react";
import {
  Building2,
  CheckCircle2,
  MapPin,
  Plus,
  Users,
} from "lucide-react";
import toast from "react-hot-toast";

import Title from "../../components/Admin/Title";
import {
  getLocations,
  createTheatre,
} from "../../lib/api";

export default function AddTheatre() {

  const [locations, setLocations] = React.useState([]);

  const [name, setName] = React.useState("");
  const [locationId, setLocationId] = React.useState("");
  const [totalSeats, setTotalSeats] = React.useState("");

  const [saving, setSaving] = React.useState(false);

  React.useEffect(() => {

    getLocations()
      .then((data) => {
        setLocations(
          Array.isArray(data) ? data : []
        );
      })
      .catch((error) => {
        toast.error(
          error.message || "Unable to load cities"
        );
      });

  }, []);

  const submit = async (e) => {

    e.preventDefault();

    const trimmedName = name.trim();
    const seats = Number(totalSeats);

    if (!trimmedName) {
      return toast.error(
        "Enter the theatre name"
      );
    }

    if (!locationId) {
      return toast.error(
        "Select a city"
      );
    }

    if (!Number.isInteger(seats) || seats < 10) {
      return toast.error(
        "Theatre must have at least 10 seats"
      );
    }

    if (seats > 2000) {
      return toast.error(
        "Maximum theatre capacity is 2000 seats"
      );
    }

    try {

      setSaving(true);

      await createTheatre({
        name: trimmedName,
        totalSeats: seats,
        location: {
          id: Number(locationId),
        },
      });

      toast.success(
        "Theatre created successfully"
      );

      setName("");
      setTotalSeats("");

    } catch (error) {

      toast.error(
        error.message ||
        "Could not create theatre"
      );

    } finally {

      setSaving(false);
    }
  };

  return (
    <>
      <Title
        text1="Management"
        text2="Add Theatre"
      />

      <div className="mt-8 grid gap-6 lg:grid-cols-[1fr_330px]">

        {/* FORM */}

        <form
          onSubmit={submit}
          className="
            rounded-2xl
            border border-white/10
            bg-white/[.03]
            p-6
            sm:p-8
          "
        >

          <div className="mb-7">

            <h2 className="text-xl font-semibold">
              Add a theatre
            </h2>

            <p className="mt-1 text-sm text-white/35">
              Add a cinema location and define its seating capacity.
            </p>

          </div>

          <div className="grid gap-5">

            {/* THEATRE NAME */}

            <Field
              label="Theatre name"
              icon={Building2}
            >

              <input
                value={name}
                onChange={(e) =>
                  setName(e.target.value)
                }
                placeholder="e.g. PVR IMAX"
                maxLength={100}
                required
              />

            </Field>


            {/* CITY */}

            <Field
              label="City"
              icon={MapPin}
            >

              <select
                value={locationId}
                onChange={(e) =>
                  setLocationId(e.target.value)
                }
                required
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

            </Field>


            {/* SEATS */}

            <Field
              label="Total seats"
              icon={Users}
            >

              <input
                type="number"
                min="10"
                max="2000"
                step="1"
                value={totalSeats}
                onChange={(e) =>
                  setTotalSeats(e.target.value)
                }
                placeholder="e.g. 200"
                required
              />

            </Field>

          </div>


          {/* CREATE */}

          <button
            disabled={saving}
            className="
              mt-7
              flex
              w-full
              items-center
              justify-center
              gap-2
              rounded-xl
              bg-cyan-300
              py-3.5
              text-sm
              font-semibold
              text-black
              transition
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

                Creating...

              </>
            ) : (
              <>
                <Plus size={17} />
                Create theatre
              </>
            )}

          </button>

        </form>


        {/* INFO */}

        <div
          className="
            rounded-2xl
            border border-white/10
            bg-gradient-to-br
            from-cyan-400/10
            to-white/[.02]
            p-6
          "
        >

          <CheckCircle2
            className="text-cyan-300"
          />

          <h3 className="mt-5 font-semibold">
            Theatre seat capacity
          </h3>

          <p className="mt-2 text-sm leading-6 text-white/40">
            The number of seats you enter here becomes
            the default capacity for every new show
            created in this theatre.
          </p>

          <div className="mt-6 space-y-3">

            <Info
              icon={Users}
              label="Minimum"
              value="10 seats"
            />

            <Info
              icon={Users}
              label="Maximum"
              value="2,000 seats"
            />

            <Info
              icon={Building2}
              label="Duplicate theatres"
              value="Blocked within the same city"
            />

          </div>

        </div>

      </div>
    </>
  );
}


function Field({
  label,
  icon: Icon,
  children,
}) {

  return (
    <label className="block">

      <span
        className="
          mb-2
          flex
          items-center
          gap-2
          text-xs
          text-white/45
        "
      >

        <Icon
          size={14}
          className="text-cyan-300"
        />

        {label}

      </span>

      <div
        className="
          [&_input]:h-12
          [&_input]:w-full
          [&_input]:rounded-xl
          [&_input]:border
          [&_input]:border-white/10
          [&_input]:bg-white/[.04]
          [&_input]:px-4
          [&_input]:text-sm
          [&_input]:text-white
          [&_input]:outline-none
          [&_input]:placeholder:text-white/20
          [&_input]:focus:border-cyan-300/40

          [&_select]:h-12
          [&_select]:w-full
          [&_select]:rounded-xl
          [&_select]:border
          [&_select]:border-white/10
          [&_select]:bg-[#10151e]
          [&_select]:px-4
          [&_select]:text-sm
          [&_select]:text-white
          [&_select]:outline-none
          [&_select]:focus:border-cyan-300/40
        "
      >

        {children}

      </div>

    </label>
  );
}


function Info({
  icon: Icon,
  label,
  value,
}) {

  return (
    <div
      className="
        flex
        items-center
        justify-between
        rounded-xl
        bg-white/[.03]
        px-4
        py-3
      "
    >

      <span className="flex items-center gap-2 text-xs text-white/40">

        <Icon
          size={14}
          className="text-cyan-300"
        />

        {label}

      </span>

      <span className="text-xs text-white/70">
        {value}
      </span>

    </div>
  );
}