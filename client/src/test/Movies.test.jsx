import React from "react";
import { describe, expect, it, vi, beforeEach } from "vitest";
import { render, screen } from "@testing-library/react";
import { MemoryRouter } from "react-router-dom";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";

import Movies from "../pages/Movies";

vi.mock("../lib/api", async () => {
  const actual = await vi.importActual("../lib/api");

  return {
    ...actual,
    getMovies: vi.fn(),
  };
});

import { getMovies } from "../lib/api";

const CATALOGUE = [
  {
    id: 1,
    title: "Interstellar",
    rating: 8.7,
    genre: "Sci-Fi, Drama",
    releaseDate: "2014-11-05",
    duration: 169,
  },
  {
    id: 2,
    title: "Inception",
    rating: 8.8,
    genre: "Sci-Fi, Action",
    releaseDate: "2010-07-15",
    duration: 148,
  },
];

function renderMovies() {
  const queryClient = new QueryClient({
    defaultOptions: { queries: { retry: false } },
  });
  return render(
    <QueryClientProvider client={queryClient}>
      <MemoryRouter initialEntries={["/movies"]}>
        <Movies />
      </MemoryRouter>
    </QueryClientProvider>
  );
}

describe("Movie catalogue", () => {
  beforeEach(() => {
    localStorage.clear();
    getMovies.mockResolvedValue(CATALOGUE);
  });

  it("lists the movies returned by the catalogue API", async () => {
    renderMovies();

    expect(
      await screen.findByRole("heading", { name: "Interstellar" })
    ).toBeInTheDocument();

    expect(
      screen.getByRole("heading", { name: "Inception" })
    ).toBeInTheDocument();

    expect(screen.getByText("2 movies found")).toBeInTheDocument();
  });

  it("shows a helpful message when the catalogue is empty", async () => {
    getMovies.mockResolvedValue([]);

    renderMovies();

    expect(
      await screen.findByText(/No movies match your search/i)
    ).toBeInTheDocument();

    expect(screen.getByText("0 movies found")).toBeInTheDocument();
  });

  it("filters the catalogue by the search term", async () => {
    renderMovies();

    expect(
      await screen.findByRole("heading", { name: "Interstellar" })
    ).toBeInTheDocument();

    const input = screen.getByPlaceholderText(/search by movie title/i);

    // The search box is a controlled input, so the filter runs on change.
    await import("@testing-library/user-event").then(({ default: userEvent }) =>
      userEvent.setup().type(input, "Inception")
    );

    expect(
      screen.queryByRole("heading", { name: "Interstellar" })
    ).not.toBeInTheDocument();

    expect(
      screen.getByRole("heading", { name: "Inception" })
    ).toBeInTheDocument();
  });
});
