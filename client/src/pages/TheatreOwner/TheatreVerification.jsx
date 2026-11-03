import React, { useEffect, useMemo, useState } from "react";
import {
  getLocations,
  getMyTheatreVerification,
  submitTheatreVerification,
} from "../../lib/api";

const initialForm = {
  theatreName: "",
  ownerName: "",
  gstin: "",
  panNumber: "",
  registrationNumber: "",
  mobileNumber: "",
  address: "",
  city: "",
  state: "",
  pincode: "",
  numberOfScreens: "",
  totalSeats: "",
  theatreType: "",
  locationId: "",
  gstDocumentUrl: "",
  businessDocumentUrl: "",
  theatrePhotoUrl: "",
};

const theatreTypes = [
  "Multiplex",
  "Single Screen",
  "Premium Cinema",
  "Independent Cinema",
  "Drive-In Cinema",
  "Other",
];

/* =========================================================
   DARK CINEPASS DESIGN SYSTEM
========================================================= */

const inputClass =
  "w-full rounded-xl border border-white/10 bg-[#090c12] px-4 py-3 text-sm text-white outline-none transition duration-200 placeholder:text-white/25 focus:border-cyan-400/60 focus:bg-[#0b1018] focus:ring-4 focus:ring-cyan-400/10";

const labelClass =
  "mb-2 block text-sm font-semibold text-white/80";

const cardClass =
  "mb-6 rounded-3xl border border-white/[.08] bg-[#0d1119]/95 p-6 shadow-2xl shadow-black/20 backdrop-blur-xl md:p-8";

const sectionNumberClass =
  "flex h-10 w-10 shrink-0 items-center justify-center rounded-full bg-gradient-to-br from-cyan-300 to-blue-600 text-sm font-black text-slate-950 shadow-[0_0_25px_rgba(34,211,238,.18)]";

/* =========================================================
   INPUT FIELD
========================================================= */

function Field({
  label,
  name,
  value,
  onChange,
  placeholder,
  type = "text",
  required = true,
  maxLength,
}) {
  return (
    <div>
      <label htmlFor={name} className={labelClass}>
        {label}

        {required && (
          <span className="ml-1 text-cyan-400">*</span>
        )}
      </label>

      <input
        id={name}
        name={name}
        type={type}
        value={value}
        onChange={onChange}
        placeholder={placeholder}
        required={required}
        maxLength={maxLength}
        className={inputClass}
      />
    </div>
  );
}

/* =========================================================
   SECTION HEADER
========================================================= */

function SectionHeader({
  number,
  title,
  description,
}) {
  return (
    <div className="mb-7 flex items-start gap-4">
      <div className={sectionNumberClass}>
        {number}
      </div>

      <div>
        <h2 className="text-lg font-bold tracking-tight text-white md:text-xl">
          {title}
        </h2>

        <p className="mt-1 text-sm leading-6 text-white/40">
          {description}
        </p>
      </div>
    </div>
  );
}

/* =========================================================
   STATUS BADGE
========================================================= */

function StatusBadge({ status }) {
  const styles = {
    PENDING:
      "border-amber-400/20 bg-amber-400/10 text-amber-300",

    UNDER_REVIEW:
      "border-blue-400/20 bg-blue-400/10 text-blue-300",

    APPROVED:
      "border-emerald-400/20 bg-emerald-400/10 text-emerald-300",

    REJECTED:
      "border-red-400/20 bg-red-400/10 text-red-300",
  };

  return (
    <span
      className={`inline-flex rounded-full border px-3 py-1.5 text-[11px] font-bold uppercase tracking-wider ${
        styles[status] ||
        "border-white/10 bg-white/[.04] text-white/60"
      }`}
    >
      {status?.replaceAll("_", " ") || "UNKNOWN"}
    </span>
  );
}

/* =========================================================
   MAIN COMPONENT
========================================================= */

export default function TheatreVerification() {
  const [form, setForm] = useState(initialForm);

  const [locations, setLocations] = useState([]);

  const [verification, setVerification] = useState(null);

  const [loading, setLoading] = useState(true);

  const [locationsLoading, setLocationsLoading] =
    useState(true);

  const [submitting, setSubmitting] = useState(false);

  const [error, setError] = useState("");

  const [success, setSuccess] = useState("");

  const isApproved =
    verification?.status === "APPROVED";

  const isPending =
    verification?.status === "PENDING" ||
    verification?.status === "UNDER_REVIEW";

  const isRejected =
    verification?.status === "REJECTED";

  /* =======================================================
     LOAD PAGE
  ======================================================= */

  useEffect(() => {
    loadPage();
  }, []);

  async function loadPage() {
    setLoading(true);
    setLocationsLoading(true);
    setError("");

    try {
      const [locationData, verificationData] =
        await Promise.all([
          getLocations(),

          getMyTheatreVerification().catch((err) => {
            if (
              err?.status === 404 ||
              err?.response?.status === 404 ||
              String(err?.message || "").includes("404")
            ) {
              return null;
            }

            throw err;
          }),
        ]);

      setLocations(
        Array.isArray(locationData)
          ? locationData
          : []
      );

      setVerification(
        verificationData || null
      );

      if (verificationData) {
        setForm({
          theatreName:
            verificationData.theatreName || "",

          ownerName:
            verificationData.ownerName || "",

          gstin:
            verificationData.gstin || "",

          panNumber:
            verificationData.panNumber || "",

          registrationNumber:
            verificationData.registrationNumber || "",

          mobileNumber:
            verificationData.mobileNumber || "",

          address:
            verificationData.address || "",

          city:
            verificationData.city || "",

          state:
            verificationData.state || "",

          pincode:
            verificationData.pincode || "",

          numberOfScreens:
            verificationData.numberOfScreens || "",

          totalSeats:
            verificationData.totalSeats || "",

          theatreType:
            verificationData.theatreType || "",

          locationId:
            verificationData.locationId || "",

          gstDocumentUrl:
            verificationData.gstDocumentUrl || "",

          businessDocumentUrl:
            verificationData.businessDocumentUrl ||
            "",

          theatrePhotoUrl:
            verificationData.theatrePhotoUrl || "",
        });
      }
    } catch (err) {
      console.error(err);

      setError(
        err?.message ||
          "Unable to load theatre verification details. Please try again."
      );
    } finally {
      setLoading(false);
      setLocationsLoading(false);
    }
  }

  /* =======================================================
     HANDLE INPUT
  ======================================================= */

  function handleChange(event) {
    const { name, value } = event.target;

    let nextValue = value;

    if (
      name === "gstin" ||
      name === "panNumber"
    ) {
      nextValue = value.toUpperCase();
    }

    if (
      name === "mobileNumber" ||
      name === "pincode"
    ) {
      nextValue = value.replace(/\D/g, "");
    }

    if (
      name === "numberOfScreens" ||
      name === "totalSeats"
    ) {
      nextValue = value.replace(/\D/g, "");
    }

    setForm((previous) => ({
      ...previous,
      [name]: nextValue,
    }));
  }

  /* =======================================================
     SELECTED LOCATION
  ======================================================= */

  const selectedLocation = useMemo(() => {
    return locations.find(
      (location) =>
        String(location.id) ===
        String(form.locationId)
    );
  }, [locations, form.locationId]);

  /* =======================================================
     VALIDATION
  ======================================================= */

  function validateForm() {
    const gstinRegex =
      /^[0-9]{2}[A-Z]{5}[0-9]{4}[A-Z][1-9A-Z]Z[0-9A-Z]$/;

    const panRegex =
      /^[A-Z]{5}[0-9]{4}[A-Z]$/;

    const mobileRegex =
      /^[6-9][0-9]{9}$/;

    const pincodeRegex =
      /^[0-9]{6}$/;

    if (!form.theatreName.trim()) {
      return "Please enter the theatre name.";
    }

    if (!form.ownerName.trim()) {
      return "Please enter the owner name.";
    }

    if (
      !gstinRegex.test(
        form.gstin.trim().toUpperCase()
      )
    ) {
      return "Please enter a valid GSTIN.";
    }

    if (
      !panRegex.test(
        form.panNumber.trim().toUpperCase()
      )
    ) {
      return "Please enter a valid PAN number.";
    }

    if (!form.registrationNumber.trim()) {
      return "Please enter the business/registration number.";
    }

    if (!mobileRegex.test(form.mobileNumber)) {
      return "Please enter a valid 10-digit mobile number.";
    }

    if (!form.address.trim()) {
      return "Please enter the complete theatre address.";
    }

    if (!form.city.trim()) {
      return "Please enter the city.";
    }

    if (!form.state.trim()) {
      return "Please enter the state.";
    }

    if (!pincodeRegex.test(form.pincode)) {
      return "Please enter a valid 6-digit pincode.";
    }

    if (!form.numberOfScreens) {
      return "Please enter the number of screens.";
    }

    if (
      Number(form.numberOfScreens) < 1 ||
      Number(form.numberOfScreens) > 100
    ) {
      return "Number of screens must be between 1 and 100.";
    }

    if (!form.totalSeats) {
      return "Please enter the total number of seats.";
    }

    if (
      Number(form.totalSeats) < 10 ||
      Number(form.totalSeats) > 20000
    ) {
      return "Total seats must be between 10 and 20,000.";
    }

    if (!form.theatreType) {
      return "Please select the theatre type.";
    }

    if (!form.locationId) {
      return "Please select a platform location.";
    }

    return "";
  }

  /* =======================================================
     SUBMIT
  ======================================================= */

  async function handleSubmit(event) {
    event.preventDefault();

    setError("");
    setSuccess("");

    const validationError =
      validateForm();

    if (validationError) {
      setError(validationError);

      window.scrollTo({
        top: 0,
        behavior: "smooth",
      });

      return;
    }

    setSubmitting(true);

    try {
      const payload = {
        theatreName:
          form.theatreName.trim(),

        ownerName:
          form.ownerName.trim(),

        gstin:
          form.gstin.trim().toUpperCase(),

        panNumber:
          form.panNumber.trim().toUpperCase(),

        registrationNumber:
          form.registrationNumber.trim(),

        mobileNumber:
          form.mobileNumber.trim(),

        address:
          form.address.trim(),

        city:
          form.city.trim(),

        state:
          form.state.trim(),

        pincode:
          form.pincode.trim(),

        numberOfScreens:
          Number(form.numberOfScreens),

        totalSeats:
          Number(form.totalSeats),

        theatreType:
          form.theatreType,

        locationId:
          Number(form.locationId),

        gstDocumentUrl:
          form.gstDocumentUrl.trim() || null,

        businessDocumentUrl:
          form.businessDocumentUrl.trim() || null,

        theatrePhotoUrl:
          form.theatrePhotoUrl.trim() || null,
      };

      const result =
        await submitTheatreVerification(
          payload
        );

      setVerification(result);

      setSuccess(
        "Your theatre verification application has been submitted successfully."
      );

      window.scrollTo({
        top: 0,
        behavior: "smooth",
      });
    } catch (err) {
      console.error(err);

      setError(
        err?.message ||
          "Unable to submit your verification application. Please try again."
      );

      window.scrollTo({
        top: 0,
        behavior: "smooth",
      });
    } finally {
      setSubmitting(false);
    }
  }

  /* =======================================================
     LOADING
  ======================================================= */

  if (loading) {
    return (
      <div className="min-h-screen bg-[#05070b] pt-28 text-white">
        <div className="flex min-h-[65vh] items-center justify-center">
          <div className="text-center">
            <div className="mx-auto h-10 w-10 animate-spin rounded-full border-4 border-white/10 border-t-cyan-400" />

            <p className="mt-5 text-sm font-medium text-white/40">
              Loading verification details...
            </p>
          </div>
        </div>
      </div>
    );
  }

  /* =======================================================
     PAGE
  ======================================================= */

  return (
    <div className="min-h-screen bg-[#05070b] px-4 pb-16 pt-28 text-white md:px-8 lg:px-12">

      {/* Background glow */}
      <div className="pointer-events-none fixed inset-0 -z-0 overflow-hidden">
        <div className="absolute left-[10%] top-[10%] h-72 w-72 rounded-full bg-cyan-400/[.035] blur-3xl" />

        <div className="absolute right-[5%] top-[35%] h-96 w-96 rounded-full bg-blue-600/[.025] blur-3xl" />
      </div>

      <div className="relative z-10 mx-auto max-w-6xl">

        {/* =================================================
            PAGE HEADER
        ================================================= */}

        <div className="mb-8">

          <div className="mb-4 inline-flex items-center rounded-full border border-cyan-400/20 bg-cyan-400/[.06] px-3 py-1.5 text-[10px] font-bold uppercase tracking-[.18em] text-cyan-300">
            Theatre Owner
          </div>

          <h1 className="text-3xl font-black tracking-tight text-white md:text-4xl lg:text-5xl">
            Theatre Verification
          </h1>

          <p className="mt-3 max-w-3xl text-sm leading-6 text-white/45 md:text-base">
            Complete your theatre business profile to
            request approval. Once approved by the
            platform administrator, you can manage your
            theatre, shows, bookings and business
            analytics.
          </p>
        </div>

        {/* =================================================
            STATUS CARD
        ================================================= */}

        {verification && (
          <div className="mb-6 overflow-hidden rounded-3xl border border-white/[.08] bg-[#0d1119]/95 shadow-2xl shadow-black/20 backdrop-blur-xl">

            <div className="flex flex-col gap-5 p-5 md:flex-row md:items-center md:justify-between md:p-6">

              <div>
                <p className="text-[10px] font-bold uppercase tracking-[.18em] text-white/30">
                  Application Status
                </p>

                <div className="mt-3 flex flex-wrap items-center gap-3">
                  <StatusBadge
                    status={verification.status}
                  />

                  {verification.theatreName && (
                    <span className="text-sm font-semibold text-white/80">
                      {verification.theatreName}
                    </span>
                  )}
                </div>
              </div>

              {verification.submittedAt && (
                <div className="text-sm text-white/35">
                  Submitted{" "}
                  <span className="font-semibold text-white/65">
                    {new Date(
                      verification.submittedAt
                    ).toLocaleDateString()}
                  </span>
                </div>
              )}
            </div>

            {/* Pending */}
            {isPending && (
              <div className="border-t border-amber-400/10 bg-amber-400/[.04] px-5 py-4 md:px-6">
                <p className="text-sm font-semibold text-amber-300">
                  Your application is waiting for
                  administrator review.
                </p>

                <p className="mt-1 text-sm text-amber-200/50">
                  You will receive access to the theatre
                  owner dashboard after approval.
                </p>
              </div>
            )}

            {/* Approved */}
            {isApproved && (
              <div className="border-t border-emerald-400/10 bg-emerald-400/[.04] px-5 py-4 md:px-6">
                <p className="text-sm font-semibold text-emerald-300">
                  Your theatre has been approved.
                </p>

                <p className="mt-1 text-sm text-emerald-200/50">
                  Your owner dashboard can now be activated.
                </p>
              </div>
            )}

            {/* Rejected */}
            {isRejected && (
              <div className="border-t border-red-400/10 bg-red-400/[.04] px-5 py-4 md:px-6">
                <p className="text-sm font-semibold text-red-300">
                  Your application needs changes.
                </p>

                {verification.rejectionReason && (
                  <p className="mt-2 text-sm leading-6 text-red-200/60">
                    <span className="font-semibold text-red-300">
                      Reason:
                    </span>{" "}
                    {verification.rejectionReason}
                  </p>
                )}

                <p className="mt-2 text-sm text-red-200/50">
                  Update the details below and submit
                  the application again.
                </p>
              </div>
            )}
          </div>
        )}

        {/* =================================================
            ERROR
        ================================================= */}

        {error && (
          <div className="mb-6 rounded-2xl border border-red-400/20 bg-red-400/[.06] px-5 py-4">
            <p className="text-sm font-semibold text-red-300">
              {error}
            </p>
          </div>
        )}

        {/* =================================================
            SUCCESS
        ================================================= */}

        {success && (
          <div className="mb-6 rounded-2xl border border-emerald-400/20 bg-emerald-400/[.06] px-5 py-4">
            <p className="text-sm font-semibold text-emerald-300">
              {success}
            </p>
          </div>
        )}

        {/* =================================================
            APPROVED
        ================================================= */}

        {isApproved ? (
          <div className="rounded-3xl border border-emerald-400/15 bg-[#0d1119]/95 p-8 text-center shadow-2xl shadow-black/20 backdrop-blur-xl md:p-14">

            <div className="mx-auto flex h-20 w-20 items-center justify-center rounded-full border border-emerald-400/20 bg-emerald-400/10 shadow-[0_0_35px_rgba(52,211,153,.12)]">
              <span className="text-2xl font-black text-emerald-300">
                ✓
              </span>
            </div>

            <h2 className="mt-7 text-2xl font-black text-white md:text-3xl">
              Theatre Approved
            </h2>

            <p className="mx-auto mt-3 max-w-xl text-sm leading-6 text-white/45">
              Your theatre verification has been
              approved by the administrator. Your
              theatre owner dashboard can now be used
              for theatre operations.
            </p>

            <button
              type="button"
              onClick={() =>
                (window.location.href = "/owner")
              }
              className="mt-8 rounded-xl bg-cyan-400 px-7 py-3 text-sm font-bold text-slate-950 shadow-[0_0_25px_rgba(34,211,238,.12)] transition hover:bg-cyan-300 hover:shadow-[0_0_30px_rgba(34,211,238,.2)]"
            >
              Open Owner Dashboard
            </button>
          </div>
        ) : (

          <form onSubmit={handleSubmit}>

            {/* =================================================
                01 BUSINESS INFORMATION
            ================================================= */}

            <div className={cardClass}>

              <SectionHeader
                number="01"
                title="Business Information"
                description="Provide the official details of your theatre business."
              />

              <div className="grid gap-5 md:grid-cols-2">

                <Field
                  label="Theatre Name"
                  name="theatreName"
                  value={form.theatreName}
                  onChange={handleChange}
                  placeholder="Example: SRH Cinemas"
                />

                <Field
                  label="Owner Name"
                  name="ownerName"
                  value={form.ownerName}
                  onChange={handleChange}
                  placeholder="Full legal name"
                />

                <Field
                  label="GSTIN"
                  name="gstin"
                  value={form.gstin}
                  onChange={handleChange}
                  placeholder="22AAAAA0000A1Z5"
                  maxLength={15}
                />

                <Field
                  label="PAN Number"
                  name="panNumber"
                  value={form.panNumber}
                  onChange={handleChange}
                  placeholder="ABCDE1234F"
                  maxLength={10}
                />

                <Field
                  label="Business / Registration Number"
                  name="registrationNumber"
                  value={form.registrationNumber}
                  onChange={handleChange}
                  placeholder="Business registration number"
                />

                <Field
                  label="Mobile Number"
                  name="mobileNumber"
                  value={form.mobileNumber}
                  onChange={handleChange}
                  placeholder="10-digit mobile number"
                  type="tel"
                  maxLength={10}
                />

              </div>
            </div>

            {/* =================================================
                02 BUSINESS ADDRESS
            ================================================= */}

            <div className={cardClass}>

              <SectionHeader
                number="02"
                title="Business Address"
                description="Enter the complete registered theatre location."
              />

              <div className="grid gap-5 md:grid-cols-2">

                <div className="md:col-span-2">
                  <label
                    htmlFor="address"
                    className={labelClass}
                  >
                    Complete Address
                    <span className="ml-1 text-cyan-400">
                      *
                    </span>
                  </label>

                  <textarea
                    id="address"
                    name="address"
                    value={form.address}
                    onChange={handleChange}
                    rows={4}
                    placeholder="Building number, street, area, landmark..."
                    className={`${inputClass} resize-none`}
                  />
                </div>

                <Field
                  label="City"
                  name="city"
                  value={form.city}
                  onChange={handleChange}
                  placeholder="Example: Bengaluru"
                />

                <Field
                  label="State"
                  name="state"
                  value={form.state}
                  onChange={handleChange}
                  placeholder="Example: Karnataka"
                />

                <Field
                  label="Pincode"
                  name="pincode"
                  value={form.pincode}
                  onChange={handleChange}
                  placeholder="6-digit pincode"
                  maxLength={6}
                />

                <div>
                  <label
                    htmlFor="locationId"
                    className={labelClass}
                  >
                    Platform Location
                    <span className="ml-1 text-cyan-400">
                      *
                    </span>
                  </label>

                  <select
                    id="locationId"
                    name="locationId"
                    value={form.locationId}
                    onChange={handleChange}
                    disabled={locationsLoading}
                    className={`${inputClass} cursor-pointer`}
                  >
                    <option
                      value=""
                      className="bg-[#0d1119] text-white"
                    >
                      {locationsLoading
                        ? "Loading locations..."
                        : "Select location"}
                    </option>

                    {locations.map((location) => (
                      <option
                        key={location.id}
                        value={location.id}
                        className="bg-[#0d1119] text-white"
                      >
                        {location.city}
                        {location.state
                          ? `, ${location.state}`
                          : ""}
                      </option>
                    ))}
                  </select>

                  {selectedLocation && (
                    <p className="mt-2 text-xs text-white/30">
                      Selected platform location:{" "}
                      <span className="text-cyan-300/70">
                        {selectedLocation.city}
                      </span>
                    </p>
                  )}
                </div>

              </div>
            </div>

            {/* =================================================
                03 THEATRE DETAILS
            ================================================= */}

            <div className={cardClass}>

              <SectionHeader
                number="03"
                title="Theatre Details"
                description="Tell us about your cinema infrastructure."
              />

              <div className="grid gap-5 md:grid-cols-3">

                <Field
                  label="Number of Screens"
                  name="numberOfScreens"
                  value={form.numberOfScreens}
                  onChange={handleChange}
                  placeholder="Example: 5"
                  type="number"
                />

                <Field
                  label="Total Seats"
                  name="totalSeats"
                  value={form.totalSeats}
                  onChange={handleChange}
                  placeholder="Example: 800"
                  type="number"
                />

                <div>
                  <label
                    htmlFor="theatreType"
                    className={labelClass}
                  >
                    Theatre Type
                    <span className="ml-1 text-cyan-400">
                      *
                    </span>
                  </label>

                  <select
                    id="theatreType"
                    name="theatreType"
                    value={form.theatreType}
                    onChange={handleChange}
                    className={`${inputClass} cursor-pointer`}
                  >
                    <option
                      value=""
                      className="bg-[#0d1119] text-white"
                    >
                      Select theatre type
                    </option>

                    {theatreTypes.map((type) => (
                      <option
                        key={type}
                        value={type}
                        className="bg-[#0d1119] text-white"
                      >
                        {type}
                      </option>
                    ))}
                  </select>
                </div>

              </div>

              {/* Stats */}
              <div className="mt-6 grid gap-4 md:grid-cols-3">

                <div className="rounded-2xl border border-white/[.06] bg-white/[.025] p-5 transition hover:border-cyan-400/20">
                  <p className="text-[10px] font-bold uppercase tracking-[.16em] text-white/30">
                    Screens
                  </p>

                  <p className="mt-2 text-2xl font-black text-white">
                    {form.numberOfScreens || "—"}
                  </p>
                </div>

                <div className="rounded-2xl border border-white/[.06] bg-white/[.025] p-5 transition hover:border-cyan-400/20">
                  <p className="text-[10px] font-bold uppercase tracking-[.16em] text-white/30">
                    Total Seats
                  </p>

                  <p className="mt-2 text-2xl font-black text-white">
                    {form.totalSeats || "—"}
                  </p>
                </div>

                <div className="rounded-2xl border border-white/[.06] bg-white/[.025] p-5 transition hover:border-cyan-400/20">
                  <p className="text-[10px] font-bold uppercase tracking-[.16em] text-white/30">
                    Theatre Type
                  </p>

                  <p className="mt-2 truncate text-lg font-black text-white">
                    {form.theatreType || "—"}
                  </p>
                </div>

              </div>
            </div>

            {/* =================================================
                04 BUSINESS DOCUMENTS
            ================================================= */}

            <div className={cardClass}>

              <SectionHeader
                number="04"
                title="Business Documents"
                description="Provide document references for administrator review."
              />

              <div className="mb-6 rounded-2xl border border-cyan-400/10 bg-cyan-400/[.035] p-5">

                <p className="text-sm font-semibold text-cyan-300">
                  Document upload
                </p>

                <p className="mt-2 text-xs leading-5 text-white/40">
                  The current backend stores document URLs.
                  Actual file upload and cloud storage can
                  be connected later.
                </p>

              </div>

              <div className="grid gap-5">

                <Field
                  label="GST Document URL"
                  name="gstDocumentUrl"
                  value={form.gstDocumentUrl}
                  onChange={handleChange}
                  placeholder="https://..."
                  required={false}
                  type="url"
                />

                <Field
                  label="Business Registration Document URL"
                  name="businessDocumentUrl"
                  value={form.businessDocumentUrl}
                  onChange={handleChange}
                  placeholder="https://..."
                  required={false}
                  type="url"
                />

                <Field
                  label="Theatre Photo URL"
                  name="theatrePhotoUrl"
                  value={form.theatrePhotoUrl}
                  onChange={handleChange}
                  placeholder="https://..."
                  required={false}
                  type="url"
                />

              </div>
            </div>

            {/* =================================================
                05 REVIEW & SUBMIT
            ================================================= */}

            <div className={cardClass}>

              <SectionHeader
                number="05"
                title="Review & Submit"
                description="Check your information before sending the application."
              />

              <div className="grid gap-3 md:grid-cols-2">

                <div className="rounded-2xl border border-white/[.06] bg-white/[.025] p-4">
                  <p className="text-[10px] font-bold uppercase tracking-wider text-white/25">
                    Theatre
                  </p>

                  <p className="mt-2 font-bold text-white/80">
                    {form.theatreName ||
                      "Not provided"}
                  </p>
                </div>

                <div className="rounded-2xl border border-white/[.06] bg-white/[.025] p-4">
                  <p className="text-[10px] font-bold uppercase tracking-wider text-white/25">
                    Owner
                  </p>

                  <p className="mt-2 font-bold text-white/80">
                    {form.ownerName ||
                      "Not provided"}
                  </p>
                </div>

                <div className="rounded-2xl border border-white/[.06] bg-white/[.025] p-4">
                  <p className="text-[10px] font-bold uppercase tracking-wider text-white/25">
                    Location
                  </p>

                  <p className="mt-2 font-bold text-white/80">
                    {selectedLocation
                      ? `${selectedLocation.city}${
                          selectedLocation.state
                            ? `, ${selectedLocation.state}`
                            : ""
                        }`
                      : "Not selected"}
                  </p>
                </div>

                <div className="rounded-2xl border border-white/[.06] bg-white/[.025] p-4">
                  <p className="text-[10px] font-bold uppercase tracking-wider text-white/25">
                    Infrastructure
                  </p>

                  <p className="mt-2 font-bold text-white/80">
                    {form.numberOfScreens || 0}{" "}
                    screens ·{" "}
                    {form.totalSeats || 0} seats
                  </p>
                </div>

              </div>

              {/* Confirmation */}
              <div className="mt-6 flex items-start gap-3 rounded-2xl border border-white/[.06] bg-white/[.025] p-4">

                <input
                  id="confirmDetails"
                  type="checkbox"
                  required
                  className="mt-1 h-4 w-4 cursor-pointer rounded border-white/20 bg-[#090c12] text-cyan-400 accent-cyan-400 focus:ring-cyan-400"
                />

                <label
                  htmlFor="confirmDetails"
                  className="cursor-pointer text-sm leading-6 text-white/50"
                >
                  I confirm that the information
                  provided above is accurate and belongs
                  to my theatre business. I understand
                  that the administrator will review the
                  application before approval.
                </label>

              </div>

              {/* Buttons */}
              <div className="mt-7 flex flex-col gap-3 sm:flex-row sm:justify-end">

                <button
                  type="button"
                  onClick={() => {
                    setForm(initialForm);
                    setError("");
                    setSuccess("");
                  }}
                  disabled={
                    submitting || isPending
                  }
                  className="rounded-xl border border-white/10 bg-white/[.03] px-6 py-3 text-sm font-bold text-white/65 transition hover:border-white/20 hover:bg-white/[.06] hover:text-white disabled:cursor-not-allowed disabled:opacity-40"
                >
                  Clear Form
                </button>

                <button
                  type="submit"
                  disabled={
                    submitting || isPending
                  }
                  className="rounded-xl bg-cyan-400 px-7 py-3 text-sm font-bold text-slate-950 shadow-[0_0_25px_rgba(34,211,238,.12)] transition hover:bg-cyan-300 hover:shadow-[0_0_35px_rgba(34,211,238,.2)] disabled:cursor-not-allowed disabled:opacity-40"
                >
                  {submitting
                    ? "Submitting..."
                    : isRejected
                    ? "Resubmit Application"
                    : "Submit for Verification"}
                </button>

              </div>
            </div>

            {/* =================================================
                BOTTOM NOTE
            ================================================= */}

            <div className="pb-8 text-center">

              <p className="text-xs leading-5 text-white/25">
                Your application will remain pending
                until it is reviewed by an administrator.
              </p>

            </div>

          </form>
        )}
      </div>
    </div>
  );
}