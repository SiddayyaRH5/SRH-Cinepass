import React from "react";
import { describe, expect, it, vi, beforeEach } from "vitest";
import { render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { MemoryRouter } from "react-router-dom";

import Automation from "../pages/Admin/Automation";

vi.mock("../lib/api", async () => {
  const actual = await vi.importActual("../lib/api");

  return {
    ...actual,
    getAdminMovies: vi.fn(),
    getAdminSyncStatus: vi.fn(),
    getAutomationConfig: vi.fn(),
    getAutomationRuns: vi.fn(),
    runAutomation: vi.fn(),
    excludeMovie: vi.fn(),
    protectMovie: vi.fn(),
    extendMovie: vi.fn(),
    reactivateMovie: vi.fn(),
    archiveMovie: vi.fn(),
  };
});

import {
  getAdminMovies,
  getAdminSyncStatus,
  getAutomationConfig,
  getAutomationRuns,
  runAutomation,
  protectMovie,
  reactivateMovie,
  archiveMovie,
} from "../lib/api";

const CONFIG_ENABLED = {
  schedulerEnabled: true,
  cleanupEnabled: true,
  tmdbSyncEnabled: true,
  retirementEnabled: true,
  ticketBackfillEnabled: true,
  horizonDays: 7,
  timezone: "Asia/Kolkata",
};

const CONFIG_DISABLED = {
  schedulerEnabled: false,
  cleanupEnabled: false,
  tmdbSyncEnabled: false,
  retirementEnabled: false,
  ticketBackfillEnabled: false,
  horizonDays: 7,
  timezone: "Asia/Kolkata",
};

const SYNC_STATUS = {
  totalMovies: 10,
  tmdbManagedMovies: 6,
  manualMovies: 4,
  archivedMovies: 2,
  excludedMovies: 1,
  moviesWithoutTmdbId: 3,
  duplicateTmdbIds: [],
  moviesWithSyncError: 0,
  failedMovies: [],
};

const MOVIES = [
  {
    id: 1,
    title: "Active Movie",
    status: "ACTIVE",
    source: "TMDB",
    adminProtected: false,
    excludedFromAutoShows: false,
    retirementHoldReason: null,
  },
  {
    id: 2,
    title: "Archived Movie",
    status: "ARCHIVED",
    source: "TMDB",
    adminProtected: false,
    excludedFromAutoShows: false,
    retirementHoldReason: null,
  },
];

const RUN = {
  id: 7,
  triggerType: "SCHEDULED",
  startedAt: "2026-11-15T02:00:00",
  outcome: "SUCCESS",
  moviesInserted: 2,
  moviesUpdated: 3,
  moviesSkipped: 1,
  showsCreated: 28,
  showsSkipped: 4,
  showsFailed: 0,
  durationSeconds: 12,
  message: "completed",
};

function renderAutomation() {
  return render(
    <MemoryRouter initialEntries={["/admin/automation"]}>
      <Automation />
    </MemoryRouter>
  );
}

describe("Admin automation dashboard", () => {
  beforeEach(() => {
    localStorage.clear();

    getAdminSyncStatus.mockResolvedValue(SYNC_STATUS);
    getAutomationConfig.mockResolvedValue(CONFIG_ENABLED);
    getAutomationRuns.mockResolvedValue([RUN]);
    getAdminMovies.mockResolvedValue(MOVIES);
  });

  it("renders the dashboard overview and panels", async () => {
    renderAutomation();

    expect(
      await screen.findByText("Automation")
    ).toBeInTheDocument();

    expect(await screen.findByText("Total movies")).toBeInTheDocument();
    expect(screen.getByText("Active movies")).toBeInTheDocument();
    expect(screen.getByText("Automation execution history")).toBeInTheDocument();
  });

  it("shows a loading state first", () => {
    getAdminSyncStatus.mockReturnValue(new Promise(() => {}));
    getAutomationConfig.mockReturnValue(new Promise(() => {}));
    getAutomationRuns.mockReturnValue(new Promise(() => {}));
    getAdminMovies.mockReturnValue(new Promise(() => {}));

    renderAutomation();

    expect(screen.getByText(/Loading automation status/i)).toBeInTheDocument();
  });

  it("shows an error state with a retry action when loading fails", async () => {
    getAdminSyncStatus.mockRejectedValue(new Error("Unable to reach the server"));

    renderAutomation();

    expect(
      await screen.findByText("Unable to reach the server")
    ).toBeInTheDocument();

    expect(
      screen.getByRole("button", { name: /retry/i })
    ).toBeInTheDocument();
  });

  it("retries loading when the retry button is clicked", async () => {
    const user = userEvent.setup();

    getAdminSyncStatus
      .mockRejectedValueOnce(new Error("boom"))
      .mockResolvedValue(SYNC_STATUS);

    renderAutomation();

    expect(await screen.findByText("boom")).toBeInTheDocument();

    await user.click(screen.getByRole("button", { name: /retry/i }));

    await waitFor(() => {
      expect(screen.getByText("Total movies")).toBeInTheDocument();
    });
  });

  it("displays the actual disabled configuration state", async () => {
    getAutomationConfig.mockResolvedValue(CONFIG_DISABLED);

    renderAutomation();

    expect(
      await screen.findByText(/daily automation scheduler is disabled/i)
    ).toBeInTheDocument();

    expect(screen.getAllByText("Disabled").length).toBeGreaterThan(0);
  });

  it("shows the disabled TMDB sync notice when sync is off", async () => {
    getAutomationConfig.mockResolvedValue({
      ...CONFIG_ENABLED,
      tmdbSyncEnabled: false,
    });

    renderAutomation();

    expect(
      await screen.findByText(/TMDB synchronization is disabled/i)
    ).toBeInTheDocument();
  });

  it("shows the disabled retirement notice when retirement is off", async () => {
    getAutomationConfig.mockResolvedValue({
      ...CONFIG_ENABLED,
      retirementEnabled: false,
    });

    renderAutomation();

    expect(
      await screen.findByText(/Automatic movie retirement is disabled/i)
    ).toBeInTheDocument();
  });

  it("requires confirmation before triggering automation", async () => {
    const user = userEvent.setup();
    const confirmSpy = vi.spyOn(window, "confirm").mockReturnValue(false);

    renderAutomation();

    await screen.findByText("Total movies");

    await user.click(screen.getByRole("button", { name: /run automation/i }));

    expect(runAutomation).not.toHaveBeenCalled();
    confirmSpy.mockRestore();
  });

  it("triggers automation after confirmation", async () => {
    const user = userEvent.setup();
    const confirmSpy = vi.spyOn(window, "confirm").mockReturnValue(true);

    runAutomation.mockResolvedValue({ outcome: "SUCCESS" });

    renderAutomation();

    await screen.findByText("Total movies");

    await user.click(screen.getByRole("button", { name: /run automation/i }));

    await waitFor(() => {
      expect(runAutomation).toHaveBeenCalled();
    });

    confirmSpy.mockRestore();
  });

  it("requires confirmation before archiving a movie", async () => {
    const user = userEvent.setup();
    const confirmSpy = vi.spyOn(window, "confirm").mockReturnValue(false);

    renderAutomation();

    await screen.findByText("Active Movie");

    const archiveButtons = screen.getAllByRole("button", { name: /archive/i });
    await user.click(archiveButtons[0]);

    expect(archiveMovie).not.toHaveBeenCalled();
    confirmSpy.mockRestore();
  });

  it("reactivates an archived movie after clicking reactivate", async () => {
    const user = userEvent.setup();

    reactivateMovie.mockResolvedValue({});

    renderAutomation();

    await screen.findByText("Archived Movie");

    const reactivateButtons = screen.getAllByRole("button", {
      name: /reactivate/i,
    });
    await user.click(reactivateButtons[0]);

    await waitFor(() => {
      expect(reactivateMovie).toHaveBeenCalledWith(2);
    });
  });

  it("protects a movie when protect is clicked", async () => {
    const user = userEvent.setup();

    protectMovie.mockResolvedValue({});

    renderAutomation();

    await screen.findByText("Active Movie");

    const protectButtons = screen.getAllByRole("button", {
      name: /protect/i,
    });
    await user.click(protectButtons[0]);

    await waitFor(() => {
      expect(protectMovie).toHaveBeenCalled();
    });
  });

  it("shows an empty history state", async () => {
    getAutomationRuns.mockResolvedValue([]);

    renderAutomation();

    expect(
      await screen.findByText(/No automation runs recorded yet/i)
    ).toBeInTheDocument();
  });

  it("shows the disabled run button when scheduler is off", async () => {
    getAutomationConfig.mockResolvedValue(CONFIG_DISABLED);

    renderAutomation();

    await screen.findByText(/daily automation scheduler is disabled/i);

    const runButton = screen.getByRole("button", { name: /run automation/i });
    expect(runButton).toBeDisabled();
  });

  it("disables the sync button when TMDB sync is off", async () => {
    getAutomationConfig.mockResolvedValue({
      ...CONFIG_ENABLED,
      tmdbSyncEnabled: false,
    });

    renderAutomation();

    await screen.findByText(/TMDB synchronization is disabled/i);

    const syncButtons = screen.getAllByRole("button", {
      name: /synchronize now/i,
    });
    expect(syncButtons[0]).toBeDisabled();
  });
});
