<<<<<<< HEAD
const configuredBase = import.meta.env.VITE_API_BASE_URL || "http://localhost:8080";
export const API_BASE = configuredBase.replace(/\/+$/, "");

export const apiUrl = (path) => `${API_BASE}${path.startsWith("/") ? path : `/${path}`}`;

const inFlightGets = new Map();
const DEFAULT_TIMEOUT_MS = 90_000;

async function performApiFetch(path, options = {}) {
  const {
    timeoutMs = DEFAULT_TIMEOUT_MS,
    signal: externalSignal,
    ...fetchOptions
  } = options;

  const method = (fetchOptions.method || "GET").toUpperCase();
  const token = localStorage.getItem("cinepassToken");
  const isPublicAuthPost = method === "POST" &&
    (path === "/api/auth/login" || path === "/api/auth/signup");
  const isPublicCatalogueGet = method === "GET" && (
    path === "/api/health/live" || path === "/api/health/ready" ||
    path === "/api/movies" || path.startsWith("/api/movies/") ||
    path === "/api/locations" || path.startsWith("/api/locations/") ||
    path === "/api/shows" || path.startsWith("/api/shows/") ||
    path === "/api/theatres" || path.startsWith("/api/theatres/location/") ||
    (/^\/api\/theatres\/[^/]+$/).test(path) && path !== "/api/theatres/my" ||
    path === "/api/tmdb/search"
  );

  const headers = new Headers(fetchOptions.headers || {});
  if (!headers.has("Content-Type") && fetchOptions.body && !(fetchOptions.body instanceof FormData)) {
    headers.set("Content-Type", "application/json");
  }
  if (token && !isPublicAuthPost && !isPublicCatalogueGet) {
    headers.set("Authorization", `Bearer ${token}`);
  }

  const controller = new AbortController();
  let timedOut = false;
  const timeoutId = setTimeout(() => {
    timedOut = true;
    controller.abort();
  }, timeoutMs);

  const abortFromCaller = () => controller.abort();
  if (externalSignal) {
    if (externalSignal.aborted) controller.abort();
    else externalSignal.addEventListener("abort", abortFromCaller, { once: true });
  }

  try {
    const response = await fetch(apiUrl(path), {
      ...fetchOptions,
      method,
      headers,
      signal: controller.signal,
    });

    const contentType = response.headers.get("content-type") || "";
    const data = contentType.includes("application/json")
      ? await response.json().catch(() => null)
      : await response.text().catch(() => "");

    if (!response.ok) {
      const message = typeof data === "string" && data
        ? data
        : data?.message || data?.error || `Request failed (${response.status})`;
      const error = new Error(message);
      error.status = response.status;
      throw error;
    }

    return data;
  } catch (error) {
    if (timedOut) {
      const timeoutError = new Error(
        path === "/api/auth/signup"
          ? "The request timed out. Your account may still have been created. Try logging in before submitting signup again."
          : "The server is taking longer than expected to respond. Please try again when the service is available."
      );
      timeoutError.code = "REQUEST_TIMEOUT";
      timeoutError.uncertain = method !== "GET";
      throw timeoutError;
    }
    if (error?.name === "AbortError") throw error;
    if (error instanceof TypeError) {
      const networkError = new Error("Unable to reach the booking service. It may be starting up; please try again shortly.");
      networkError.code = "NETWORK_ERROR";
      throw networkError;
    }
    throw error;
  } finally {
    clearTimeout(timeoutId);
    externalSignal?.removeEventListener("abort", abortFromCaller);
  }
}

export function apiFetch(path, options = {}) {
  const { dedupe: dedupeOption = true, ...requestOptions } = options;
  const method = (requestOptions.method || "GET").toUpperCase();
  const canDedupe = method === "GET" && dedupeOption && !requestOptions.signal;
  if (!canDedupe) return performApiFetch(path, requestOptions);

  const token = localStorage.getItem("cinepassToken") || "anonymous";
  const key = `${token}:${apiUrl(path)}`;
  const existing = inFlightGets.get(key);
  if (existing) return existing;

  const request = performApiFetch(path, requestOptions).finally(() => inFlightGets.delete(key));
  inFlightGets.set(key, request);
  return request;
}

// MOVIES
export const getMovies = (options = {}) => apiFetch("/api/movies", options);
export const getMovie = (id) => apiFetch(`/api/movies/${id}`);

// LOCATIONS
export const getLocations = () => apiFetch("/api/locations");

// THEATRES
export const getTheatresByLocation = (id) => apiFetch(`/api/theatres/location/${id}`);
export const getTheatre = (id) => apiFetch(`/api/theatres/${id}`);
export const getAllTheatres = () => apiFetch("/api/theatres");
export const getMyTheatres = () => apiFetch("/api/theatres/my");
export const createTheatre = (payload) => apiFetch("/api/theatres", { method: "POST", body: JSON.stringify(payload) });
export const updateTheatre = (id, payload) => apiFetch(`/api/theatres/${id}`, { method: "PUT", body: JSON.stringify(payload) });
export const deleteTheatre = (id) => apiFetch(`/api/theatres/${id}`, { method: "DELETE" });

// SHOWS
export const getShows = () => apiFetch("/api/shows");
export const getShow = (id) => apiFetch(`/api/shows/${id}`);
export const getShowsByTheatreAndDate = (theatreId, date) => apiFetch(`/api/shows/theatre/${theatreId}/date/${date}`);
export const createShow = (payload) => apiFetch("/api/shows", { method: "POST", body: JSON.stringify(payload) });
export const updateShow = (id, payload) => apiFetch(`/api/shows/${id}`, { method: "PUT", body: JSON.stringify(payload) });
export const deleteShow = (id) => apiFetch(`/api/shows/${id}`, { method: "DELETE" });

// SEATS
export const getSeats = (showId) => apiFetch(`/api/shows/${showId}/seats`);

// BOOKINGS
export const getMyBookings = () => apiFetch("/api/bookings/me");
export const getAllBookings = () => apiFetch("/api/bookings");
export const createBooking = (payload) => apiFetch("/api/bookings", { method: "POST", body: JSON.stringify(payload) });

// THEATRE VERIFICATION
export const submitTheatreVerification = (data) => apiFetch("/api/theatre-verification", { method: "POST", body: JSON.stringify(data) });
export const getMyTheatreVerification = () => apiFetch("/api/theatre-verification/my");
export const getPendingTheatreVerifications = () => apiFetch("/api/theatre-verification/pending");
export const approveTheatreVerification = (id) => apiFetch(`/api/theatre-verification/${id}/approve`, { method: "PUT" });
export const rejectTheatreVerification = (id, reason) => apiFetch(`/api/theatre-verification/${id}/reject`, { method: "PUT", body: JSON.stringify({ reason }) });
=======
const API_BASE =
  import.meta.env.VITE_API_BASE_URL ||
  "http://localhost:8080";

export const apiUrl = (path) => `${API_BASE}${path}`;

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
    headers.set("Authorization", `Bearer ${token}`);
  }

  const response = await fetch(apiUrl(path), {
    ...options,
    headers,
  });

  const contentType =
    response.headers.get("content-type") || "";

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
};

// MOVIES
export const getMovies = () =>
  apiFetch("/api/movies");

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

export { API_BASE };
>>>>>>> origin/main
