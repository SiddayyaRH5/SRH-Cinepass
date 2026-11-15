import React from "react";

import {
  Navigate,
  Route,
  Routes,
  useLocation,
} from "react-router-dom";

import { Toaster } from "react-hot-toast";

import Navbar from "./components/Navbar";
import Footer from "./components/Footer";

import Home from "./pages/Home";
import Movies from "./pages/Movies";
import MovieDetails from "./pages/MovieDetails";
import MyBookings from "./pages/MyBookings";
import Ticket from "./pages/Ticket";
import StaffScanner from "./pages/Staff/StaffScanner";
import SeatLayout from "./pages/SeatLayout";
import Favorite from "./pages/Favorite";
import Login from "./pages/Login";
import Signup from "./pages/Signup";
import Theaters from "./pages/Theaters";
import TheatreShows from "./pages/TheatreShows";
import OAuth2Success from "./pages/OAuth2Success";

import Layout from "./pages/Admin/Layout";
import Dashboard from "./pages/Admin/Dashboard";
import ListShows from "./pages/Admin/ListShows";
import ListofBookings from "./pages/Admin/ListofBookings";
import AddShows from "./pages/Admin/AddShows";
import AddTheatre from "./pages/Admin/AddTheatre";
import ManageTheatres from "./pages/Admin/ManageTheatres";

import TheatreVerification from "./pages/TheatreOwner/TheatreVerification";
import OwnerDashboard from "./pages/TheatreOwner/OwnerDashboard";

import TheatreVerificationAdmin from "./pages/Admin/TheatreVerificationAdmin";
import StaffManagement from "./pages/Admin/StaffManagement";

import { LocationProvider } from "./lib/LocationContext";
import { useAuth } from "./context/AuthContext";


/* ============================================================
   PROTECTED USER ROUTE
============================================================ */

function ProtectedRoute({ children }) {

  const { isAuthenticated } = useAuth();
  const location = useLocation();

  if (!isAuthenticated) {

    return (
      <Navigate
        to="/login"
        replace
        state={{
          from: location.pathname,
        }}
      />
    );
  }

  return children;
}


/* ============================================================
   PROTECTED ADMIN ROUTE
============================================================ */

function AdminRoute({ children }) {

  const { isAuthenticated, user } = useAuth();

  if (!isAuthenticated) {
    return <Navigate to="/login" replace />;
  }

  if (user?.role !== "ADMIN") {
    return <Navigate to="/" replace />;
  }

  return children;
}


/* ============================================================
   PROTECTED THEATRE OWNER ROUTE
============================================================ */

function OwnerRoute({ children }) {

  const { isAuthenticated, user } = useAuth();

  if (!isAuthenticated) {
    return <Navigate to="/login" replace />;
  }

  if (user?.role !== "THEATRE_OWNER") {
    return <Navigate to="/" replace />;
  }

  return children;
}


/* ============================================================
   PROTECTED STAFF ROUTE
   The backend independently enforces ROLE_STAFF/ROLE_ADMIN on
   every verification request; this guard is only for navigation.
============================================================ */

function StaffRoute({ children }) {

  const { isAuthenticated, user } = useAuth();
  const location = useLocation();

  if (!isAuthenticated) {
    return (
      <Navigate
        to="/login"
        replace
        state={{ from: location.pathname }}
      />
    );
  }

  if (user?.role !== "STAFF" && user?.role !== "ADMIN") {
    return <Navigate to="/" replace />;
  }

  return children;
}


/* ============================================================
   MAIN SHELL
============================================================ */

function Shell() {

  const { pathname } = useLocation();

  const admin =
    pathname.startsWith("/admin");

  const owner =
    pathname.startsWith("/owner");

  const staff =
    pathname.startsWith("/staff");

  return (
    <>

      {/* CUSTOMER NAVBAR */}

      {!admin && !owner && !staff && <Navbar />}


      <Routes>

        {/* =====================================================
            MAIN PAGES
        ===================================================== */}

        <Route
          path="/"
          element={<Home />}
        />

        <Route
          path="/movies"
          element={<Movies />}
        />

        <Route
          path="/movie/:id"
          element={<MovieDetails />}
        />


        {/* =====================================================
            AUTHENTICATION
        ===================================================== */}

        <Route
          path="/login"
          element={<Login />}
        />

        <Route
          path="/signup"
          element={<Signup />}
        />


        {/* =====================================================
            GOOGLE OAUTH
        ===================================================== */}

        <Route
          path="/oauth2/success"
          element={<OAuth2Success />}
        />


        {/* =====================================================
            THEATRES
        ===================================================== */}

        <Route
          path="/theaters"
          element={<Theaters />}
        />

        <Route
          path="/theatre/:theatreId"
          element={<TheatreShows />}
        />


        {/* =====================================================
            SHOW / SEAT BOOKING
            LOGIN REQUIRED
        ===================================================== */}

        <Route
          path="/show/:id"
          element={
            <ProtectedRoute>
              <SeatLayout />
            </ProtectedRoute>
          }
        />


        {/* =====================================================
            USER PAGES
            LOGIN REQUIRED
        ===================================================== */}

        <Route
          path="/My-Bookings"
          element={
            <ProtectedRoute>
              <MyBookings />
            </ProtectedRoute>
          }
        />

        <Route
          path="/favorite"
          element={
            <ProtectedRoute>
              <Favorite />
            </ProtectedRoute>
          }
        />

        <Route
          path="/tickets/:ticketId"
          element={
            <ProtectedRoute>
              <Ticket />
            </ProtectedRoute>
          }
        />


        {/* =====================================================
            STAFF TICKET SCANNER
            STAFF / ADMIN ONLY
        ===================================================== */}

        <Route
          path="/staff/scanner"
          element={
            <StaffRoute>
              <StaffScanner />
            </StaffRoute>
          }
        />


        {/* =====================================================
            ADMIN PAGES
            ADMIN ONLY
        ===================================================== */}

        <Route
          path="/admin"
          element={
            <AdminRoute>
              <Layout />
            </AdminRoute>
          }
        >

          <Route
            index
            element={<Dashboard />}
          />

          <Route
            path="add-theatre"
            element={<AddTheatre />}
          />

          <Route
            path="manage-theatres"
            element={<ManageTheatres />}
          />

          <Route
            path="list-shows"
            element={<ListShows />}
          />

          <Route
            path="list-bookings"
            element={<ListofBookings />}
          />

          <Route
            path="add-shows"
            element={<AddShows />}
          />

          <Route
            path="staff"
            element={<StaffManagement />}
          />

        </Route>


        {/* =====================================================
            THEATRE OWNER DASHBOARD
            THEATRE_OWNER ONLY
        ===================================================== */}

        <Route
          path="/owner"
          element={
            <OwnerRoute>
              <OwnerDashboard />
            </OwnerRoute>
          }
        />


        {/* =====================================================
            THEATRE OWNER VERIFICATION
        ===================================================== */}

        <Route
          path="/theatre-verification"
          element={
            <OwnerRoute>
              <TheatreVerification />
            </OwnerRoute>
          }
        />


        {/* =====================================================
            ADMIN THEATRE VERIFICATION
            ADMIN ONLY
        ===================================================== */}

        <Route
          path="/admin/theatre-verification"
          element={
            <AdminRoute>
              <TheatreVerificationAdmin />
            </AdminRoute>
          }
        />


        {/* =====================================================
            FALLBACK
        ===================================================== */}

        <Route
          path="*"
          element={<Home />}
        />

      </Routes>


      {/* CUSTOMER FOOTER */}

      {!admin && !owner && !staff && <Footer />}

    </>
  );
}


/* ============================================================
   APP
============================================================ */

export default function App() {

  return (

    <LocationProvider>

      <Toaster
        position="top-right"
        toastOptions={{
          duration: 3500,

          style: {
            background: "#111722",
            color: "#fff",
            border: "1px solid rgba(255,255,255,.1)",
            borderRadius: "14px",
          },
        }}
      />

      <Shell />

    </LocationProvider>
  );
}

