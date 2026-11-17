import React from "react";
import { describe, expect, it, vi, beforeEach } from "vitest";
import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { MemoryRouter, Route, Routes } from "react-router-dom";

import MovieDetails from "../pages/MovieDetails";
import { LocationProvider } from "../lib/LocationContext";
import { formatDate } from "../lib/formatters";

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

  it("renders an embedded YouTube trailer player", async () => {
    getMovie.mockResolvedValue({
      ...MOVIE,
      trailerKey: "abc123",
    });

    renderMovie();

    const playButton = await screen.findByRole("button", {
      name: /play interstellar trailer/i,
    });

    expect(playButton).toBeInTheDocument();

    // Thumbnail image is shown before playback
    expect(
      screen.getByAltText(/interstellar trailer thumbnail/i)
    ).toBeInTheDocument();

    // Clicking play switches to the embedded iframe
    const user = userEvent.setup();
    await user.click(playButton);

    const iframe = await screen.findByTitle(/interstellar trailer/i);
    expect(iframe).toBeInTheDocument();
    expect(iframe.getAttribute("src")).toContain(
      "youtube-nocookie.com/embed/abc123"
    );
  });

  it("shows a YouTube fallback link for the trailer", async () => {
    getMovie.mockResolvedValue({
      ...MOVIE,
      trailerKey: "abc123",
    });

    renderMovie();

    expect(
      await screen.findByRole("link", { name: /open on youtube/i })
    ).toHaveAttribute("href", "https://www.youtube.com/watch?v=abc123");
  });

  it("does not render a trailer section when no trailer exists", async () => {
    getMovie.mockResolvedValue({
      ...MOVIE,
      trailerKey: null,
    });

    renderMovie();

    await screen.findByRole("heading", { name: "Interstellar" });

    expect(
      screen.queryByRole("button", { name: /play.*trailer/i })
    ).not.toBeInTheDocument();

    expect(
      screen.queryByText(/watch trailer/i)
    ).not.toBeInTheDocument();
  });

  it("renders cast cards with actor names and characters", async () => {
    getMovie.mockResolvedValue({
      ...MOVIE,
      castJson: JSON.stringify([
        { name: "Actor One", character: "Hero", profilePath: "/p1.jpg" },
        { name: "Actor Two", character: "Villain", profilePath: "" },
      ]),
    });

    renderMovie();

    expect(await screen.findByText("Actor One")).toBeInTheDocument();
    expect(screen.getByText("Hero")).toBeInTheDocument();
    expect(screen.getByText("Actor Two")).toBeInTheDocument();
    expect(screen.getByText("Villain")).toBeInTheDocument();
    expect(screen.getByAltText("Actor One")).toBeInTheDocument();
  });

  it("shows a no-photo placeholder for cast without profile images", async () => {
    getMovie.mockResolvedValue({
      ...MOVIE,
      castJson: JSON.stringify([
        { name: "No Photo Actor", character: "Extra", profilePath: "" },
      ]),
    });

    renderMovie();

    expect(
      await screen.findByText("No Photo Actor")
    ).toBeInTheDocument();
    expect(screen.getByText("No photo")).toBeInTheDocument();
  });

  it("handles malformed cast JSON gracefully", async () => {
    getMovie.mockResolvedValue({
      ...MOVIE,
      castJson: "not valid json",
    });

    renderMovie();

    expect(
      await screen.findByRole("heading", { name: "Interstellar" })
    ).toBeInTheDocument();

    // No cast section rendered
    expect(screen.queryByText("Top billed cast")).not.toBeInTheDocument();
  });

  it("shows show all toggle when cast exceeds 8 members", async () => {
    const cast = Array.from({ length: 12 }, (_, i) => ({
      name: `Actor ${i}`,
      character: `Role ${i}`,
      profilePath: "",
    }));

    getMovie.mockResolvedValue({
      ...MOVIE,
      castJson: JSON.stringify(cast),
    });

    renderMovie();

    expect(
      await screen.findByText(/show all \(12\)/i)
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

  it("offers every date of the rolling seven day window", async () => {
    const dates = [
      "2026-11-16",
      "2026-11-17",
      "2026-11-18",
      "2026-11-19",
      "2026-11-20",
      "2026-11-21",
      "2026-11-22",
    ];

    getMovieShowDates.mockResolvedValue(
      dates.map((date) => ({ date }))
    );

    renderMovie();

    for (const date of dates) {
      expect(
        await screen.findByRole("button", { name: formatDate(date) })
      ).toBeInTheDocument();
    }

    // The window never shows more than the seven configured dates.
    expect(
      screen.getAllByRole("button", { name: /Nov/ })
    ).toHaveLength(dates.length);
  });

  it("shows an archived notice and no booking flow for a retired movie", async () => {
    getMovie.mockResolvedValue({ ...MOVIE, status: "ARCHIVED" });

    renderMovie();

    expect(
      await screen.findByText(/This movie is no longer in theatres/i)
    ).toBeInTheDocument();

    expect(screen.getByText(/No longer bookable/i)).toBeInTheDocument();

    // The booking steps are not offered for a retired movie.
    expect(screen.queryByText("Book tickets")).not.toBeInTheDocument();
    expect(
      screen.queryByRole("button", { name: "Hyderabad" })
    ).not.toBeInTheDocument();

    // A retired movie never triggers a discovery request.
    expect(getMovieCities).not.toHaveBeenCalled();
    expect(getMovieTheatres).not.toHaveBeenCalled();
    expect(getMovieShowDates).not.toHaveBeenCalled();
    expect(getMovieShowTimes).not.toHaveBeenCalled();
  });

  it("still shows the metadata of a retired movie", async () => {
    getMovie.mockResolvedValue({ ...MOVIE, status: "ARCHIVED" });

    renderMovie();

    expect(
      await screen.findByRole("heading", { name: "Interstellar" })
    ).toBeInTheDocument();

    expect(screen.getByText(/A team travels through a wormhole/i)).toBeInTheDocument();
  });
});
