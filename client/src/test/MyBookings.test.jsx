import React from "react";
import { describe, expect, it, vi, beforeEach } from "vitest";
import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { MemoryRouter, Route, Routes } from "react-router-dom";

import MyBookings from "../pages/MyBookings";
import { AuthProvider } from "../context/AuthContext";

vi.mock("../lib/api", async () => {
  const actual = await vi.importActual("../lib/api");

  return {
    ...actual,
    getMyBookings: vi.fn(),
  };
});

vi.mock("../lib/tickets", async () => {
  const actual = await vi.importActual("../lib/tickets");

  return {
    ...actual,
    getMyTickets: vi.fn(),
  };
});

import { getMyBookings } from "../lib/api";
import { getMyTickets } from "../lib/tickets";

const BOOKING = {
  id: 7,
  status: "CONFIRMED",
  paymentStatus: "PAID",
  seatNumbers: ["S1", "S2"],
  numberOfSeats: 2,
  totalAmount: 500,
  bookingDate: "2026-11-15T10:00:00",
  show: {
    showDate: "2026-11-20",
    showTime: "19:30:00",
    movie: { title: "Interstellar", posterUrl: null },
    theatre: { name: "PVR Nexus", location: { city: "Hyderabad" } },
  },
};

function renderBookings() {
  localStorage.setItem("cinepassToken", "test-token");
  localStorage.setItem(
    "cinepassUser",
    JSON.stringify({ id: 1, name: "Asha Rao", role: "USER" })
  );

  return render(
    <MemoryRouter initialEntries={["/My-Bookings"]}>
      <AuthProvider>
        <Routes>
          <Route path="/My-Bookings" element={<MyBookings />} />
          <Route path="/tickets/:ticketId" element={<div>Ticket route</div>} />
        </Routes>
      </AuthProvider>
    </MemoryRouter>
  );
}

describe("My Bookings ticket navigation", () => {
  beforeEach(() => {
    getMyBookings.mockResolvedValue([BOOKING]);
    getMyTickets.mockResolvedValue([
      {
        ticketId: 42,
        bookingId: 7,
        status: "BOOKED",
        ticketReference: "SRH-ABCDEFGHJKLM",
      },
    ]);
  });

  it("shows ticket status and navigates to the digital ticket", async () => {
    const user = userEvent.setup();

    renderBookings();

    expect(await screen.findByText("Interstellar")).toBeInTheDocument();
    expect(screen.getByText("A1, A2")).toBeInTheDocument();
    expect(screen.getByText("Valid for entry")).toBeInTheDocument();

    await user.click(screen.getByRole("button", { name: /view ticket/i }));

    expect(await screen.findByText("Ticket route")).toBeInTheDocument();
  });

  it("does not show a ticket button when the booking has no issued ticket", async () => {
    getMyTickets.mockResolvedValue([]);

    renderBookings();

    expect(await screen.findByText("Interstellar")).toBeInTheDocument();
    expect(
      screen.queryByRole("button", { name: /view ticket/i })
    ).not.toBeInTheDocument();
  });
});
