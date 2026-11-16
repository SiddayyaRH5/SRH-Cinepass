import React from "react";
import {
  Activity,
  AlertCircle,
  Archive,
  CalendarDays,
  CheckCircle2,
  Clock3,
  Film,
  Loader2,
  Play,
  RefreshCw,
  ShieldCheck,
  Sparkles,
  XCircle,
} from "lucide-react";
import { Link } from "react-router-dom";

import Title from "../../components/Admin/Title";
import Loading from "../../components/Loading";
import {
  archiveMovie,
  excludeMovie,
  extendMovie,
  getAdminMovies,
  getAdminSyncStatus,
  getAutomationConfig,
  getAutomationRuns,
  protectMovie,
  reactivateMovie,
  runAutomation,
} from "../../lib/api";

const OUTCOME_TONE = {
  SUCCESS: "text-emerald-300",
  PARTIAL: "text-amber-300",
  FAILED: "text-red-300",
  SKIPPED: "text-white/40",
};

function StatusPill({ status }) {
  const tone =
    status === "SUCCESS"
      ? "border-emerald-300/30 bg-emerald-300/10 text-emerald-200"
      : status === "PARTIAL"
      ? "border-amber-300/30 bg-amber-300/10 text-amber-200"
      : status === "FAILED"
      ? "border-red-400/30 bg-red-400/10 text-red-200"
      : status === "RUNNING"
      ? "border-cyan-300/30 bg-cyan-300/10 text-cyan-200"
      : "border-white/10 bg-white/[.03] text-white/40";

  return (
    <span
      className={`inline-flex items-center gap-1.5 rounded-full border px-2.5 py-1 text-[10px] font-semibold uppercase tracking-wider ${tone}`}
    >
      {status === "SUCCESS" && <CheckCircle2 size={11} />}
      {status === "PARTIAL" && <AlertCircle size={11} />}
      {status === "FAILED" && <XCircle size={11} />}
      {status === "RUNNING" && <Loader2 size={11} className="animate-spin" />}
      {status}
    </span>
  );
}

function StatCard({ label, value, icon: Icon, tone = "text-cyan-300" }) {
  return (
    <div className="rounded-2xl border border-white/10 bg-white/[.03] p-4">
      <div className="flex items-center justify-between">
        <span className={`grid h-9 w-9 place-items-center rounded-xl bg-white/[.04] ${tone}`}>
          <Icon size={17} />
        </span>
      </div>
      <p className="mt-4 text-[10px] uppercase tracking-wider text-white/30">
        {label}
      </p>
      <p className="mt-1 text-xl font-semibold text-white">
        {value ?? "—"}
      </p>
    </div>
  );
}

function Panel({ title, subtitle, children, actions }) {
  return (
    <section className="rounded-2xl border border-white/10 bg-white/[.025] p-5 sm:p-6">
      <div className="flex flex-wrap items-start justify-between gap-3">
        <div>
          <h2 className="text-base font-semibold text-white">{title}</h2>
          {subtitle && (
            <p className="mt-1 text-xs text-white/35">{subtitle}</p>
          )}
        </div>
        {actions && <div className="flex flex-wrap gap-2">{actions}</div>}
      </div>
      <div className="mt-5">{children}</div>
    </section>
  );
}

function Button({ children, onClick, variant = "default", disabled }) {
  const base =
    "inline-flex items-center gap-2 rounded-xl px-3.5 py-2 text-xs font-semibold transition disabled:opacity-40 disabled:cursor-not-allowed";
  const styles =
    variant === "danger"
      ? "border border-red-400/30 bg-red-400/10 text-red-200 hover:bg-red-400/15"
      : variant === "primary"
      ? "border border-cyan-300/30 bg-cyan-300/10 text-cyan-200 hover:bg-cyan-300/15"
      : "border border-white/10 bg-white/[.03] text-white/60 hover:bg-white/[.05] hover:text-white";

  return (
    <button
      type="button"
      onClick={onClick}
      disabled={disabled}
      className={`${base} ${styles}`}
    >
      {children}
    </button>
  );
}

export default function Automation() {
  const [status, setStatus] = React.useState(null);
  const [config, setConfig] = React.useState(null);
  const [runs, setRuns] = React.useState([]);
  const [movies, setMovies] = React.useState([]);
  const [loading, setLoading] = React.useState(true);
  const [error, setError] = React.useState("");
  const [busy, setBusy] = React.useState(false);
  const [toast, setToast] = React.useState("");

  const load = React.useCallback(async () => {
    setLoading(true);
    setError("");

    try {
      const [s, cfg, r, m] = await Promise.all([
        getAdminSyncStatus(),
        getAutomationConfig(),
        getAutomationRuns(),
        getAdminMovies(),
      ]);

      setStatus(s);
      setConfig(cfg);
      setRuns(Array.isArray(r) ? r : []);
      setMovies(Array.isArray(m) ? m : []);
    } catch (err) {
      setError(err?.message || "Unable to load automation status.");
    } finally {
      setLoading(false);
    }
  }, []);

  React.useEffect(() => {
    load();
  }, [load]);

  const notify = (message) => {
    setToast(message);
    window.setTimeout(() => setToast(""), 3500);
  };

  const handleRun = async () => {
    if (
      !window.confirm(
        "Trigger the daily automation now? This will run TMDB sync, lifecycle evaluation and show generation."
      )
    ) {
      return;
    }

    setBusy(true);
    try {
      const run = await runAutomation();
      notify(`Automation finished: ${run?.outcome ?? "completed"}.`);
      await load();
    } catch (err) {
      notify(err?.message || "Automation failed.");
    } finally {
      setBusy(false);
    }
  };

  const handleSync = async () => {
    if (
      !window.confirm(
        "Synchronize movies from TMDB now? This will import and update movies."
      )
    ) {
      return;
    }

    setBusy(true);
    try {
      await runAutomation();
      notify("Synchronization triggered via automation run.");
      await load();
    } catch (err) {
      notify(err?.message || "Synchronization failed.");
    } finally {
      setBusy(false);
    }
  };

  const handleMovieAction = async (action, movie) => {
    setBusy(true);
    try {
      if (action === "protect") {
        await protectMovie(movie.id, !movie.adminProtected);
        notify(
          movie.adminProtected
            ? "Protection removed."
            : "Movie protected from retirement."
        );
      } else if (action === "exclude") {
        await excludeMovie(movie.id, !movie.excludedFromAutoShows, "");
        notify(
          movie.excludedFromAutoShows
            ? "Movie re-enabled for shows."
            : "Movie excluded from show generation."
        );
      } else if (action === "archive") {
        if (!window.confirm(`Archive "${movie.title}"?`)) {
          setBusy(false);
          return;
        }
        await archiveMovie(movie.id);
        notify("Movie archived. History preserved.");
      } else if (action === "reactivate") {
        await reactivateMovie(movie.id);
        notify("Movie reactivated.");
      } else if (action === "extend") {
        const until = window.prompt(
          "Extend active period until (YYYY-MM-DD):",
          movie.activeUntil || ""
        );
        if (!until) {
          setBusy(false);
          return;
        }
        await extendMovie(movie.id, until);
        notify("Active period extended.");
      }
      await load();
    } catch (err) {
      notify(err?.message || "Action failed.");
    } finally {
      setBusy(false);
    }
  };

  if (loading) {
    return (
      <main className="min-h-screen bg-[#05070b] px-5 pb-20 pt-28 sm:px-8">
        <Loading label="Loading automation status..." />
      </main>
    );
  }

  if (error) {
    return (
      <main className="min-h-screen bg-[#05070b] px-5 pb-20 pt-28 sm:px-8">
        <div className="mx-auto max-w-[1440px]">
          <Title text1="Automation" text2="Dashboard" />
          <div className="mt-6 flex flex-wrap items-center justify-between gap-3 rounded-xl border border-red-400/25 bg-red-400/[.07] px-4 py-3 text-sm text-red-200">
            <span className="flex items-center gap-2">
              <AlertCircle size={15} />
              {error}
            </span>
            <button
              type="button"
              onClick={load}
              className="flex items-center gap-1.5 rounded-lg border border-red-300/30 px-2.5 py-1 text-xs text-red-100 transition hover:bg-red-300/10"
            >
              <RefreshCw size={12} />
              Retry
            </button>
          </div>
        </div>
      </main>
    );
  }

  const lastRun = runs.length ? runs[0] : null;
  const archivedCount = movies.filter((m) => m.status === "ARCHIVED").length;
  const activeCount = movies.length - archivedCount;
  const protectedCount = movies.filter((m) => m.adminProtected).length;
  const excludedCount = movies.filter((m) => m.excludedFromAutoShows).length;
  const tmdbCount = movies.filter((m) => m.source === "TMDB").length;
  const manualCount = movies.length - tmdbCount;

  return (
    <main className="min-h-screen bg-[#05070b] px-5 pb-20 pt-28 sm:px-8 lg:px-12">
      <div className="mx-auto max-w-[1440px]">
        <div className="flex flex-wrap items-end justify-between gap-3">
          <Title text1="Automation" text2="Dashboard" />
          <div className="flex flex-wrap gap-2">
            <Button onClick={load} disabled={busy}>
              <RefreshCw size={13} />
              Refresh
            </Button>
            <Button
              onClick={handleRun}
              variant="primary"
              disabled={busy || config?.schedulerEnabled === false}
            >
              <Play size={13} />
              Run automation
            </Button>
          </div>
        </div>

        {toast && (
          <div className="mt-4 rounded-xl border border-cyan-300/25 bg-cyan-300/[.07] px-4 py-3 text-sm text-cyan-100">
            {toast}
          </div>
        )}

        {config?.schedulerEnabled === false && (
          <div className="mt-4 rounded-xl border border-amber-300/25 bg-amber-300/[.07] px-4 py-3 text-sm text-amber-100/80">
            The daily automation scheduler is disabled
            (SHOW_SCHEDULER_ENABLED=false). Manual runs are not available.
            Show generation, TMDB sync and movie retirement are all paused.
          </div>
        )}

        {/* =====================================================
            OVERVIEW
        ===================================================== */}

        <Panel
          title="Automation overview"
          subtitle="Latest execution and aggregate counters"
          className="mt-6"
        >
          <div className="grid gap-4 sm:grid-cols-2 xl:grid-cols-4">
            <StatCard
              label="Last run"
              value={lastRun ? new Date(lastRun.startedAt).toLocaleString() : "Never"}
              icon={Clock3}
            />
            <StatCard
              label="Last outcome"
              value={
                lastRun ? (
                  <StatusPill status={lastRun.outcome} />
                ) : (
                  "—"
                )
              }
              icon={Activity}
            />
            <StatCard
              label="Movies archived"
              value={status?.archivedMovies ?? archivedCount}
              icon={Archive}
            />
            <StatCard
              label="Sync errors"
              value={status?.moviesWithSyncError ?? 0}
              icon={AlertCircle}
              tone="text-red-300"
            />
          </div>
        </Panel>

        {/* =====================================================
            TMDB SYNC
        ===================================================== */}

        <Panel
          title="TMDB catalogue synchronization"
          subtitle="Indian theatrical release dates only (region IN)"
          className="mt-6"
        >
          <div className="grid gap-4 sm:grid-cols-2 xl:grid-cols-4">
            <StatCard label="Total movies" value={status?.totalMovies ?? movies.length} icon={Film} />
            <StatCard label="TMDB-managed" value={status?.tmdbManagedMovies ?? tmdbCount} icon={Film} tone="text-cyan-300" />
            <StatCard label="Manual" value={status?.manualMovies ?? manualCount} icon={Film} tone="text-white/40" />
            <StatCard label="Without TMDB id" value={status?.moviesWithoutTmdbId ?? 0} icon={Film} tone="text-amber-300" />
          </div>

          {status?.duplicateTmdbIds?.length > 0 && (
            <div className="mt-4 rounded-xl border border-amber-300/25 bg-amber-300/[.07] px-4 py-3 text-xs text-amber-100/80">
              <p className="font-semibold text-amber-100">
                Duplicate TMDB identifiers detected
              </p>
              <p className="mt-1">
                {status.duplicateTmdbIds.join(", ")} — these are reported only.
                They are never merged or deleted automatically.
              </p>
            </div>
          )}

          {status?.failedMovies?.length > 0 && (
            <div className="mt-4 rounded-xl border border-red-400/25 bg-red-400/[.07] px-4 py-3 text-xs text-red-100/80">
              <p className="font-semibold text-red-100">
                Synchronization errors
              </p>
              <ul className="mt-2 space-y-1">
                {status.failedMovies.map((m) => (
                  <li key={m.id}>
                    {m.title} (#{m.id}): {m.lastSyncError}
                  </li>
                ))}
              </ul>
            </div>
          )}

          {config?.tmdbSyncEnabled === false && (
            <div className="mt-4 rounded-xl border border-amber-300/25 bg-amber-300/[.07] px-4 py-3 text-xs text-amber-100/80">
              TMDB synchronization is disabled by backend configuration
              (MOVIE_SYNC_ENABLED=false). Manual synchronization is not
              available.
            </div>
          )}

          <div className="mt-5 flex flex-wrap gap-2">
            <Button
              onClick={handleSync}
              variant="primary"
              disabled={busy || config?.tmdbSyncEnabled === false}
            >
              <RefreshCw size={13} />
              Synchronize now
            </Button>
            <p className="self-center text-[11px] text-white/35">
              Uses the existing daily automation endpoint. A confirmation is
              required.
            </p>
          </div>
        </Panel>

        {/* =====================================================
            MOVIE LIFECYCLE
        ===================================================== */}

        <Panel
          title="Movie lifecycle and retirement"
          subtitle="60-day retirement with safety holds"
          className="mt-6"
        >
          {config?.retirementEnabled === false && (
            <div className="mb-4 rounded-xl border border-amber-300/25 bg-amber-300/[.07] px-4 py-3 text-xs text-amber-100/80">
              Automatic movie retirement is disabled by backend configuration
              (MOVIE_RETIREMENT_ENABLED=false). Manual archive remains
              available.
            </div>
          )}

          <div className="grid gap-4 sm:grid-cols-2 xl:grid-cols-4">
            <StatCard label="Active movies" value={activeCount} icon={Film} tone="text-emerald-300" />
            <StatCard label="Archived movies" value={archivedCount} icon={Archive} tone="text-white/40" />
            <StatCard label="Protected" value={protectedCount} icon={ShieldCheck} tone="text-amber-300" />
            <StatCard label="Excluded from shows" value={excludedCount} icon={XCircle} tone="text-amber-300" />
          </div>

          <div className="mt-5 overflow-x-auto">
            <table className="w-full text-left text-xs">
              <thead>
                <tr className="border-b border-white/10 text-white/35">
                  <th className="py-2 pr-4 font-medium">Movie</th>
                  <th className="py-2 pr-4 font-medium">Status</th>
                  <th className="py-2 pr-4 font-medium">Source</th>
                  <th className="py-2 pr-4 font-medium">Hold reason</th>
                  <th className="py-2 pr-4 font-medium">Actions</th>
                </tr>
              </thead>
              <tbody>
                {movies.map((movie) => (
                  <tr
                    key={movie.id}
                    className="border-b border-white/5 text-white/70"
                  >
                    <td className="py-2 pr-4 font-medium text-white/85">
                      {movie.title}
                      <span className="ml-2 text-[10px] text-white/30">
                        #{movie.id}
                      </span>
                    </td>
                    <td className="py-2 pr-4">
                      <StatusPill
                        status={
                          movie.status === "ARCHIVED" ? "SKIPPED" : "SUCCESS"
                        }
                      />
                      <span className="ml-2 text-[10px] text-white/40">
                        {movie.status}
                      </span>
                    </td>
                    <td className="py-2 pr-4 text-white/45">
                      {movie.source}
                    </td>
                    <td className="py-2 pr-4 text-white/40">
                      {movie.retirementHoldReason || "—"}
                    </td>
                    <td className="py-2 pr-4">
                      <div className="flex flex-wrap gap-1.5">
                        {movie.status === "ARCHIVED" ? (
                          <Button
                            onClick={() =>
                              handleMovieAction("reactivate", movie)
                            }
                            disabled={busy}
                          >
                            Reactivate
                          </Button>
                        ) : (
                          <Button
                            onClick={() =>
                              handleMovieAction("archive", movie)
                            }
                            variant="danger"
                            disabled={busy}
                          >
                            Archive
                          </Button>
                        )}
                        <Button
                          onClick={() =>
                            handleMovieAction("protect", movie)
                          }
                          disabled={busy}
                        >
                          {movie.adminProtected
                            ? "Unprotect"
                            : "Protect"}
                        </Button>
                        <Button
                          onClick={() =>
                            handleMovieAction("exclude", movie)
                          }
                          disabled={busy}
                        >
                          {movie.excludedFromAutoShows
                            ? "Enable shows"
                            : "Exclude shows"}
                        </Button>
                        <Button
                          onClick={() =>
                            handleMovieAction("extend", movie)
                          }
                          disabled={busy}
                        >
                          Extend
                        </Button>
                      </div>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>

            {movies.length === 0 && (
              <p className="mt-4 text-sm text-white/40">
                No movies in the catalogue yet.
              </p>
            )}
          </div>
        </Panel>

        {/* =====================================================
            AUTOMATION HISTORY
        ===================================================== */}

        <Panel
          title="Automation execution history"
          subtitle="Last 50 runs"
          className="mt-6"
        >
          {runs.length === 0 ? (
            <p className="text-sm text-white/40">
              No automation runs recorded yet.
            </p>
          ) : (
            <div className="overflow-x-auto">
              <table className="w-full text-left text-xs">
                <thead>
                  <tr className="border-b border-white/10 text-white/35">
                    <th className="py-2 pr-4 font-medium">ID</th>
                    <th className="py-2 pr-4 font-medium">Trigger</th>
                    <th className="py-2 pr-4 font-medium">Started</th>
                    <th className="py-2 pr-4 font-medium">Outcome</th>
                    <th className="py-2 pr-4 font-medium">Movies</th>
                    <th className="py-2 pr-4 font-medium">Shows</th>
                    <th className="py-2 pr-4 font-medium">Message</th>
                  </tr>
                </thead>
                <tbody>
                  {runs.map((run) => (
                    <tr
                      key={run.id}
                      className="border-b border-white/5 text-white/70"
                    >
                      <td className="py-2 pr-4 font-medium text-white/85">
                        {run.id}
                      </td>
                      <td className="py-2 pr-4">{run.triggerType}</td>
                      <td className="py-2 pr-4 text-white/45">
                        {run.startedAt
                          ? new Date(run.startedAt).toLocaleString()
                          : "—"}
                      </td>
                      <td className="py-2 pr-4">
                        <StatusPill status={run.outcome} />
                      </td>
                      <td className="py-2 pr-4">
                        +{run.moviesInserted} / ~{run.moviesUpdated} /{" "}
                        {run.moviesSkipped} skipped
                      </td>
                      <td className="py-2 pr-4">
                        {run.showsCreated} created /{" "}
                        {run.showsSkipped} skipped /{" "}
                        {run.showsFailed} failed
                      </td>
                      <td className="py-2 pr-4 text-white/40">
                        {run.message || "—"}
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
        </Panel>

        {/* =====================================================
            SEVEN-DAY SHOW GENERATION
        ===================================================== */}

        <Panel
          title="Seven-day show generation"
          subtitle="Rolling window: today + next 6 dates"
          className="mt-6"
        >
          <div className="grid gap-4 sm:grid-cols-2 xl:grid-cols-4">
            <StatCard label="Last run created" value={lastRun?.showsCreated ?? 0} icon={CalendarDays} tone="text-cyan-300" />
            <StatCard label="Last run skipped" value={lastRun?.showsSkipped ?? 0} icon={Clock3} tone="text-white/40" />
            <StatCard label="Last run failed" value={lastRun?.showsFailed ?? 0} icon={XCircle} tone="text-red-300" />
            <StatCard label="Duration (s)" value={lastRun?.durationSeconds ?? 0} icon={Activity} tone="text-white/40" />
          </div>

          <p className="mt-4 text-xs text-white/35">
            The scheduler runs every day at 2:00 AM IST. Shows are generated
            for active, non-excluded movies with a confirmed Indian theatrical
            release date that has already passed. Existing shows are never
            regenerated or deleted.
          </p>
        </Panel>

        {/* =====================================================
            CONFIGURATION
        ===================================================== */}

        <Panel
          title="Automation configuration"
          subtitle="Backend-controlled; changes require Render env vars"
          className="mt-6"
        >
          <div className="grid gap-3 sm:grid-cols-2 xl:grid-cols-3">
            <StatusRow
              label="Scheduler"
              value={config?.schedulerEnabled ? "Enabled" : "Disabled"}
              active={config?.schedulerEnabled}
            />
            <StatusRow
              label="TMDB sync"
              value={config?.tmdbSyncEnabled ? "Enabled" : "Disabled"}
              active={config?.tmdbSyncEnabled}
            />
            <StatusRow
              label="Movie retirement"
              value={config?.retirementEnabled ? "Enabled" : "Disabled"}
              active={config?.retirementEnabled}
            />
            <StatusRow
              label="Show cleanup"
              value={config?.cleanupEnabled ? "Enabled" : "Disabled"}
              active={config?.cleanupEnabled}
            />
            <StatusRow
              label="Ticket backfill"
              value={config?.ticketBackfillEnabled ? "Enabled" : "Disabled"}
              active={config?.ticketBackfillEnabled}
            />
            <StatusRow
              label="Horizon window"
              value={`${config?.horizonDays ?? 7} calendar dates`}
              active={true}
            />
            <StatusRow
              label="Timezone"
              value={config?.timezone || "Asia/Kolkata"}
              active={true}
            />
          </div>
          <p className="mt-4 text-[11px] text-white/30">
            To disable any feature in production, set the corresponding
            Render environment variable to false:{" "}
            <code className="text-cyan-300/70">SHOW_SCHEDULER_ENABLED</code>,{" "}
            <code className="text-cyan-300/70">MOVIE_SYNC_ENABLED</code>,{" "}
            <code className="text-cyan-300/70">MOVIE_RETIREMENT_ENABLED</code>,{" "}
            <code className="text-cyan-300/70">SHOW_CLEANUP_ENABLED</code>.
          </p>
        </Panel>
      </div>
    </main>
  );
}

function StatusRow({ label, value, active = true }) {
  return (
    <div className="flex items-center justify-between rounded-xl border border-white/5 bg-white/[.03] px-4 py-3">
      <span className="text-sm text-white/45">{label}</span>
      <span
        className={`flex items-center gap-2 text-xs ${
          active ? "text-green-300" : "text-red-300"
        }`}
      >
        <span
          className={`h-1.5 w-1.5 rounded-full ${
            active ? "bg-green-300" : "bg-red-300"
          }`}
        />
        {value}
      </span>
    </div>
  );
}
