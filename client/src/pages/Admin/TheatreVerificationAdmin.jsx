import React, { useEffect, useState } from "react";
import {
  Building2,
  CheckCircle2,
  XCircle,
  MapPin,
  User,
  Phone,
  FileText,
  ExternalLink,
  Clock3,
  Users,
  Monitor,
  AlertTriangle,
  ShieldCheck,
  X,
  Loader2,
} from "lucide-react";

import {
  getPendingTheatreVerifications,
  approveTheatreVerification,
  rejectTheatreVerification,
} from "../../lib/api";

export default function TheatreVerificationAdmin() {
  const [applications, setApplications] = useState([]);
  const [loading, setLoading] = useState(true);

  // Custom modal state
  const [modal, setModal] = useState({
    open: false,
    type: null,
    application: null,
  });

  const [rejectionReason, setRejectionReason] = useState("");
  const [processing, setProcessing] = useState(false);

  useEffect(() => {
    loadApplications();
  }, []);

  const loadApplications = async () => {
    try {
      setLoading(true);

      const data = await getPendingTheatreVerifications();

      setApplications(Array.isArray(data) ? data : []);
    } catch (error) {
      console.error("Failed to load theatre applications:", error);
      alert("Failed to load theatre applications.");
    } finally {
      setLoading(false);
    }
  };

  // Open approve modal
  const openApproveModal = (application) => {
    setModal({
      open: true,
      type: "approve",
      application,
    });
  };

  // Open reject modal
  const openRejectModal = (application) => {
    setRejectionReason("");

    setModal({
      open: true,
      type: "reject",
      application,
    });
  };

  // Close modal
  const closeModal = () => {
    if (processing) return;

    setModal({
      open: false,
      type: null,
      application: null,
    });

    setRejectionReason("");
  };

  // Approve theatre
  const handleApprove = async () => {
    if (!modal.application) return;

    try {
      setProcessing(true);

      await approveTheatreVerification(modal.application.id);

      setApplications((prev) =>
        prev.filter((item) => item.id !== modal.application.id)
      );

      closeModal();

      alert("Theatre approved successfully.");
    } catch (error) {
      console.error("Approval failed:", error);
      alert(error?.message || "Failed to approve theatre.");
    } finally {
      setProcessing(false);
    }
  };

  // Reject theatre
  const handleReject = async () => {
    if (!modal.application) return;

    if (!rejectionReason.trim()) {
      return;
    }

    try {
      setProcessing(true);

      await rejectTheatreVerification(
        modal.application.id,
        rejectionReason.trim()
      );

      setApplications((prev) =>
        prev.filter((item) => item.id !== modal.application.id)
      );

      closeModal();

      alert("Theatre application rejected.");
    } catch (error) {
      console.error("Rejection failed:", error);
      alert(error?.message || "Failed to reject theatre.");
    } finally {
      setProcessing(false);
    }
  };

  return (
    <div className="min-h-screen bg-[#05070b] px-4 pb-16 pt-28 text-white sm:px-6 lg:px-10">

      {/* Background glow */}
      <div className="pointer-events-none fixed inset-0 overflow-hidden">
        <div className="absolute -left-32 top-20 h-80 w-80 rounded-full bg-cyan-500/[0.04] blur-3xl" />
        <div className="absolute right-0 top-96 h-96 w-96 rounded-full bg-blue-500/[0.04] blur-3xl" />
      </div>

      <div className="relative mx-auto max-w-7xl">

        {/* Header */}
        <div className="mb-10">
          <div className="mb-4 flex items-center gap-3">
            <div className="flex h-12 w-12 items-center justify-center rounded-2xl border border-cyan-400/20 bg-cyan-400/[0.08]">
              <ShieldCheck
                size={25}
                className="text-cyan-400"
              />
            </div>

            <div>
              <p className="text-xs font-semibold uppercase tracking-[0.2em] text-cyan-400/80">
                Administration
              </p>

              <h1 className="text-2xl font-bold tracking-tight sm:text-3xl">
                Theatre Verification
              </h1>
            </div>
          </div>

          <p className="max-w-2xl text-sm leading-6 text-white/45">
            Review theatre owner applications, verify submitted business
            information, and approve or reject theatre registrations.
          </p>
        </div>

        {/* Stats */}
        <div className="mb-8 grid grid-cols-1 gap-4 sm:grid-cols-3">

          {/* Pending */}
          <div className="rounded-2xl border border-white/[0.07] bg-[#0d1119] p-5">
            <div className="flex items-center justify-between">
              <div>
                <p className="text-xs font-medium uppercase tracking-wider text-white/40">
                  Pending Applications
                </p>

                <p className="mt-2 text-3xl font-bold text-white">
                  {applications.length}
                </p>
              </div>

              <div className="flex h-11 w-11 items-center justify-center rounded-xl bg-amber-400/10">
                <Clock3
                  size={21}
                  className="text-amber-400"
                />
              </div>
            </div>
          </div>

          {/* Status */}
          <div className="rounded-2xl border border-white/[0.07] bg-[#0d1119] p-5">
            <div className="flex items-center justify-between">
              <div>
                <p className="text-xs font-medium uppercase tracking-wider text-white/40">
                  Review Status
                </p>

                <p className="mt-2 text-lg font-bold text-amber-400">
                  Requires Review
                </p>
              </div>

              <div className="flex h-11 w-11 items-center justify-center rounded-xl bg-amber-400/10">
                <AlertTriangle
                  size={21}
                  className="text-amber-400"
                />
              </div>
            </div>
          </div>

          {/* Platform */}
          <div className="rounded-2xl border border-white/[0.07] bg-[#0d1119] p-5">
            <div className="flex items-center justify-between">
              <div>
                <p className="text-xs font-medium uppercase tracking-wider text-white/40">
                  Platform
                </p>

                <p className="mt-2 text-lg font-bold text-white">
                  SRH CinePass
                </p>
              </div>

              <div className="flex h-11 w-11 items-center justify-center rounded-xl bg-cyan-400/10">
                <Building2
                  size={21}
                  className="text-cyan-400"
                />
              </div>
            </div>
          </div>
        </div>

        {/* Loading */}
        {loading && (
          <div className="flex min-h-[300px] items-center justify-center">
            <div className="flex items-center gap-3 text-white/50">
              <Loader2
                size={20}
                className="animate-spin text-cyan-400"
              />

              <span>Loading applications...</span>
            </div>
          </div>
        )}

        {/* Empty */}
        {!loading && applications.length === 0 && (
          <div className="rounded-3xl border border-white/[0.07] bg-[#0d1119] px-6 py-16 text-center">

            <div className="mx-auto mb-5 flex h-16 w-16 items-center justify-center rounded-2xl bg-emerald-400/10">
              <CheckCircle2
                size={30}
                className="text-emerald-400"
              />
            </div>

            <h2 className="text-xl font-semibold">
              No Pending Applications
            </h2>

            <p className="mx-auto mt-2 max-w-md text-sm leading-6 text-white/40">
              All theatre verification applications have been reviewed.
            </p>
          </div>
        )}

        {/* Applications */}
        {!loading && applications.length > 0 && (
          <div className="space-y-6">

            {applications.map((application) => (
              <div
                key={application.id}
                className="overflow-hidden rounded-3xl border border-white/[0.07] bg-[#0d1119]"
              >

                {/* Application Header */}
                <div className="border-b border-white/[0.07] bg-white/[0.015] px-6 py-5 sm:px-7">
                  <div className="flex flex-col gap-5 lg:flex-row lg:items-center lg:justify-between">

                    <div className="flex items-start gap-4">

                      <div className="flex h-14 w-14 shrink-0 items-center justify-center rounded-2xl border border-cyan-400/20 bg-cyan-400/[0.08]">
                        <Building2
                          size={26}
                          className="text-cyan-400"
                        />
                      </div>

                      <div>
                        <div className="flex flex-wrap items-center gap-3">

                          <h2 className="text-xl font-bold text-white">
                            {application.theatreName}
                          </h2>

                          <span className="rounded-full border border-amber-400/20 bg-amber-400/10 px-3 py-1 text-[11px] font-bold uppercase tracking-wider text-amber-400">
                            Pending
                          </span>
                        </div>

                        <p className="mt-1 text-sm text-white/40">
                          Application #{application.id}
                        </p>
                      </div>
                    </div>

                    <div className="flex flex-col gap-3 sm:flex-row">

                      <button
                        type="button"
                        onClick={() => openRejectModal(application)}
                        disabled={processing}
                        className="
                          inline-flex
                          items-center
                          justify-center
                          gap-2
                          rounded-xl
                          border
                          border-red-400/20
                          bg-red-400/[0.06]
                          px-5
                          py-3
                          text-sm
                          font-semibold
                          text-red-400
                          transition
                          hover:border-red-400/40
                          hover:bg-red-400/10
                          disabled:cursor-not-allowed
                          disabled:opacity-40
                        "
                      >
                        <XCircle size={17} />
                        Reject
                      </button>

                      <button
                        type="button"
                        onClick={() => openApproveModal(application)}
                        disabled={processing}
                        className="
                          inline-flex
                          items-center
                          justify-center
                          gap-2
                          rounded-xl
                          bg-cyan-400
                          px-5
                          py-3
                          text-sm
                          font-bold
                          text-black
                          shadow-lg
                          shadow-cyan-400/10
                          transition
                          hover:bg-cyan-300
                          hover:shadow-cyan-400/20
                          disabled:cursor-not-allowed
                          disabled:opacity-40
                        "
                      >
                        <CheckCircle2 size={17} />
                        Approve Theatre
                      </button>

                    </div>
                  </div>
                </div>

                {/* Information */}
                <div className="grid gap-0 lg:grid-cols-2">

                  {/* Business Information */}
                  <div className="border-b border-white/[0.07] p-6 lg:border-r lg:border-b-0 sm:p-7">

                    <div className="mb-5 flex items-center gap-2">
                      <FileText
                        size={18}
                        className="text-cyan-400"
                      />

                      <h3 className="font-semibold">
                        Business Information
                      </h3>
                    </div>

                    <div className="grid gap-5 sm:grid-cols-2">

                      <InfoItem
                        label="Owner Name"
                        value={application.ownerName}
                        icon={<User size={16} />}
                      />

                      <InfoItem
                        label="Mobile Number"
                        value={application.mobileNumber}
                        icon={<Phone size={16} />}
                      />

                      <InfoItem
                        label="GSTIN"
                        value={application.gstin}
                      />

                      <InfoItem
                        label="PAN Number"
                        value={application.panNumber}
                      />

                      <InfoItem
                        label="Registration Number"
                        value={application.registrationNumber}
                      />

                      <InfoItem
                        label="Theatre Type"
                        value={application.theatreType}
                      />

                    </div>
                  </div>

                  {/* Location */}
                  <div className="p-6 sm:p-7">

                    <div className="mb-5 flex items-center gap-2">
                      <MapPin
                        size={18}
                        className="text-cyan-400"
                      />

                      <h3 className="font-semibold">
                        Location & Capacity
                      </h3>
                    </div>

                    <div className="grid gap-5 sm:grid-cols-2">

                      <InfoItem
                        label="Address"
                        value={application.address}
                      />

                      <InfoItem
                        label="City"
                        value={application.city}
                      />

                      <InfoItem
                        label="State"
                        value={application.state}
                      />

                      <InfoItem
                        label="Pincode"
                        value={application.pincode}
                      />

                      <InfoItem
                        label="Number of Screens"
                        value={application.numberOfScreens}
                        icon={<Monitor size={16} />}
                      />

                      <InfoItem
                        label="Total Seats"
                        value={application.totalSeats}
                        icon={<Users size={16} />}
                      />

                    </div>
                  </div>
                </div>

                {/* Documents */}
                <div className="border-t border-white/[0.07] px-6 py-5 sm:px-7">

                  <div className="mb-4 flex items-center gap-2">
                    <FileText
                      size={17}
                      className="text-cyan-400"
                    />

                    <h3 className="font-semibold">
                      Submitted Documents
                    </h3>
                  </div>

                  <div className="flex flex-wrap gap-3">

                    <DocumentLink
                      label="GST Document"
                      url={application.gstDocumentUrl}
                    />

                    <DocumentLink
                      label="Business Document"
                      url={application.businessDocumentUrl}
                    />

                    <DocumentLink
                      label="Theatre Photo"
                      url={application.theatrePhotoUrl}
                    />

                  </div>
                </div>

              </div>
            ))}
          </div>
        )}
      </div>

      {/* ===================================================== */}
      {/* CUSTOM APPROVE / REJECT MODAL */}
      {/* ===================================================== */}

      {modal.open && (
        <div
          className="
            fixed
            inset-0
            z-[9999]
            flex
            items-center
            justify-center
            bg-black/75
            px-4
            backdrop-blur-md
          "
          onClick={closeModal}
        >

          <div
            className="
              w-full
              max-w-md
              overflow-hidden
              rounded-3xl
              border
              border-white/[0.10]
              bg-[#0d1119]
              shadow-2xl
              shadow-black/60
            "
            onClick={(e) => e.stopPropagation()}
          >

            {/* Modal top */}
            <div className="relative px-6 pb-5 pt-6">

              <button
                type="button"
                onClick={closeModal}
                disabled={processing}
                className="
                  absolute
                  right-4
                  top-4
                  flex
                  h-9
                  w-9
                  items-center
                  justify-center
                  rounded-xl
                  text-white/40
                  transition
                  hover:bg-white/[0.06]
                  hover:text-white
                  disabled:opacity-30
                "
              >
                <X size={19} />
              </button>

              {/* Icon */}
              <div
                className={`
                  mb-5
                  flex
                  h-14
                  w-14
                  items-center
                  justify-center
                  rounded-2xl
                  ${
                    modal.type === "approve"
                      ? "bg-cyan-400/10"
                      : "bg-red-400/10"
                  }
                `}
              >
                {modal.type === "approve" ? (
                  <CheckCircle2
                    size={28}
                    className="text-cyan-400"
                  />
                ) : (
                  <XCircle
                    size={28}
                    className="text-red-400"
                  />
                )}
              </div>

              {/* Title */}
              <h2 className="pr-8 text-xl font-bold text-white">
                {modal.type === "approve"
                  ? "Approve Theatre?"
                  : "Reject Application?"}
              </h2>

              <p className="mt-2 text-sm leading-6 text-white/45">
                {modal.type === "approve"
                  ? `You're about to approve "${modal.application?.theatreName}". This will create the theatre under this owner's account.`
                  : `You're about to reject "${modal.application?.theatreName}". Please provide a reason for the rejection.`}
              </p>
            </div>

            {/* Reject reason */}
            {modal.type === "reject" && (
              <div className="px-6 pb-5">

                <label className="mb-2 block text-sm font-semibold text-white/80">
                  Rejection Reason
                </label>

                <textarea
                  value={rejectionReason}
                  onChange={(e) => setRejectionReason(e.target.value)}
                  placeholder="Enter the reason for rejecting this application..."
                  rows={4}
                  disabled={processing}
                  className="
                    w-full
                    resize-none
                    rounded-2xl
                    border
                    border-white/[0.10]
                    bg-[#090c12]
                    px-4
                    py-3
                    text-sm
                    text-white
                    outline-none
                    transition
                    placeholder:text-white/20
                    focus:border-red-400/50
                    focus:ring-4
                    focus:ring-red-400/10
                  "
                />

                {!rejectionReason.trim() && (
                  <p className="mt-2 text-xs text-red-400/70">
                    Please enter a rejection reason.
                  </p>
                )}
              </div>
            )}

            {/* Buttons */}
            <div className="flex gap-3 border-t border-white/[0.07] bg-white/[0.015] px-6 py-5">

              <button
                type="button"
                onClick={closeModal}
                disabled={processing}
                className="
                  flex-1
                  rounded-xl
                  border
                  border-white/[0.10]
                  bg-white/[0.04]
                  px-4
                  py-3
                  text-sm
                  font-semibold
                  text-white/70
                  transition
                  hover:bg-white/[0.07]
                  hover:text-white
                  disabled:opacity-40
                "
              >
                Cancel
              </button>

              {modal.type === "approve" ? (
                <button
                  type="button"
                  onClick={handleApprove}
                  disabled={processing}
                  className="
                    flex-1
                    rounded-xl
                    bg-cyan-400
                    px-4
                    py-3
                    text-sm
                    font-bold
                    text-black
                    transition
                    hover:bg-cyan-300
                    disabled:cursor-not-allowed
                    disabled:opacity-50
                  "
                >
                  {processing ? (
                    <span className="flex items-center justify-center gap-2">
                      <Loader2
                        size={17}
                        className="animate-spin"
                      />
                      Approving...
                    </span>
                  ) : (
                    "Approve Theatre"
                  )}
                </button>
              ) : (
                <button
                  type="button"
                  onClick={handleReject}
                  disabled={processing || !rejectionReason.trim()}
                  className="
                    flex-1
                    rounded-xl
                    bg-red-500
                    px-4
                    py-3
                    text-sm
                    font-bold
                    text-white
                    transition
                    hover:bg-red-400
                    disabled:cursor-not-allowed
                    disabled:opacity-40
                  "
                >
                  {processing ? (
                    <span className="flex items-center justify-center gap-2">
                      <Loader2
                        size={17}
                        className="animate-spin"
                      />
                      Rejecting...
                    </span>
                  ) : (
                    "Reject Application"
                  )}
                </button>
              )}

            </div>
          </div>
        </div>
      )}
    </div>
  );
}


/* ========================================================= */
/* INFO ITEM */
/* ========================================================= */

function InfoItem({ label, value, icon }) {
  return (
    <div>
      <p className="mb-1.5 flex items-center gap-1.5 text-[11px] font-semibold uppercase tracking-wider text-white/30">
        {icon}
        {label}
      </p>

      <p className="break-words text-sm font-medium text-white/80">
        {value || "Not provided"}
      </p>
    </div>
  );
}


/* ========================================================= */
/* DOCUMENT LINK */
/* ========================================================= */

function DocumentLink({ label, url }) {
  if (!url) {
    return (
      <div className="rounded-xl border border-white/[0.07] bg-white/[0.025] px-4 py-3 text-sm text-white/30">
        {label}: Not provided
      </div>
    );
  }

  return (
    <a
      href={url}
      target="_blank"
      rel="noopener noreferrer"
      className="
        inline-flex
        items-center
        gap-2
        rounded-xl
        border
        border-white/[0.08]
        bg-white/[0.03]
        px-4
        py-3
        text-sm
        font-medium
        text-white/65
        transition
        hover:border-cyan-400/30
        hover:bg-cyan-400/[0.06]
        hover:text-cyan-400
      "
    >
      <FileText size={16} />

      {label}

      <ExternalLink size={14} />
    </a>
  );
}