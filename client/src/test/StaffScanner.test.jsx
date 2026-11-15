import React from "react";
import { describe, expect, it, vi, beforeEach } from "vitest";
import { fireEvent, render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { MemoryRouter } from "react-router-dom";

import StaffScanner from "../pages/Staff/StaffScanner";
import { AuthProvider } from "../context/AuthContext";

const scannerStart = vi.fn();
const scannerStop = vi.fn(async () => {});
const scannerClear = vi.fn();
const scannerPause = vi.fn();
const scannerResume = vi.fn();

vi.mock("html5-qrcode", () => {
  class Html5Qrcode {
    start(...args) {
      return scannerStart(...args);
    }

    stop() {
      return scannerStop();
    }

    clear() {
      return scannerClear();
    }

    pause() {
      return scannerPause();
    }

    resume() {
      return scannerResume();
    }
  }

  return { Html5Qrcode, default: { Html5Qrcode } };
});

vi.mock("../lib/tickets", async () => {
  const actual = await vi.importActual("../lib/tickets");

  return {
    ...actual,
    verifyTicket: vi.fn(),
  };
});

import { verifyTicket } from "../lib/tickets";

const VALID_TOKEN = "A".repeat(43);

function renderScanner(role = "STAFF") {
  localStorage.setItem("cinepassToken", "test-token");
  localStorage.setItem(
    "cinepassUser",
    JSON.stringify({ id: 9, name: "Sam Staff", role })
  );

  return render(
    <MemoryRouter>
      <AuthProvider>
        <StaffScanner />
      </AuthProvider>
    </MemoryRouter>
  );
}

async function submitManualToken(user, token = VALID_TOKEN) {
  fireEvent.change(
    screen.getByPlaceholderText(/paste the verification token/i),
    { target: { value: token } }
  );

  await user.click(screen.getByRole("button", { name: /verify ticket/i }));
}

describe("Staff scanner", () => {
  beforeEach(() => {
    verifyTicket.mockReset();
    scannerStart.mockReset();
    scannerStop.mockClear();
    scannerClear.mockClear();
    scannerPause.mockClear();
    scannerResume.mockClear();
  });

  it("blocks customers from the staff scanner UI", () => {
    renderScanner("USER");

    expect(screen.getByText("Staff access required")).toBeInTheDocument();
    expect(
      screen.queryByRole("button", { name: /start camera/i })
    ).not.toBeInTheDocument();
  });

  it("shows a clear camera permission error", async () => {
    const user = userEvent.setup();

    Object.defineProperty(navigator, "mediaDevices", {
      configurable: true,
      value: { getUserMedia: vi.fn() },
    });

    scannerStart.mockRejectedValue(
      Object.assign(new Error("Permission denied"), {
        name: "NotAllowedError",
      })
    );

    renderScanner("STAFF");

    await user.click(screen.getByRole("button", { name: /start camera/i }));

    expect(
      await screen.findByText(/camera permission was denied/i)
    ).toBeInTheDocument();
    expect(verifyTicket).not.toHaveBeenCalled();
  });

  it("verifies a manually entered token only through the backend", async () => {
    const user = userEvent.setup();

    verifyTicket.mockResolvedValue({
      result: "VALID",
      message: "Ticket Verified Successfully",
      ticketReference: "SRH-ABCDEFGHJKLM",
      movieTitle: "Interstellar",
      theatreName: "PVR Nexus",
      showDate: "2026-11-20",
      showTime: "19:30:00",
      seatNumbers: ["A1", "A2"],
      customerName: "Asha Rao",
      verifiedAt: "2026-11-20T19:10:00",
      verifiedBy: "Sam Staff",
    });

    renderScanner("STAFF");

    await submitManualToken(user);

    expect(
      await screen.findByText("Ticket Verified Successfully")
    ).toBeInTheDocument();
    expect(verifyTicket).toHaveBeenCalledWith(VALID_TOKEN);
    expect(screen.getByText("SRH-ABCDEFGHJKLM")).toBeInTheDocument();
  });

  it("shows an invalid result returned by the backend", async () => {
    const user = userEvent.setup();

    verifyTicket.mockResolvedValue({
      result: "INVALID",
      message: "Invalid Ticket",
    });

    renderScanner("STAFF");

    await submitManualToken(user);

    expect(await screen.findByText("Invalid Ticket")).toBeInTheDocument();
  });

  it("shows an already-used result without admitting the ticket", async () => {
    const user = userEvent.setup();

    verifyTicket.mockResolvedValue({
      result: "ALREADY_USED",
      message: "Ticket Already Used",
      ticketReference: "SRH-ABCDEFGHJKLM",
    });

    renderScanner("STAFF");

    await submitManualToken(user);

    expect(await screen.findByText("Ticket Already Used")).toBeInTheDocument();
    expect(
      screen.queryByText(/admission recorded atomically/i)
    ).not.toBeInTheDocument();
  });

  it("rejects a malformed token locally without calling the API", async () => {
    const user = userEvent.setup();

    renderScanner("STAFF");

    await submitManualToken(user, "not-a-valid-token");

    expect(await screen.findByText("Invalid Ticket")).toBeInTheDocument();
    expect(verifyTicket).not.toHaveBeenCalled();
  });

  it("handles an unauthorized backend response", async () => {
    const user = userEvent.setup();

    verifyTicket.mockRejectedValue(
      Object.assign(new Error("Forbidden"), { status: 403 })
    );

    renderScanner("STAFF");

    await submitManualToken(user);

    expect(
      await screen.findByText(/staff access required/i)
    ).toBeInTheDocument();
  });
});
