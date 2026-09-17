const API_BASE =
  import.meta.env.VITE_API_BASE_URL ||
  "http://localhost:8080";

export const apiUrl = (path) =>
  `${API_BASE}${path}`;

export async function apiFetch(path, options = {}) {
  const token = localStorage.getItem("cinepassToken");

  const headers = new Headers(options.headers || {});

  if (
    !headers.has("Content-Type") &&
    options.body &&
    !(options.body instanceof FormData)
  ) {
    headers.set("Content-Type", "application/json");
  }

  if (token) {
    headers.set(
      "Authorization",
      `Bearer ${token}`
    );
  }

  const response = await fetch(
    apiUrl(path),
    {
      ...options,
      headers
    }
  );

  const contentType =
    response.headers.get("content-type") || "";

  const data =
    contentType.includes("application/json")
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

export const getMovies = () =>
  apiFetch("/api/movies");

export const getMovie = (id) =>
  apiFetch(`/api/movies/${id}`);

export const getLocations = () =>
  apiFetch("/api/locations");

export const getTheatresByLocation = (id) =>
  apiFetch(`/api/theatres/location/${id}`);

export const getTheatre = (id) =>
  apiFetch(`/api/theatres/${id}`);

export const getShows = () =>
  apiFetch("/api/shows");

export const getShow = (id) =>
  apiFetch(`/api/shows/${id}`);

export const getShowsByTheatreAndDate = (theatreId, date) =>
  apiFetch(
    `/api/shows/theatre/${theatreId}/date/${date}`
  );

export const getSeats = (showId) =>
  apiFetch(`/api/shows/${showId}/seats`);

export const getMyBookings = () =>
  apiFetch("/api/bookings/me");

export const getAllBookings = () =>
  apiFetch("/api/bookings");

export const createBooking = (payload) =>
  apiFetch("/api/bookings", {
    method: "POST",
    body: JSON.stringify(payload)
  });

export const createShow = (payload) =>
  apiFetch("/api/shows", {
    method: "POST",
    body: JSON.stringify(payload)
  });

export const updateShow = (id, payload) =>
  apiFetch(`/api/shows/${id}`, {
    method: "PUT",
    body: JSON.stringify(payload)
  });

export const deleteShow = (id) =>
  apiFetch(`/api/shows/${id}`, {
    method: "DELETE"
  });

export const createTheatre = (payload) =>
  apiFetch("/api/theatres", {
    method: "POST",
    body: JSON.stringify(payload)
  });

export const getAllTheatres = () =>
  apiFetch("/api/theatres");

export const updateTheatre = (id, payload) =>
  apiFetch(`/api/theatres/${id}`, {
    method: "PUT",
    body: JSON.stringify(payload)
  });

export const deleteTheatre = (id) =>
  apiFetch(`/api/theatres/${id}`, {
    method: "DELETE"
  });

export const getMyTheatres = () =>
  apiFetch("/api/theatres/my");

export const submitTheatreVerification = (data) =>
  apiFetch("/api/theatre-verification", {
    method: "POST",
    body: JSON.stringify(data)
  });

export const getMyTheatreVerification = () =>
  apiFetch("/api/theatre-verification/my");

export const getPendingTheatreVerifications = () =>
  apiFetch("/api/theatre-verification/pending");

export const approveTheatreVerification = (id) =>
  apiFetch(`/api/theatre-verification/${id}/approve`, {
    method: "PUT"
  });

export const rejectTheatreVerification = (id, reason) =>
  apiFetch(`/api/theatre-verification/${id}/reject`, {
    method: "PUT",
    body: JSON.stringify({ reason })
  });

export { API_BASE };