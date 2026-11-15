import React from "react";
import { describe, expect, it, vi, beforeEach } from "vitest";
import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { MemoryRouter, Route, Routes } from "react-router-dom";

import MovieDetails from "../pages/MovieDetails";
import { LocationProvider } from "../lib/LocationContext";

vi.mock("../lib/api", async () => {
  const actual = await vi.importActual("../lib/api");

  return {
    ...actual,
    getMovie: vi.fn(),
    getMovieCities: vi.fn(),
    getMovieTheatres: vi.fn(),
    getMovieShowDates: vi.fn(),
    getMovieShowTimes: vi.fn(),
  };
});

import {
  getMovie,
  getMovieCities,
  getMovieShowDates,
  getMovieShowTimes,
  getMovieTheatres,
} from "../lib/api";

const MOVIE = {
  id: 1,
  title: "Interstellar",
  rating: 8.7,
  description: "A team travels through a wormhole.",
  duration: 169,
  language: "English",
};

function renderMovie() {
  return render(
    <MemoryRouter initialEntries={["/movies/1"]}>
      <LocationProvider>
        <Routes>
          <Route path="/movies/:id" element={<MovieDetails />} />
          <Route path="/show/:id" element={<div>Seat layout route</div>} />
        </Routes>
      </LocationProvider>
    </MemoryRouter>
  );
}

describe("Movie details booking flow", () => {
  beforeEach(() => {
    localStorage.clear();

    getMovie.mockResolvedValue(MOVIE);
    getMovieCities.mockResolvedValue([
      { id: 10, city: "Hyderabad", state: "Telangana", country: "India" },
    ]);
    getMovieTheatres.mockResolvedValue([
      { id: 20, name: "PVR Nexus", city: "Hyderabad", totalSeats: 120 },
    ]);
    getMovieShowDates.mockResolvedValue([{ date: "2026-11-20" }]);
    getMovieShowTimes.mockResolvedValue([
      {
        id: 99,
        showDate: "2026-11-20",
        showTime: "19:30:00",
        ticketPrice: 250,
        totalSeats: 120,
        availableSeats: 42,
      },
    ]);
  });

  it("walks city -> theatre -> date -> showtime and opens the seat map", async () => {
    const user = userEvent.setup();

    renderMovie();

    expect(
      await screen.findByRole("button", { name: "Hyderabad" })
    ).toBeInTheDocument();

    expect(await screen.findByText("PVR Nexus")).toBeInTheDocument();

    expect(await screen.findByText(/20 Nov/)).toBeInTheDocument();

    const showtime = await screen.findByRole("button", {
      name: /7:30 PM/,
    });

    expect(showtime).toHaveTextContent("42 left");
    expect(showtime).toHaveTextContent("₹250");

    await user.click(showtime);

    expect(await screen.findByText("Seat layout route")).toBeInTheDocument();
  });

  it("shows an empty state when the movie has no bookable shows", async () => {
    getMovieCities.mockResolvedValue([]);

    renderMovie();

    expect(
      await screen.findByText(/No shows are scheduled for this movie yet/i)
    ).toBeInTheDocument();
  });

  it("shows an error state with a retry action when cities fail to load", async () => {
    getMovieCities.mockRejectedValue(new Error("Unable to reach the server"));

    renderMovie();

    expect(
      await screen.findByText("Unable to reach the server")
    ).toBeInTheDocument();

    expect(
      screen.getByRole("button", { name: /retry/i })
    ).toBeInTheDocument();
  });

  it("marks a sold-out showtime as disabled instead of bookable", async () => {
    getMovieShowTimes.mockResolvedValue([
      {
        id: 100,
        showDate: "2026-11-20",
        showTime: "22:00:00",
        ticketPrice: 250,
        totalSeats: 120,
        availableSeats: 0,
      },
    ]);

    renderMovie();

    const showtime = await screen.findByRole("button", {
      name: /10:00 PM/,
    });

    expect(showtime).toBeDisabled();
    expect(showtime).toHaveTextContent("Sold out");
  });
});
