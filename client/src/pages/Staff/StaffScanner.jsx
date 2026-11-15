import React from "react";
import { useNavigate } from "react-router-dom";
import {
  AlertTriangle,
  Camera,
  CameraOff,
  CheckCircle2,
  Clock3,
  Keyboard,
  Loader2,
  LogOut,
  MapPin,
  ScanLine,
  ShieldCheck,
  Ticket,
  Users,
  XCircle,
} from "lucide-react";

import { useAuth } from "../../context/AuthContext";
import {
  formatDate,
  formatDateTime,
  formatTime,
} from "../../lib/formatters";
import {
  isWellFormedTicketToken,
  verifyTicket,
} from "../../lib/tickets";

function extractToken(value) {
  const input = String(value ?? "").trim();

  if (!input) return "";

  try {
    const url = new URL(input);
    const queryToken = url.searchParams.get("token");

    if (queryToken) return queryToken.trim();
  } catch {
    // Not a URL; the QR contains the opaque token directly.
  }

  return input;
}

function cameraErrorMessage(error) {
  const name = error?.name || "";
  const raw = String(error?.message || "");

  if (name === "NotAllowedError" || /permission/i.test(raw)) {
    return "Camera permission was denied. Allow camera access in your browser settings, then try again.";
  }

  if (name === "NotFoundError" || /not found/i.test(raw)) {
    return "No camera was found on this device. You can still verify a ticket using manual token entry.";
  }

  if (name === "NotReadableError" || /could not start/i.test(raw)) {
    return "The camera is already in use by another application. Close it and try again.";
  }

  if (name === "OverconstrainedError") {
    return "This camera does not support the requested video mode. Try another device or use manual entry.";
  }

  if (name === "SecurityError") {
    return "Camera access requires HTTPS. Open the deployed site over HTTPS and try again.";
  }

  if (!navigator?.mediaDevices?.getUserMedia) {
    return "This browser does not support camera scanning. Use manual token entry instead.";
  }

  return "The camera could not be started. Check browser permission and try again, or use manual entry.";
}

const RESULT_TONES = {
  VALID: {
    card: "border-emerald-400/30 bg-emerald-400/[.08]",
    icon: "text-emerald-300",
    title: "text-emerald-200",
    Icon: CheckCircle2,
  },
  ALREADY_USED: {
    card: "border-red-400/30 bg-red-400/[.08]",
    icon: "text-red-300",
    title: "text-red-200",
    Icon: XCircle,
  },
  CANCELLED: {
    card: "border-red-400/30 bg-red-400/[.08]",
    icon: "text-red-300",
    title: "text-red-200",
    Icon: XCircle,
  },
  INVALID: {
    card: "border-red-400/30 bg-red-400/[.08]",
    icon: "text-red-300",
    title: "text-red-200",
    Icon: XCircle,
  },
  EXPIRED: {
    card: "border-red-400/30 bg-red-400/[.08]",
    icon: "text-red-300",
    title: "text-red-200",
    Icon: XCircle,
  },
  NOT_CONFIRMED: {
    card: "border-amber-400/30 bg-amber-400/[.08]",
    icon: "text-amber-300",
    title: "text-amber-200",
    Icon: AlertTriangle,
  },
  NOT_YET_VALID: {
    card: "border-amber-400/30 bg-amber-400/[.08]",
    icon: "text-amber-300",
    title: "text-amber-200",
    Icon: Clock3,
  },
  UNAUTHORIZED: {
    card: "border-red-400/30 bg-red-400/[.08]",
    icon: "text-red-300",
    title: "text-red-200",
    Icon: ShieldCheck,
  },
  RATE_LIMITED: {
    card: "border-amber-400/30 bg-amber-400/[.08]",
    icon: "text-amber-300",
    title: "text-amber-200",
    Icon: Clock3,
  },
};

export default function StaffScanner() {
  const { user, logout } = useAuth();
  const navigate = useNavigate();

  const scannerRef = React.useRef(null);
  const processingRef = React.useRef(false);
  const mountedRef = React.useRef(true);

  const [cameraState, setCameraState] = React.useState("idle");
  const [cameraError, setCameraError] = React.useState("");
  const [manualToken, setManualToken] = React.useState("");
  const [result, setResult] = React.useState(null);
  const [processing, setProcessing] = React.useState(false);

  const stopScanner = React.useCallback(async () => {
    const scanner = scannerRef.current;
    scannerRef.current = null;

    if (scanner) {
      try {
        await scanner.stop();
      } catch {
        // The scanner may already be stopped.
      }

      try {
        scanner.clear();
      } catch {
        // Nothing to clear.
      }
    }

    if (mountedRef.current) {
      setCameraState("idle");
    }
  }, []);

  React.useEffect(() => {
    mountedRef.current = true;

    return () => {
      mountedRef.current = false;

      const scanner = scannerRef.current;
      scannerRef.current = null;

      if (scanner) {
        try {
          scanner.stop().catch(() => {});
        } catch {
          // Ignore cleanup errors.
        }

        try {
          scanner.clear();
        } catch {
          // Ignore cleanup errors.
        }
      }
    };
  }, []);

  const pauseScanner = React.useCallback(() => {
    const scanner = scannerRef.current;

    if (scanner) {
      try {
        scanner.pause(true);
      } catch {
        // Pausing is best-effort; the processing guard still prevents repeats.
      }
    }
  }, []);

  const submitToken = React.useCallback(
    async (rawToken) => {
      const token = extractToken(rawToken);

      if (processingRef.current) return;

      if (!token) {
        setResult({
          result: "INVALID",
          message: "Enter or scan a ticket token.",
        });
        return;
      }

      if (!isWellFormedTicketToken(token)) {
        setResult({
          result: "INVALID",
          message: "Invalid Ticket",
        });
        pauseScanner();
        return;
      }

      processingRef.current = true;
      setProcessing(true);
      setResult(null);

      try {
        const data = await verifyTicket(token);

        if (mountedRef.current) {
          setResult(data);
        }

        pauseScanner();
        setManualToken("");
      } catch (error) {
        if (mountedRef.current) {
          const unauthorized = error.status === 401 || error.status === 403;
          const rateLimited = error.status === 429;

          setResult({
            result: unauthorized
              ? "UNAUTHORIZED"
              : rateLimited
                ? "RATE_LIMITED"
                : "INVALID",
            message: unauthorized
              ? "Staff access required. Sign in with a staff account."
              : rateLimited
                ? "Too many scans. Please wait a moment."
                : error.message || "Ticket verification failed.",
          });
        }
      } finally {
        processingRef.current = false;

        if (mountedRef.current) {
          setProcessing(false);
        }
      }
    },
    [pauseScanner]
  );

  const startScanner = React.useCallback(async () => {
    if (scannerRef.current || cameraState === "starting") return;

    setCameraError("");
    setResult(null);

    if (
      !window.isSecureContext &&
      !["localhost", "127.0.0.1"].includes(window.location.hostname)
    ) {
      setCameraState("error");
      setCameraError(
        "Camera access requires HTTPS. Open the deployed site over HTTPS, or use manual token entry."
      );
      return;
    }

    if (!navigator?.mediaDevices?.getUserMedia) {
      setCameraState("error");
      setCameraError(
        "This browser does not support camera scanning. Use manual token entry instead."
      );
      return;
    }

    setCameraState("starting");

    try {
      const module = await import("html5-qrcode");
      const Html5Qrcode = module.Html5Qrcode || module.default?.Html5Qrcode;

      if (!Html5Qrcode) {
        throw new Error("Scanner library is unavailable");
      }

      const scanner = new Html5Qrcode("staff-qr-reader", {
        verbose: false,
      });

      scannerRef.current = scanner;

      await scanner.start(
        { facingMode: "environment" },
        {
          fps: 10,
          qrbox: (viewfinderWidth, viewfinderHeight) => {
            const size = Math.floor(
              Math.min(viewfinderWidth, viewfinderHeight) * 0.72
            );

            return {
              width: Math.max(160, size),
              height: Math.max(160, size),
            };
          },
          aspectRatio: 1,
          disableFlip: false,
        },
        (decodedText) => {
          submitToken(decodedText);
        },
        () => {
          // Per-frame decode errors are expected while aiming the camera.
        }
      );

      if (!mountedRef.current) {
        await stopScanner();
        return;
      }

      setCameraState("scanning");
    } catch (error) {
      console.error("Staff camera error:", error);

      try {
        scannerRef.current?.clear();
      } catch {
        // Ignore cleanup errors.
      }

      scannerRef.current = null;

      if (mountedRef.current) {
        setCameraState("error");
        setCameraError(cameraErrorMessage(error));
      }
    }
  }, [cameraState, stopScanner, submitToken]);

  const scanNext = () => {
    setResult(null);
    setCameraError("");

    const scanner = scannerRef.current;

    if (scanner) {
      try {
        scanner.resume();
        setCameraState("scanning");
      } catch {
        setCameraState("error");
        setCameraError("The camera could not be resumed. Stop and start it again.");
      }
    } else {
      startScanner();
    }
  };

  const handleManualSubmit = (event) => {
    event.preventDefault();
    submitToken(manualToken);
  };

  const tone = RESULT_TONES[result?.result] || RESULT_TONES.INVALID;
  const ResultIcon = tone.Icon;
  const scanning = cameraState === "scanning";
  const starting = cameraState === "starting";

  if (!["STAFF", "ADMIN"].includes(user?.role)) {
    return (
      <main className="min-h-screen px-5 pb-20 pt-36 text-center">
        <ShieldCheck size={44} className="mx-auto text-red-300/70" />

        <h1 className="mt-5 text-2xl font-semibold">Staff access required</h1>

        <p className="mx-auto mt-2 max-w-md text-sm text-white/45">
          This page is only available to authorised theatre staff.
        </p>

        <button
          type="button"
          onClick={() => navigate("/")}
          className="mt-7 rounded-full bg-cyan-300 px-6 py-3 text-sm font-semibold text-slate-950 transition hover:bg-cyan-200"
        >
          Back to SRH CinePass
        </button>
      </main>
    );
  }

  return (
    <main className="min-h-screen px-4 pb-20 pt-28 sm:px-6 lg:px-10">
      <div className="mx-auto max-w-[1200px]">
        <div className="mb-8 flex flex-col justify-between gap-4 sm:flex-row sm:items-end">
          <div>
            <p className="text-xs font-semibold uppercase tracking-[.25em] text-cyan-300">
              Theatre operations
            </p>

            <h1 className="mt-2 flex items-center gap-3 text-3xl font-semibold sm:text-4xl">
              <ScanLine className="text-cyan-300" size={30} />
              Ticket scanner
            </h1>

            <p className="mt-2 text-sm text-white/40">
              Signed in as {user?.name || "staff"} · {user?.role}
            </p>
          </div>

          <div className="flex flex-wrap items-center gap-2 self-start">
            <span className="inline-flex items-center gap-2 rounded-full border border-emerald-400/20 bg-emerald-400/10 px-3 py-1.5 text-[10px] font-semibold uppercase tracking-wider text-emerald-300">
              <ShieldCheck size={13} />
              Authorised staff only
            </span>

            <button
              type="button"
              onClick={() => {
                logout();
                navigate("/");
              }}
              className="flex items-center gap-2 rounded-full border border-white/10 px-4 py-2 text-xs text-white/55 transition hover:border-red-400/30 hover:text-red-200"
            >
              <LogOut size={14} />
              Sign out
            </button>
          </div>
        </div>

        <div className="grid gap-6 xl:grid-cols-[1.15fr_.85fr]">
          <section className="rounded-3xl border border-white/10 bg-[#0b1017] p-5 sm:p-7">
            <div className="flex flex-wrap items-center justify-between gap-3">
              <div>
                <h2 className="text-lg font-semibold">Scan a ticket</h2>
                <p className="mt-1 text-xs text-white/35">
                  Point the camera at the QR code on the customer&apos;s ticket.
                </p>
              </div>

              <div className="flex gap-2">
                {scanning ? (
                  <button
                    type="button"
                    onClick={stopScanner}
                    className="flex items-center gap-2 rounded-full border border-red-400/20 bg-red-400/10 px-4 py-2.5 text-xs font-semibold text-red-200 transition hover:bg-red-400/15"
                  >
                    <CameraOff size={15} />
                    Stop camera
                  </button>
                ) : (
                  <button
                    type="button"
                    onClick={startScanner}
                    disabled={starting}
                    className="flex items-center gap-2 rounded-full bg-cyan-300 px-4 py-2.5 text-xs font-semibold text-slate-950 transition hover:bg-cyan-200 disabled:cursor-not-allowed disabled:opacity-50"
                  >
                    {starting ? (
                      <Loader2 className="animate-spin" size={15} />
                    ) : (
                      <Camera size={15} />
                    )}
                    {starting ? "Starting..." : "Start camera"}
                  </button>
                )}
              </div>
            </div>

            <div className="relative mt-5 overflow-hidden rounded-2xl border border-white/10 bg-black/40">
              <div
                id="staff-qr-reader"
                className="min-h-[300px] w-full [&_video]:min-h-[300px] [&_video]:w-full [&_video]:object-cover"
              />

              {!scanning && (
                <div className="pointer-events-none absolute inset-0 grid place-items-center bg-[#070a0f]/70 p-6 text-center">
                  <div>
                    <div className="mx-auto grid h-16 w-16 place-items-center rounded-2xl border border-cyan-300/20 bg-cyan-300/10 text-cyan-300">
                      {starting ? (
                        <Loader2 className="animate-spin" size={27} />
                      ) : (
                        <Camera size={27} />
                      )}
                    </div>

                    <p className="mt-4 text-sm font-medium text-white/70">
                      {starting ? "Requesting camera permission..." : "Camera is off"}
                    </p>

                    <p className="mx-auto mt-1 max-w-xs text-xs leading-5 text-white/35">
                      Start the camera to scan a QR code. Manual token entry is
                      available below if a camera is not available.
                    </p>
                  </div>
                </div>
              )}
            </div>

            {cameraError && (
              <div className="mt-4 flex gap-3 rounded-2xl border border-amber-400/20 bg-amber-400/[.07] p-4 text-xs leading-5 text-amber-100/80">
                <AlertTriangle size={17} className="mt-0.5 shrink-0 text-amber-300" />
                <p>{cameraError}</p>
              </div>
            )}

            <div className="my-6 border-t border-white/10" />

            <form onSubmit={handleManualSubmit}>
              <div className="flex items-center gap-2 text-sm font-medium">
                <Keyboard size={17} className="text-cyan-300" />
                Manual token entry
              </div>

              <p className="mt-1 text-xs text-white/35">
                Use this fallback when the customer&apos;s QR code cannot be
                scanned. The token is verified by the server, never locally.
              </p>

              <div className="mt-4 flex flex-col gap-3 sm:flex-row">
                <input
                  value={manualToken}
                  onChange={(event) => setManualToken(event.target.value)}
                  placeholder="Paste the verification token"
                  autoComplete="off"
                  spellCheck={false}
                  className="min-w-0 flex-1 rounded-xl border border-white/10 bg-white/[.04] px-4 py-3 font-mono text-sm text-white outline-none placeholder:text-white/25 focus:border-cyan-300/40"
                />

                <button
                  type="submit"
                  disabled={processing || !manualToken.trim()}
                  className="flex items-center justify-center gap-2 rounded-xl bg-cyan-300 px-5 py-3 text-sm font-semibold text-slate-950 transition hover:bg-cyan-200 disabled:cursor-not-allowed disabled:opacity-40"
                >
                  {processing ? (
                    <Loader2 className="animate-spin" size={16} />
                  ) : (
                    <ShieldCheck size={16} />
                  )}
                  Verify ticket
                </button>
              </div>
            </form>
          </section>

          <section className="rounded-3xl border border-white/10 bg-[#0b1017] p-5 sm:p-7">
            <div className="flex items-center justify-between gap-3">
              <div>
                <h2 className="text-lg font-semibold">Verification result</h2>
                <p className="mt-1 text-xs text-white/35">
                  Admission is only confirmed by the backend.
                </p>
              </div>

              {result && (
                <button
                  type="button"
                  onClick={scanNext}
                  className="rounded-full border border-white/15 px-4 py-2 text-xs text-white/65 transition hover:border-cyan-300/35 hover:text-white"
                >
                  Scan next
                </button>
              )}
            </div>

            {processing && !result ? (
              <div className="mt-6 flex min-h-[300px] flex-col items-center justify-center gap-3 rounded-2xl border border-white/10 bg-white/[.02] text-sm text-white/45">
                <Loader2 className="animate-spin text-cyan-300" size={26} />
                Verifying ticket...
              </div>
            ) : result ? (
              <div className={`mt-6 rounded-2xl border p-5 ${tone.card}`}>
                <div className="flex items-start gap-3">
                  <ResultIcon size={26} className={`mt-0.5 shrink-0 ${tone.icon}`} />

                  <div className="min-w-0">
                    <p className={`text-xl font-semibold ${tone.title}`}>
                      {result.message || "Verification result"}
                    </p>

                    <p className="mt-1 text-xs uppercase tracking-wider text-white/35">
                      {result.result}
                    </p>
                  </div>
                </div>

                {result.result !== "INVALID" && result.ticketReference && (
                  <div className="mt-5 space-y-3 border-t border-white/10 pt-4 text-sm">
                    <ResultRow icon={Ticket} label="Movie" value={result.movieTitle} />
                    <ResultRow icon={MapPin} label="Theatre" value={result.theatreName} />
                    <ResultRow
                      icon={Clock3}
                      label="Show"
                      value={
                        result.showDate
                          ? `${formatDate(result.showDate)} · ${formatTime(result.showTime)}`
                          : null
                      }
                    />
                    <ResultRow
                      icon={Users}
                      label="Seats"
                      value={
                        result.seatNumbers?.length
                          ? result.seatNumbers.join(", ")
                          : null
                      }
                    />

                    {result.customerName && (
                      <ResultRow
                        icon={Ticket}
                        label="Customer"
                        value={result.customerName}
                      />
                    )}

                    {result.verifiedAt && (
                      <ResultRow
                        icon={CheckCircle2}
                        label="Verified at"
                        value={formatDateTime(result.verifiedAt)}
                      />
                    )}

                    {result.verifiedBy && (
                      <ResultRow
                        icon={ShieldCheck}
                        label="Verified by"
                        value={result.verifiedBy}
                      />
                    )}

                    <p className="pt-2 font-mono text-[11px] text-white/35">
                      {result.ticketReference}
                    </p>
                  </div>
                )}

                {result.result === "VALID" && (
                  <div className="mt-5 rounded-xl border border-emerald-300/20 bg-emerald-300/10 p-3 text-xs leading-5 text-emerald-100/80">
                    Admission recorded atomically. This ticket cannot be used
                    again.
                  </div>
                )}
              </div>
            ) : (
              <div className="mt-6 flex min-h-[300px] flex-col items-center justify-center rounded-2xl border border-dashed border-white/10 bg-white/[.02] p-6 text-center">
                <ShieldCheck size={30} className="text-white/20" />

                <p className="mt-4 text-sm font-medium text-white/55">
                  Waiting for a ticket
                </p>

                <p className="mx-auto mt-1 max-w-xs text-xs leading-5 text-white/30">
                  Scan a QR code or enter a token manually. A green result is
                  shown only after the server marks the ticket as USED.
                </p>
              </div>
            )}

            <div className="mt-6 grid grid-cols-3 gap-2 text-center text-[10px] uppercase tracking-wider text-white/30">
              <span className="rounded-lg border border-emerald-400/15 bg-emerald-400/[.05] px-2 py-2 text-emerald-300/80">
                Green · Admit
              </span>
              <span className="rounded-lg border border-amber-400/15 bg-amber-400/[.05] px-2 py-2 text-amber-200/80">
                Yellow · Check
              </span>
              <span className="rounded-lg border border-red-400/15 bg-red-400/[.05] px-2 py-2 text-red-300/80">
                Red · Reject
              </span>
            </div>
          </section>
        </div>
      </div>
    </main>
  );
}

function ResultRow({ icon: Icon, label, value }) {
  if (!value) return null;

  return (
    <div className="flex gap-3">
      {Icon ? (
        <Icon size={15} className="mt-0.5 shrink-0 text-cyan-300/70" />
      ) : null}

      <div className="min-w-0">
        <p className="text-[10px] uppercase tracking-wider text-white/25">
          {label}
        </p>

        <p className="mt-0.5 break-words text-sm text-white/75">{value}</p>
      </div>
    </div>
  );
}
