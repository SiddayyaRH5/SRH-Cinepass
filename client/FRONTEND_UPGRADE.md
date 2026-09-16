# SRH CinePass Frontend — Premium UI Upgrade

This version keeps the React/Vite frontend and connects it to the Spring Boot API.

## Main changes
- Removed Clerk runtime usage and provider.
- Added centralized JWT AuthContext.
- Added reusable API client with Bearer token support.
- Rebuilt navigation, location selector, movie discovery, movie details, theatres, show list, seat selection, bookings and admin UI.
- Added responsive cinematic design, glass panels, hover states, page entrance animations, skeleton/loading states and reduced-motion support.
- Booking now sends only `showId` and `seatIds`; the backend derives the authenticated user from the JWT.
- My Bookings uses `/api/bookings/me`.
- Location uses `/api/locations`.
- Admin screens use live shows/bookings APIs where available.

## Run

From `client`:

```powershell
npm install
npm run dev
```

Backend should be running at:

`http://localhost:8080`

Frontend normally runs at:

`http://localhost:5173`

## API base

Configured in `.env`:

`VITE_API_BASE_URL='http://localhost:8080'`

If your backend uses another port, change that value.
