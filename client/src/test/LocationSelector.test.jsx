import React from "react";
import { describe, expect, it, vi, beforeEach, afterEach } from "vitest";
import { render, screen, waitFor, within } from "@testing-library/react";
import userEvent from "@testing-library/user-event";

import LocationSelector from "../components/LocationSelector";
import { LocationProvider, useLocationContext } from "../lib/LocationContext";

vi.mock("../lib/api", async () => {
  const actual = await vi.importActual("../lib/api");

  return {
    ...actual,
    getLocations: vi.fn(),
  };
});

import { getLocations } from "../lib/api";

function SelectionProbe() {
  const { selectedLocation } = useLocationContext();

  return (
    <p data-testid="selection">
      {selectedLocation ? `${selectedLocation.city}|${selectedLocation.id}` : "none"}
    </p>
  );
}

function renderSelector() {
  return render(
    <LocationProvider>
      <LocationSelector />
      <SelectionProbe />
    </LocationProvider>
  );
}

const BACKEND_LOCATIONS = [
  { id: 7, city: "Bengaluru", state: "Karnataka", country: "India" },
  { id: 9, city: "Mumbai", state: "Maharashtra", country: "India" },
];

const originalFetch = globalThis.fetch;

function setGeolocation(geolocation) {
  Object.defineProperty(navigator, "geolocation", {
    value: geolocation,
    configurable: true,
    writable: true,
  });
}

async function openDialog() {
  await userEvent.click(
    screen.getByRole("button", { name: /select your city/i })
  );

  return screen.getByRole("dialog", { name: /select your city/i });
}

describe("LocationSelector city selection", () => {
  beforeEach(() => {
    localStorage.clear();
    getLocations.mockResolvedValue(BACKEND_LOCATIONS);
  });

  afterEach(() => {
    setGeolocation(undefined);
    globalThis.fetch = originalFetch;
  });

  it("opens the city dialog with heading and subtitle", async () => {
    renderSelector();

    const dialog = await openDialog();

    expect(
      within(dialog).getByRole("heading", { name: "Select your city" })
    ).toBeInTheDocument();

    expect(
      within(dialog).getByText(
        "Choose your city to discover movies and theatres near you."
      )
    ).toBeInTheDocument();
  });

  it("shows popular cities and no duplicates in the full list", async () => {
    renderSelector();

    const dialog = await openDialog();

    expect(
      within(dialog).getByRole("region", { name: "Popular cities" })
    ).toBeInTheDocument();

    // Bengaluru is popular, so it must not be repeated in the A-Z list.
    expect(
      within(dialog).getAllByRole("button", { name: /^Bengaluru/ })
    ).toHaveLength(1);
  });

  it("filters instantly, case-insensitively, and by partial match", async () => {
    renderSelector();

    const dialog = await openDialog();
    const input = within(dialog).getByRole("searchbox", {
      name: /search cities/i,
    });

    await userEvent.type(input, "kol");

    expect(
      within(dialog).getByRole("button", { name: /^Kolkata/ })
    ).toBeInTheDocument();

    expect(
      within(dialog).queryByRole("button", { name: /^Mumbai/ })
    ).not.toBeInTheDocument();
  });

  it("resolves common alternate spellings while searching", async () => {
    renderSelector();

    const dialog = await openDialog();
    const input = within(dialog).getByRole("searchbox", {
      name: /search cities/i,
    });

    await userEvent.type(input, "bangalore");

    expect(
      within(dialog).getByRole("button", { name: /^Bengaluru/ })
    ).toBeInTheDocument();
  });

  it("shows an empty state and can clear the search", async () => {
    renderSelector();

    const dialog = await openDialog();
    const input = within(dialog).getByRole("searchbox", {
      name: /search cities/i,
    });

    await userEvent.type(input, "zzzzzz");

    expect(
      within(dialog).getByText(/No cities match/i)
    ).toBeInTheDocument();

    await userEvent.click(
      within(dialog).getByRole("button", {
        name: /clear search and show all cities/i,
      })
    );

    expect(
      within(dialog).getByRole("region", { name: "Popular cities" })
    ).toBeInTheDocument();
  });

  it("selects a city, closes the dialog, and persists the choice", async () => {
    const { unmount } = renderSelector();

    const dialog = await openDialog();

    await userEvent.click(
      within(dialog).getByRole("button", { name: /^Bengaluru/ })
    );

    expect(
      screen.queryByRole("dialog", { name: /select your city/i })
    ).not.toBeInTheDocument();

    expect(screen.getByTestId("selection")).toHaveTextContent("Bengaluru|7");

    const saved = JSON.parse(localStorage.getItem("cinepassLocation"));
    expect(saved).toMatchObject({ city: "Bengaluru", id: 7 });

    unmount();

    // A fresh mount restores the persisted city on the trigger.
    renderSelector();

    expect(
      screen.getByRole("button", { name: /currently Bengaluru/i })
    ).toBeInTheDocument();
  });

  it("selects curated cities that have no backend record", async () => {
    renderSelector();

    const dialog = await openDialog();
    const input = within(dialog).getByRole("searchbox", {
      name: /search cities/i,
    });

    await userEvent.type(input, "Udupi");

    await userEvent.click(
      within(dialog).getByRole("button", { name: /^Udupi/ })
    );

    expect(screen.getByTestId("selection")).toHaveTextContent("Udupi|null");
  });

  it("detects the city from browser geolocation", async () => {
    const getCurrentPosition = vi.fn((success) =>
      success({ coords: { latitude: 12.97, longitude: 77.59 } })
    );
    setGeolocation({ getCurrentPosition });

    globalThis.fetch = vi.fn().mockResolvedValue({
      ok: true,
      json: async () => ({ address: { city: "Bangalore Urban" } }),
    });

    renderSelector();

    const dialog = await openDialog();

    await userEvent.click(
      within(dialog).getByRole("button", { name: /detect my location/i })
    );

    await waitFor(() => {
      expect(screen.getByTestId("selection")).toHaveTextContent("Bengaluru|7");
    });

    expect(getCurrentPosition).toHaveBeenCalledTimes(1);
  });

  it("never assumes a city when location permission is denied", async () => {
    const getCurrentPosition = vi.fn((_success, error) =>
      error({ code: 1 })
    );
    setGeolocation({ getCurrentPosition });

    renderSelector();

    const dialog = await openDialog();

    await userEvent.click(
      within(dialog).getByRole("button", { name: /detect my location/i })
    );

    expect(
      await within(dialog).findByText(/permission was denied/i)
    ).toBeInTheDocument();

    expect(screen.getByTestId("selection")).toHaveTextContent("none");
    expect(localStorage.getItem("cinepassLocation")).toBeNull();
    expect(
      screen.getByRole("dialog", { name: /select your city/i })
    ).toBeInTheDocument();
  });

  it("handles a geocoding failure without selecting a city", async () => {
    setGeolocation({
      getCurrentPosition: vi.fn((success) =>
        success({ coords: { latitude: 0, longitude: 0 } })
      ),
    });

    globalThis.fetch = vi.fn().mockResolvedValue({
      ok: true,
      json: async () => ({ address: { city: "Atlantis" } }),
    });

    renderSelector();

    const dialog = await openDialog();

    await userEvent.click(
      within(dialog).getByRole("button", { name: /detect my location/i })
    );

    expect(
      await within(dialog).findByText(/couldn't match your location/i)
    ).toBeInTheDocument();

    expect(screen.getByTestId("selection")).toHaveTextContent("none");
  });
});
