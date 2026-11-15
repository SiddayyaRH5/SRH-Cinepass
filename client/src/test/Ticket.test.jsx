import React from "react";
import { describe, expect, it, vi, beforeEach } from "vitest";
import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { MemoryRouter, Route, Routes } from "react-router-dom";

import Ticket from "../pages/Ticket";
import { AuthProvider } from "../context/AuthContext";

vi.mock("../lib/tickets", async () => {
  const actual = await vi.importActual("../lib/tickets");

  return {
    ...actual,
    getTicket: vi.fn(),
    fetchTicketQrUrl: vi.fn(),
  };
});

import { fetchTicketQrUrl, getTicket } from "../lib/tickets";

const TICKET = {
  ticketId: 42,
  bookingId: 7,
  ticketReference: "SRH-ABCDEFGHJKLM",
  status: "BOOKED",
  bookingStatus: "CONFIRMED",
  movieTitle: "Interstellar",
  moviePosterUrl: null,
  customerName: "Asha Rao",
  theatreName: "PVR Nexus",
  theatreCity: "Hyderabad",
  showDate: "2026-11-20",
  showTime: "19:30:00",
  seatNumbers: ["S1", "S2"],
  quantity: 2,
  bookingTimestamp: "2026-11-15T10:00:00",
  issuedAt: "2026-11-15T10:00:01",
};

function renderTicket() {
  localStorage.setItem("cinepassToken", "test-token");
  localStorage.setItem(
    "cinepassUser",
    JSON.stringify({ id: 1, name: "Asha Rao", role: "USER" })
  );

  return render(
    <MemoryRouter initialEntries={["/tickets/42"]}>
      <AuthProvider>
        <Routes>
          <Route path="/tickets/:ticketId" element={<Ticket />} />
          <Route path="/My-Bookings" element={<div>My bookings page</div>} />
        </Routes>
      </AuthProvider>
    </MemoryRouter>
  );
}

describe("Digital ticket page", () => {
  beforeEach(() => {
    getTicket.mockResolvedValue(TICKET);
    fetchTicketQrUrl.mockResolvedValue("blob:test-qr");
    window.print = vi.fn();
  });

  it("renders the ticket details and the backend QR image", async () => {
    renderTicket();

    expect(
      await screen.findByRole("heading", { name: "Interstellar" })
    ).toBeInTheDocument();

    expect(screen.getByText("PVR Nexus · Hyderabad")).toBeInTheDocument();
    expect(screen.getByText("A1, A2")).toBeInTheDocument();
    expect(screen.getByText("Asha Rao")).toBeInTheDocument();
    expect(screen.getAllByText("SRH-ABCDEFGHJKLM").length).toBeGreaterThan(0);
    expect(screen.getByText("Valid for entry")).toBeInTheDocument();

    const qr = await screen.findByAltText("SRH CinePass ticket QR code");
    expect(qr).toHaveAttribute("src", "blob:test-qr");
  });

  it("opens the browser print dialog for download / print", async () => {
    const user = userEvent.setup();

    renderTicket();

    await screen.findByRole("heading", { name: "Interstellar" });
    await screen.findByAltText("SRH CinePass ticket QR code");

    await user.click(
      screen.getByRole("button", { name: /download \/ print/i })
    );

    expect(window.print).toHaveBeenCalledTimes(1);
  });

  it("shows an error state when the ticket cannot be loaded", async () => {
    getTicket.mockRejectedValue(new Error("You do not have access to this ticket"));

    renderTicket();

    expect(await screen.findByText("Ticket unavailable")).toBeInTheDocument();
    expect(
      screen.getByText("You do not have access to this ticket")
    ).toBeInTheDocument();
  });
});
