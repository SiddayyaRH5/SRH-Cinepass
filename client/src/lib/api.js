
const API_BASE = (
  import.meta.env.VITE_API_BASE_URL || "http://localhost:8080"
).replace(/\/+$/, "");

export const apiUrl = (path) => `${API_BASE}${path}`;

export async function apiFetch(path, options = {}) {
  const token = localStorage.getItem("cinepassToken");

  const headers = new Headers(options.headers || {});

  if (
    options.body &&
    !(options.body instanceof FormData) &&
    !headers.has("Content-Type")
  ) {
    headers.set("Content-Type", "application/json");
  }

  if (token) {
    headers.set("Authorization", `Bearer ${token}`);
  }

  const response = await fetch(apiUrl(path), {
    ...options,
    headers,
  });

  if (response.status === 204) {
    if (!response.ok) {
      throw new Error(`Request failed (${response.status})`);
    }
    return null;
  }

  const contentType = response.headers.get("content-type") || "";

  const data = contentType.includes("application/json")
    ? await response.json().catch(() => null)
    : await response.text().catch(() => "");

  if (!response.ok) {
    const message =
      typeof data === "string" && data
        ? data
        : data?.message ||
        data?.error ||
        `Request failed (${response.status})`;

    const error = new Error(message);
    error.status = response.status;
    throw error;
  }

  return data;
}

// MOVIES
export const getMovies = () => apiFetch("/api/movies");

export const getMovie = (id) =>
  apiFetch(`/api/movies/${id}`);

// LOCATIONS
export const getLocations = () =>
  apiFetch("/api/locations");

// THEATRES
export const getTheatresByLocation = (id) =>
  apiFetch(`/api/theatres/location/${id}`);

export const getTheatre = (id) =>
  apiFetch(`/api/theatres/${id}`);

export const getAllTheatres = () =>
  apiFetch("/api/theatres");

export const getMyTheatres = () =>
  apiFetch("/api/theatres/my");

export const createTheatre = (payload) =>
  apiFetch("/api/theatres", {
    method: "POST",
    body: JSON.stringify(payload),
  });

export const updateTheatre = (id, payload) =>
  apiFetch(`/api/theatres/${id}`, {
    method: "PUT",
    body: JSON.stringify(payload),
  });

export const deleteTheatre = (id) =>
  apiFetch(`/api/theatres/${id}`, {
    method: "DELETE",
  });

// SHOWS
export const getShows = () =>
  apiFetch("/api/shows");

export const getShow = (id) =>
  apiFetch(`/api/shows/${id}`);

// Movie-based discovery: city -> theatre -> date -> showtime.
export const getMovieCities = (movieId) =>
  apiFetch(`/api/shows/discover/movies/${movieId}/cities`);

export const getMovieTheatres = (movieId, locationId) =>
  apiFetch(
    `/api/shows/discover/movies/${movieId}/cities/${locationId}/theatres`
  );

export const getMovieShowDates = (movieId, theatreId) =>
  apiFetch(
    `/api/shows/discover/movies/${movieId}/theatres/${theatreId}/dates`
  );

export const getMovieShowTimes = (movieId, theatreId, date) =>
  apiFetch(
    `/api/shows/discover/movies/${movieId}/theatres/${theatreId}/dates/${date}`
  );

// Live lifecycle + seat availability for a single show. Called immediately
// before creating an order so a stale page cannot book a closed show.
export const getShowAvailability = (showId) =>
  apiFetch(`/api/shows/${showId}/availability`);

export const getShowsByTheatreAndDate = (theatreId, date) =>
  apiFetch(`/api/shows/theatre/${theatreId}/date/${date}`);

export const createShow = (payload) =>
  apiFetch("/api/shows", {
    method: "POST",
    body: JSON.stringify(payload),
  });

export const updateShow = (id, payload) =>
  apiFetch(`/api/shows/${id}`, {
    method: "PUT",
    body: JSON.stringify(payload),
  });

export const deleteShow = (id) =>
  apiFetch(`/api/shows/${id}`, {
    method: "DELETE",
  });

// SEATS
export const getSeats = (showId) =>
  apiFetch(`/api/shows/${showId}/seats`);

// BOOKINGS
export const getMyBookings = () =>
  apiFetch("/api/bookings/me");

export const getAllBookings = () =>
  apiFetch("/api/bookings");

export const createBooking = (payload) =>
  apiFetch("/api/bookings", {
    method: "POST",
    body: JSON.stringify(payload),
  });

// THEATRE VERIFICATION
export const submitTheatreVerification = (data) =>
  apiFetch("/api/theatre-verification", {
    method: "POST",
    body: JSON.stringify(data),
  });

export const getMyTheatreVerification = () =>
  apiFetch("/api/theatre-verification/my");

export const getPendingTheatreVerifications = () =>
  apiFetch("/api/theatre-verification/pending");

export const approveTheatreVerification = (id) =>
  apiFetch(`/api/theatre-verification/${id}/approve`, {
    method: "PUT",
  });

export const rejectTheatreVerification = (id, reason) =>
  apiFetch(`/api/theatre-verification/${id}/reject`, {
    method: "PUT",
    body: JSON.stringify({ reason }),
  });

// ADMIN AUTOMATION
export const getAdminMovies = () =>
  apiFetch("/api/admin/movies");

export const getAdminImportedMovies = () =>
  apiFetch("/api/admin/movies/imported");

export const getAdminSyncStatus = () =>
  apiFetch("/api/admin/movies/sync-status");

export const excludeMovie = (id, excluded, reason) =>
  apiFetch(`/api/admin/movies/${id}/exclude`, {
    method: "POST",
    body: JSON.stringify({ excluded, reason }),
  });

export const protectMovie = (id, protectedMovie) =>
  apiFetch(`/api/admin/movies/${id}/protect`, {
    method: "POST",
    body: JSON.stringify({ protectedMovie }),
  });

export const extendMovie = (id, activeUntil) =>
  apiFetch(`/api/admin/movies/${id}/extend`, {
    method: "POST",
    body: JSON.stringify({ activeUntil }),
  });

export const reactivateMovie = (id) =>
  apiFetch(`/api/admin/movies/${id}/reactivate`, {
    method: "POST",
  });

export const archiveMovie = (id) =>
  apiFetch(`/api/admin/movies/${id}/archive`, {
    method: "POST",
  });

export const getAutomationRuns = () =>
  apiFetch("/api/admin/automation/runs");

export const getAutomationConfig = () =>
  apiFetch("/api/admin/automation/config");

export const runAutomation = () =>
  apiFetch("/api/admin/automation/run", {
    method: "POST",
  });

export { API_BASE };
