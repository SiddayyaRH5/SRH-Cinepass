// Centralized query-key factories for TanStack Query.
// Use these consistently across all components to ensure cache
// reuse and correct invalidation.

export const queryKeys = {
  movies: {
    all: () => ["movies"],
    detail: (id) => ["movies", "detail", id],
    recommendations: (id) => ["movies", "recommendations", id],
  },
  theatres: {
    all: () => ["theatres"],
    detail: (id) => ["theatres", "detail", id],
    byLocation: (locationId) => ["theatres", "location", locationId],
  },
  shows: {
    all: () => ["shows"],
    byMovie: (movieId) => ["shows", "movie", movieId],
    byTheatre: (theatreId) => ["shows", "theatre", theatreId],
    byDate: (date) => ["shows", "date", date],
    discovery: {
      cities: (movieId) => ["shows", "discover", "cities", movieId],
      theatres: (movieId, locationId) => ["shows", "discover", "theatres", movieId, locationId],
      dates: (movieId, theatreId) => ["shows", "discover", "dates", movieId, theatreId],
      showTimes: (movieId, theatreId, date) => ["shows", "discover", "showtimes", movieId, theatreId, date],
    },
  },
  bookings: {
    all: () => ["bookings"],
    byUser: (userId) => ["bookings", "user", userId],
    detail: (id) => ["bookings", "detail", id],
  },
  locations: {
    all: () => ["locations"],
  },
  admin: {
    movies: () => ["admin", "movies"],
    syncStatus: () => ["admin", "sync-status"],
    automationRuns: () => ["admin", "automation-runs"],
    automationConfig: () => ["admin", "automation-config"],
  },
};
