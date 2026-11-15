import React from "react";
import { useNavigate, useParams } from "react-router-dom";
import {
  ArrowLeft,
  CalendarDays,
  CheckCircle2,
  Clock3,
  Download,
  Loader2,
  MapPin,
  QrCode,
  RefreshCw,
  Ticket as TicketIcon,
  Users,
  XCircle,
} from "lucide-react";

import { assets } from "../assets/assets";
import {
  fetchTicketQrUrl,
  getTicket,
} from "../lib/tickets";
import {
  formatDate,
  formatDateTime,
  formatTime,
} from "../lib/formatters";
import { seatLabels } from "../lib/seatLabels";

const STATUS = {
  BOOKED: {
    label: "Valid for entry",
    icon: CheckCircle2,
    className: "bg-emerald-400/10 text-emerald-300 border-emerald-400/20",
  },
  USED: {
    label: "Admitted",
    icon: CheckCircle2,
    className: "bg-white/10 text-white/60 border-white/15",
  },
  CANCELLED: {
    label: "Cancelled",
    icon: XCircle,
    className: "bg-red-400/10 text-red-300 border-red-400/20",
  },
  EXPIRED: {
    label: "Expired",
    icon: XCircle,
    className: "bg-amber-400/10 text-amber-200 border-amber-400/20",
  },
};

export default function Ticket() {
  const { ticketId } = useParams();
  const navigate = useNavigate();

  const [ticket, setTicket] = React.useState(null);
  const [loading, setLoading] = React.useState(true);
  const [error, setError] = React.useState("");
  const [qrUrl, setQrUrl] = React.useState(null);
  const [qrError, setQrError] = React.useState("");
  const [qrLoading, setQrLoading] = React.useState(false);

  const loadTicket = React.useCallback(async () => {
    setLoading(true);
    setError("");

    try {
      const data = await getTicket(ticketId);
      setTicket(data);
    } catch (requestError) {
      setError(
        requestError.message ||
          "This ticket could not be loaded. Please try again."
      );
    } finally {
      setLoading(false);
    }
  }, [ticketId]);

  React.useEffect(() => {
    let active = true;

    setLoading(true);
    setError("");

    getTicket(ticketId)
      .then((data) => {
        if (active) setTicket(data);
      })
      .catch((requestError) => {
        if (active) {
          setError(
            requestError.message ||
              "This ticket could not be loaded. Please try again."
          );
        }
      })
      .finally(() => {
        if (active) setLoading(false);
      });

    return () => {
      active = false;
    };
  }, [ticketId]);

  React.useEffect(() => {
    let active = true;
    let objectUrl = null;

    if (!ticket?.ticketId) return undefined;

    setQrLoading(true);
    setQrError("");
    setQrUrl(null);

    fetchTicketQrUrl(ticket.ticketId)
      .then((url) => {
        objectUrl = url;

        if (active) {
          setQrUrl(url);
        } else {
          URL.revokeObjectURL(url);
        }
      })
      .catch((requestError) => {
        if (active) {
          setQrError(
            requestError.message ||
              "The QR code could not be loaded. Please try again."
          );
        }
      })
      .finally(() => {
        if (active) setQrLoading(false);
      });

    return () => {
      active = false;

      if (objectUrl) {
        URL.revokeObjectURL(objectUrl);
      }
    };
  }, [ticket?.ticketId]);

  if (loading) {
    return (
      <main className="min-h-screen px-4 pb-20 pt-32 sm:px-6">
        <div className="mx-auto max-w-[1100px] animate-pulse">
          <div className="h-5 w-32 rounded-full bg-white/[.06]" />
          <div className="mt-8 grid gap-6 lg:grid-cols-[1fr_330px]">
            <div className="h-[520px] rounded-[28px] border border-white/10 bg-white/[.03]" />
            <div className="h-[420px] rounded-[28px] border border-white/10 bg-white/[.03]" />
          </div>
        </div>
      </main>
    );
  }

  if (error || !ticket) {
    return (
      <main className="min-h-screen px-5 pb-20 pt-36 text-center">
        <TicketIcon size={44} className="mx-auto text-red-300/70" />

        <h1 className="mt-5 text-2xl font-semibold">Ticket unavailable</h1>

        <p className="mx-auto mt-2 max-w-md text-sm text-white/45">
          {error || "This ticket does not exist or is no longer available."}
        </p>

        <div className="mt-7 flex flex-wrap justify-center gap-3">
          <button
            type="button"
            onClick={loadTicket}
            className="flex items-center gap-2 rounded-full bg-cyan-300 px-6 py-3 text-sm font-semibold text-slate-950 transition hover:bg-cyan-200"
          >
            <RefreshCw size={16} />
            Try again
          </button>

          <button
            type="button"
            onClick={() => navigate("/My-Bookings")}
            className="rounded-full border border-white/15 px-6 py-3 text-sm text-white/75 transition hover:border-cyan-300/35 hover:text-white"
          >
            My bookings
          </button>
        </div>
      </main>
    );
  }

  const status = STATUS[ticket.status] || STATUS.BOOKED;
  const StatusIcon = status.icon;
  const seats = seatLabels(ticket.seatNumbers);
  const admitted = ticket.status === "USED";

  return (
    <main className="min-h-screen px-4 pb-20 pt-28 sm:px-6 lg:px-10">
      <div className="mx-auto max-w-[1120px]">
        <div className="no-print mb-7 flex flex-col justify-between gap-4 sm:flex-row sm:items-end">
          <div>
            <button
              type="button"
              onClick={() => navigate("/My-Bookings")}
              className="mb-5 flex items-center gap-2 text-sm text-white/45 transition hover:text-white"
            >
              <ArrowLeft size={17} />
              My bookings
            </button>

            <p className="text-xs font-semibold uppercase tracking-[.25em] text-cyan-300">
              SRH CinePass digital ticket
            </p>

            <h1 className="mt-2 text-3xl font-semibold sm:text-4xl">
              {admitted ? "Your movie ticket" : "Your cinema ticket"}
            </h1>
          </div>

          <div className="flex flex-wrap gap-3">
            <button
              type="button"
              onClick={() => window.print()}
              disabled={!qrUrl}
              title={
                qrUrl
                  ? "Print or save this ticket as a PDF"
                  : "The ticket QR code must load before printing"
              }
              className="flex items-center gap-2 rounded-full bg-cyan-300 px-5 py-3 text-sm font-semibold text-slate-950 transition hover:bg-cyan-200 disabled:cursor-not-allowed disabled:opacity-40"
            >
              <Download size={17} />
              Download / Print
            </button>
          </div>
        </div>

        <article className="print-area ticket-card overflow-hidden rounded-[28px] border border-white/10 bg-[#0b1017] shadow-[0_30px_90px_rgba(0,0,0,.35)]">
          <div className="grid lg:grid-cols-[1fr_330px]">
            <div className="ticket-panel relative p-6 sm:p-8">
              <div className="flex flex-wrap items-start justify-between gap-4">
                <div className="ticket-brand flex items-center gap-3">
                  <img
                    src={assets.logo}
                    alt="SRH CinePass"
                    className="h-10 w-auto"
                  />

                  <div>
                    <p className="text-sm font-semibold tracking-wide">
                      SRH CinePass
                    </p>

                    <p className="text-[10px] uppercase tracking-[.22em] text-white/35">
                      Digital cinema ticket
                    </p>
                  </div>
                </div>

                <span
                  className={`flex items-center gap-1.5 rounded-full border px-3 py-1.5 text-[10px] font-semibold uppercase tracking-wider ${status.className}`}
                >
                  <StatusIcon size={12} />
                  {status.label}
                </span>
              </div>

              <div className="mt-8 flex flex-col gap-6 sm:flex-row">
                <div className="h-40 w-28 shrink-0 overflow-hidden rounded-2xl border border-white/10 bg-gradient-to-br from-cyan-300/20 to-blue-900/30">
                  {ticket.moviePosterUrl ? (
                    <img
                      src={ticket.moviePosterUrl}
                      alt={ticket.movieTitle || "Movie poster"}
                      className="h-full w-full object-cover"
                    />
                  ) : (
                    <div className="grid h-full place-items-center text-cyan-200/50">
                      <TicketIcon size={34} />
                    </div>
                  )}
                </div>

                <div className="min-w-0">
                  <p className="text-[10px] font-semibold uppercase tracking-[.24em] text-cyan-300">
                    Now showing
                  </p>

                  <h2 className="mt-2 text-3xl font-semibold leading-tight sm:text-4xl">
                    {ticket.movieTitle || "Movie"}
                  </h2>

                  <p className="ticket-muted mt-3 flex items-center gap-2 text-sm text-white/50">
                    <MapPin size={15} className="shrink-0 text-cyan-300" />
                    {ticket.theatreName || "Theatre"}
                    {ticket.theatreCity ? ` · ${ticket.theatreCity}` : ""}
                  </p>

                  {ticket.screenNumber ? (
                    <p className="ticket-muted mt-1 text-sm text-white/40">
                      Screen {ticket.screenNumber}
                    </p>
                  ) : null}
                </div>
              </div>

              <div className="my-7 border-t border-dashed border-white/15" />

              <div className="grid gap-5 sm:grid-cols-2 lg:grid-cols-3">
                <Detail
                  icon={CalendarDays}
                  label="Show date"
                  value={formatDate(ticket.showDate)}
                />

                <Detail
                  icon={Clock3}
                  label="Show time"
                  value={formatTime(ticket.showTime)}
                />

                <Detail
                  icon={Users}
                  label="Seats"
                  value={seats.length ? seats.join(", ") : "—"}
                />

                <Detail
                  label="Ticket quantity"
                  value={`${ticket.quantity ?? seats.length} ticket${
                    (ticket.quantity ?? seats.length) === 1 ? "" : "s"
                  }`}
                />

                <Detail
                  label="Booking reference"
                  value={ticket.ticketReference || "—"}
                  mono
                />

                <Detail
                  label="Booked on"
                  value={formatDateTime(ticket.bookingTimestamp)}
                />
              </div>

              <div className="mt-8 flex flex-col justify-between gap-3 border-t border-white/10 pt-5 sm:flex-row sm:items-center">
                <div>
                  <p className="ticket-label text-[10px] uppercase tracking-[.22em] text-white/30">
                    Guest
                  </p>

                  <p className="mt-1 text-sm font-medium text-white/80">
                    {ticket.customerName || "SRH CinePass customer"}
                  </p>
                </div>

                <div className="sm:text-right">
                  <p className="ticket-label text-[10px] uppercase tracking-[.22em] text-white/30">
                    Booking status
                  </p>

                  <p className="ticket-accent mt-1 text-sm font-semibold text-cyan-200">
                    {ticket.bookingStatus || "CONFIRMED"}
                  </p>
                </div>
              </div>

              <div className="ticket-receipt-note mt-5 rounded-2xl border border-white/[.07] bg-white/[.03] p-4 text-[11px] leading-5 text-white/35">
                Present this QR code at the theatre entrance. The QR code is
                checked online by authorised staff and can be used only once.
              </div>
            </div>

            <aside className="ticket-panel relative flex flex-col justify-between border-t border-dashed border-white/15 bg-gradient-to-b from-cyan-300/[.08] to-transparent p-6 sm:p-8 lg:border-l lg:border-t-0">
              <div>
                <div className="flex items-center justify-between">
                  <p className="text-[10px] font-semibold uppercase tracking-[.24em] text-cyan-300">
                    Entry pass
                  </p>

                  <QrCode size={18} className="text-cyan-300/60" />
                </div>

                <div className="relative mt-5 overflow-hidden rounded-2xl bg-white p-3 shadow-[0_20px_50px_rgba(0,0,0,.25)]">
                  {qrLoading ? (
                    <div className="grid aspect-square w-full place-items-center bg-slate-100 text-slate-500">
                      <Loader2 className="animate-spin" size={26} />
                    </div>
                  ) : qrUrl ? (
                    <img
                      src={qrUrl}
                      alt="SRH CinePass ticket QR code"
                      className="ticket-qr aspect-square w-full object-contain"
                    />
                  ) : (
                    <div className="grid aspect-square w-full place-items-center bg-slate-100 px-5 text-center text-xs text-slate-500">
                      {qrError || "QR code unavailable"}
                    </div>
                  )}

                  {ticket.status !== "BOOKED" && (
                    <div className="absolute inset-0 grid place-items-center bg-slate-950/70 backdrop-blur-[1px]">
                      <span className="rotate-[-8deg] rounded-xl border-2 border-white/70 px-4 py-2 text-lg font-bold uppercase tracking-widest text-white">
                        {status.label}
                      </span>
                    </div>
                  )}
                </div>

                <p className="mt-4 text-center text-[10px] uppercase tracking-[.2em] text-white/30">
                  Scan at entrance
                </p>

                <p className="mt-2 text-center font-mono text-sm text-white/75">
                  {ticket.ticketReference}
                </p>
              </div>

              <div className="mt-6 border-t border-white/10 pt-4 text-center">
                <p className="text-[10px] uppercase tracking-[.2em] text-white/25">
                  {ticket.status === "BOOKED"
                    ? "Single entry · Non-transferable"
                    : status.label}
                </p>

                <p className="mt-2 text-[10px] text-white/25">
                  Issued {formatDateTime(ticket.issuedAt)}
                </p>
              </div>
            </aside>
          </div>
        </article>

        <div className="no-print mt-6 flex flex-col justify-between gap-3 rounded-2xl border border-white/[.07] bg-white/[.025] p-4 text-xs text-white/35 sm:flex-row sm:items-center">
          <p>
            Tip: choose <span className="text-white/60">Save as PDF</span> in
            your browser&apos;s print dialog to download this ticket.
          </p>

          <p className="text-white/25">
            Ticket reference {ticket.ticketReference}
          </p>
        </div>
      </div>
    </main>
  );
}

function Detail({ icon: Icon, label, value, mono = false }) {
  return (
    <div className="min-w-0">
      <p className="ticket-label flex items-center gap-1.5 text-[10px] uppercase tracking-[.2em] text-white/30">
        {Icon ? <Icon size={12} className="text-cyan-300/70" /> : null}
        {label}
      </p>

      <p
        className={`ticket-muted mt-1.5 break-words text-sm text-white/80 ${
          mono ? "font-mono" : ""
        }`}
      >
        {value || "—"}
      </p>
    </div>
  );
}
